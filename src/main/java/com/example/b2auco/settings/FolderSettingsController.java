package com.example.b2auco.settings;

import com.example.b2auco.results.ResultsFolderReader;
import com.example.b2auco.results.ResultsFolderViewState;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FolderSettingsController {
    private static final String TITLE = "Export folders";
    private static final String INTRO = "Choose where b2auco saves Auth requests, Backlog requests, and live test results.";
    private static final String SUMMARY_LABEL = "Current export folder";
    private static final String GLOBAL_HEADING = "Global default folder";
    private static final String GLOBAL_HELPER = "Used for all exports unless the current Burp project has its own override.";
    private static final String PROJECT_HEADING = "Current project override";
    private static final String AUTH_HEADING = "Auth folder";
    private static final String AUTH_HELPER = "Stores authentication requests and related raw captures.";
    private static final String BACKLOG_HEADING = "Backlog folder";
    private static final String BACKLOG_HELPER = "Stores queued request exports and preserves the previous export-folder setting.";
    private static final String RESULTS_HEADING = "Results folder";
    private static final String RESULTS_HELPER = "Stores result files produced by request processing.";
    private static final String PROJECT_OVERRIDE_TOGGLE_LABEL = "Override folder for this project only";
    private static final String PROJECT_HELPER_ENABLED = "Used only for this Burp project file and overrides the global folder.";
    private static final String PROJECT_HELPER_INACTIVE = "Enable the override to save a project-specific folder for this Burp project file.";
    private static final String PROJECT_HELPER_UNAVAILABLE = "Open or save a Burp project file to set a project-specific override.";
    private static final String SAVE_LABEL = "Save";
    private static final String BROWSE_LABEL = "Browse…";
    private static final String SAVED = "Folder saved.";
    private static final String OVERRIDE_DISABLED = "Project override removed.";
    private static final String BLANK = "Choose a folder before saving.";
    private static final String INVALID = "Enter a valid folder path.";
    private static final String UNWRITABLE = "This folder isn\u2019t writable. Choose another location.";

    private final FolderSettingsStore folderSettingsStore;
    private final EffectiveFolderResolver effectiveFolderResolver;
    private final Supplier<Optional<Path>> currentProjectFilePathSupplier;
    private final Predicate<Path> writableFolderProbe;
    private final ResultsFolderReader resultsFolderReader;

    private Optional<Path> projectOverrideUiIdentity = Optional.empty();
    private boolean projectOverrideUiEnabled;
    private FolderSettingsViewState.ActiveMode activeMode = FolderSettingsViewState.ActiveMode.USER_SETTING;
    private String authFeedbackMessage = "";
    private String backlogFeedbackMessage = "";
    private String resultsFeedbackMessage = "";
    private String globalFeedbackMessage = "";
    private String projectFeedbackMessage = "";
    private ResultsFolderViewState resultsViewState = unconfiguredResultsView();
    private Optional<String> selectedResultFileName = Optional.empty();

    public FolderSettingsController(
            FolderSettingsStore folderSettingsStore,
            EffectiveFolderResolver effectiveFolderResolver,
            Supplier<Optional<Path>> currentProjectFilePathSupplier
    ) {
        this(folderSettingsStore, effectiveFolderResolver, currentProjectFilePathSupplier, FolderSettingsController::isWritableFolder);
    }

    // Constructor injection keeps settings storage, path resolution, and validation probes testable.
    FolderSettingsController(
            FolderSettingsStore folderSettingsStore,
            EffectiveFolderResolver effectiveFolderResolver,
            Supplier<Optional<Path>> currentProjectFilePathSupplier,
            Predicate<Path> writableFolderProbe
    ) {
        this.folderSettingsStore = Objects.requireNonNull(folderSettingsStore, "folderSettingsStore");
        this.effectiveFolderResolver = Objects.requireNonNull(effectiveFolderResolver, "effectiveFolderResolver");
        this.currentProjectFilePathSupplier = Objects.requireNonNull(currentProjectFilePathSupplier, "currentProjectFilePathSupplier");
        this.writableFolderProbe = Objects.requireNonNull(writableFolderProbe, "writableFolderProbe");
        this.resultsFolderReader = new ResultsFolderReader();
    }

    // Loads a fresh settings snapshot for initial tab render and manual results refresh.
    public FolderSettingsViewState loadViewState() {
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        resultsViewState = readResultsView(selectedResultFileName);
        selectedResultFileName = resultsViewState.selectedFile().map(com.example.b2auco.results.ResultFileSummary::fileName);
        return buildViewState(projectFilePath, Optional.empty(), Optional.empty());
    }

    // Loads the user-selected report from the current results folder while preserving all other settings state.
    public FolderSettingsViewState selectResultsFile(String fileName) {
        selectedResultFileName = Optional.ofNullable(fileName).filter(value -> !value.isBlank());
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        resultsViewState = readResultsView(selectedResultFileName);
        selectedResultFileName = resultsViewState.selectedFile().map(com.example.b2auco.results.ResultFileSummary::fileName);
        return buildViewState(projectFilePath, Optional.empty(), Optional.empty());
    }

    // Switches back from the legacy project override panel to the user-level folder settings.
    public FolderSettingsViewState showUserSettings() {
        activeMode = FolderSettingsViewState.ActiveMode.USER_SETTING;
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        return buildViewState(projectFilePath, Optional.empty(), Optional.empty());
    }

    // Keeps the existing project override panel reachable for compatibility with prior behavior.
    public FolderSettingsViewState showProjectSettings() {
        activeMode = FolderSettingsViewState.ActiveMode.PROJECT_SETTING;
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        return buildViewState(projectFilePath, Optional.empty(), Optional.empty());
    }

    // Saves the Auth folder through the new first-class controller action for parent UI integration.
    public FolderSaveResult saveAuthFolder(String folderInput) {
        activeMode = FolderSettingsViewState.ActiveMode.AUTH_FOLDER;
        return saveConfiguredFolder(folderInput, Scope.AUTH, folderSettingsStore::saveAuthFolder, FeedbackTarget.AUTH);
    }

    // Saves the Backlog folder through the new action while preserving legacy global-folder behavior.
    public FolderSaveResult saveBacklogFolder(String folderInput) {
        activeMode = FolderSettingsViewState.ActiveMode.BACKLOG_FOLDER;
        return saveConfiguredFolder(folderInput, Scope.BACKLOG, folderSettingsStore::saveBacklogFolder, FeedbackTarget.BACKLOG);
    }

    // Saves the Results folder through the new first-class controller action for parent UI integration.
    public FolderSaveResult saveResultsFolder(String folderInput) {
        activeMode = FolderSettingsViewState.ActiveMode.RESULTS_FOLDER;
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            resultsFeedbackMessage = validation.message();
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.empty());
            return new FolderSaveResult(Scope.RESULTS, false, validation.message(), viewState);
        }

        folderSettingsStore.saveResultsFolder(validation.path());
        resultsFeedbackMessage = SAVED;
        selectedResultFileName = Optional.empty();
        resultsViewState = readResultsView(selectedResultFileName);
        selectedResultFileName = resultsViewState.selectedFile().map(com.example.b2auco.results.ResultFileSummary::fileName);
        FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.empty());
        return new FolderSaveResult(Scope.RESULTS, true, SAVED, viewState);
    }

    // Legacy global save delegates to Backlog persistence so the old settings tab keeps working until rewired.
    public FolderSaveResult saveGlobalFolder(String folderInput) {
        activeMode = FolderSettingsViewState.ActiveMode.USER_SETTING;
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            globalFeedbackMessage = validation.message();
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.of(validation.message()), Optional.empty());
            return new FolderSaveResult(Scope.GLOBAL, false, validation.message(), viewState);
        }

        folderSettingsStore.saveBacklogFolder(validation.path());
        backlogFeedbackMessage = SAVED;
        globalFeedbackMessage = SAVED;
        FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.of(SAVED), Optional.empty());
        return new FolderSaveResult(Scope.GLOBAL, true, SAVED, viewState);
    }

    // Legacy project override save remains isolated for the existing settings tab and resolver tests.
    public FolderSaveResult saveProjectOverride(String folderInput) {
        activeMode = FolderSettingsViewState.ActiveMode.PROJECT_SETTING;
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        if (!projectContextAvailable(projectFilePath)) {
            resetProjectOverrideUiState();
            projectFeedbackMessage = PROJECT_HELPER_UNAVAILABLE;
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.of(PROJECT_HELPER_UNAVAILABLE));
            return new FolderSaveResult(Scope.PROJECT, false, PROJECT_HELPER_UNAVAILABLE, viewState);
        }

        projectFilePath.ifPresent(path -> rememberProjectOverrideUiState(path, true));
        if (projectFilePath.isEmpty()) {
            projectOverrideUiEnabled = true;
        }

        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            projectFeedbackMessage = validation.message();
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.of(validation.message()));
            return new FolderSaveResult(Scope.PROJECT, false, validation.message(), viewState);
        }

        folderSettingsStore.saveCurrentProjectOverride(validation.path());
        projectFeedbackMessage = SAVED;
        FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.of(SAVED));
        return new FolderSaveResult(Scope.PROJECT, true, SAVED, viewState);
    }

    // Legacy project override toggle remains isolated for the existing settings tab and resolver tests.
    public FolderSettingsViewState setProjectOverrideEnabled(boolean enabled) {
        activeMode = enabled
                ? FolderSettingsViewState.ActiveMode.PROJECT_SETTING
                : FolderSettingsViewState.ActiveMode.USER_SETTING;
        Optional<Path> currentProjectFilePath = currentProjectFilePathSupplier.get();
        if (!projectContextAvailable(currentProjectFilePath)) {
            resetProjectOverrideUiState();
            projectFeedbackMessage = PROJECT_HELPER_UNAVAILABLE;
            return buildViewState(currentProjectFilePath, Optional.empty(), Optional.of(PROJECT_HELPER_UNAVAILABLE));
        }

        currentProjectFilePath.ifPresent(path -> rememberProjectOverrideUiState(path, enabled));
        if (currentProjectFilePath.isEmpty()) {
            projectOverrideUiEnabled = enabled;
        }
        if (!enabled) {
            folderSettingsStore.setCurrentProjectOverrideEnabled(false);
            projectFeedbackMessage = OVERRIDE_DISABLED;
            return buildViewState(currentProjectFilePath, Optional.empty(), Optional.of(OVERRIDE_DISABLED));
        }

        folderSettingsStore.setCurrentProjectOverrideEnabled(true);
        projectFeedbackMessage = folderSettingsStore.findCurrentProjectOverride().isPresent() ? SAVED : "";
        return buildViewState(currentProjectFilePath, Optional.empty(), Optional.of(projectFeedbackMessage));
    }

    // Centralizes shared validation and feedback behavior for Auth, Backlog, and Results save actions.
    private FolderSaveResult saveConfiguredFolder(
            String folderInput,
            Scope scope,
            Consumer<Path> saveAction,
            FeedbackTarget feedbackTarget
    ) {
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            setFeedbackMessage(feedbackTarget, validation.message());
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.empty());
            return new FolderSaveResult(scope, false, validation.message(), viewState);
        }

        saveAction.accept(validation.path());
        setFeedbackMessage(feedbackTarget, SAVED);
        FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.empty());
        return new FolderSaveResult(scope, true, SAVED, viewState);
    }

    // Updates only the feedback target for the folder action that just ran.
    private void setFeedbackMessage(FeedbackTarget feedbackTarget, String message) {
        switch (feedbackTarget) {
            case AUTH -> authFeedbackMessage = message;
            case BACKLOG -> {
                backlogFeedbackMessage = message;
                globalFeedbackMessage = message;
            }
            case RESULTS -> resultsFeedbackMessage = message;
        }
    }

    // Builds both the legacy two-section UI state and the new three-folder state in one snapshot.
    private FolderSettingsViewState buildViewState(
            Optional<Path> currentProjectFilePath,
            Optional<String> globalFeedback,
            Optional<String> projectFeedback
    ) {
        boolean projectAvailable = projectContextAvailable(currentProjectFilePath);
        Optional<Path> projectOverride = projectAvailable
                ? folderSettingsStore.findCurrentProjectOverride()
                : Optional.empty();
        if (projectOverride.isPresent() && projectOverrideUiIdentity.isEmpty()) {
            projectOverrideUiEnabled = folderSettingsStore.isCurrentProjectOverrideEnabled();
        }
        boolean projectOverrideEnabled = resolveProjectOverrideEnabled(projectAvailable, currentProjectFilePath, projectOverride);
        EffectiveFolderSelection effectiveFolder = effectiveFolderResolver.resolve(currentProjectFilePath, Optional.empty());

        globalFeedback.ifPresent(message -> globalFeedbackMessage = message);
        projectFeedback.ifPresent(message -> projectFeedbackMessage = message);

        FolderSettingsViewState.SectionState globalSection = new FolderSettingsViewState.SectionState(
                GLOBAL_HEADING,
                folderSettingsStore.findGlobalDefault().map(Path::toString).orElse(""),
                GLOBAL_HELPER,
                SAVE_LABEL,
                BROWSE_LABEL,
                PROJECT_OVERRIDE_TOGGLE_LABEL,
                true,
                projectAvailable,
                projectOverrideEnabled,
                true,
                globalFeedbackMessage,
                true
        );

        // Project section is the legacy project override control retained for the current settings tab.
        FolderSettingsViewState.SectionState projectSection = new FolderSettingsViewState.SectionState(
                PROJECT_HEADING,
                projectOverride.map(Path::toString).orElse(""),
                projectAvailable
                        ? (projectOverrideEnabled ? PROJECT_HELPER_ENABLED : PROJECT_HELPER_INACTIVE)
                        : PROJECT_HELPER_UNAVAILABLE,
                SAVE_LABEL,
                BROWSE_LABEL,
                PROJECT_OVERRIDE_TOGGLE_LABEL,
                true,
                projectAvailable,
                projectOverrideEnabled,
                projectAvailable && projectOverrideEnabled,
                projectFeedbackMessage,
                false
        );

        // Auth section is independent and has no project override toggle.
        FolderSettingsViewState.SectionState authSection = simpleFolderSection(
                AUTH_HEADING,
                folderSettingsStore.findAuthFolder(),
                AUTH_HELPER,
                authFeedbackMessage
        );

        // Backlog section reads the migrated legacy export-folder value.
        FolderSettingsViewState.SectionState backlogSection = simpleFolderSection(
                BACKLOG_HEADING,
                folderSettingsStore.findBacklogFolder(),
                BACKLOG_HELPER,
                backlogFeedbackMessage
        );

        // Results section is independent and has no project override toggle.
        FolderSettingsViewState.SectionState resultsSection = simpleFolderSection(
                RESULTS_HEADING,
                folderSettingsStore.findResultsFolder(),
                RESULTS_HELPER,
                resultsFeedbackMessage
        );
        return new FolderSettingsViewState(
                TITLE,
                INTRO,
                SUMMARY_LABEL,
                effectiveFolder.folderPath().toString(),
                toSummarySourceLabel(effectiveFolder.source()),
                activeMode,
                globalSection,
                projectSection,
                authSection,
                backlogSection,
                resultsSection,
                resultsViewState.statusMessage(),
                resultsViewState.markdownFiles(),
                resultsViewState.selectedFile()
                        .map(com.example.b2auco.results.ResultFileSummary::fileName)
                        .orElse(""),
                resultsViewState.content()
        );
    }

    // Reads the configured results folder every time the tab state is rebuilt so the display stays live.
    private ResultsFolderViewState readResultsView(Optional<String> preferredFileName) {
        return folderSettingsStore.findResultsFolder()
                .map(resultsFolder -> resultsFolderReader.readMarkdown(resultsFolder, preferredFileName))
                .orElseGet(() -> new ResultsFolderViewState(
                        ResultsFolderViewState.Status.MISSING_DIRECTORY,
                        "Choose a results folder to display test results.",
                        List.of(),
                        Optional.empty(),
                        ""
                ));
    }

    // Default cached result state avoids filesystem reads during unrelated Auth/Backlog/project setting changes.
    private static ResultsFolderViewState unconfiguredResultsView() {
        return new ResultsFolderViewState(
                ResultsFolderViewState.Status.MISSING_DIRECTORY,
                "Choose a results folder to display test results.",
                List.of(),
                Optional.empty(),
                ""
        );
    }

    // Creates a standard folder section for new Auth, Backlog, and Results settings.
    private FolderSettingsViewState.SectionState simpleFolderSection(
            String heading,
            Optional<Path> folderPath,
            String helperText,
            String feedbackMessage
    ) {
        return new FolderSettingsViewState.SectionState(
                heading,
                folderPath.map(Path::toString).orElse(""),
                helperText,
                SAVE_LABEL,
                BROWSE_LABEL,
                "",
                false,
                false,
                false,
                true,
                feedbackMessage,
                false
        );
    }

    // Keeps folder path validation shared across legacy and new save actions.
    private ValidationResult validate(String folderInput) {
        String normalizedInput = Objects.requireNonNullElse(folderInput, "").trim();
        if (normalizedInput.isEmpty()) {
            return ValidationResult.invalid(BLANK);
        }

        Path folderPath;
        try {
            folderPath = Paths.get(normalizedInput).normalize();
        } catch (InvalidPathException exception) {
            return ValidationResult.invalid(INVALID);
        }

        if (folderPath.toString().isBlank()) {
            return ValidationResult.invalid(INVALID);
        }

        if (!writableFolderProbe.test(folderPath)) {
            return ValidationResult.invalid(UNWRITABLE);
        }

        return ValidationResult.valid(folderPath);
    }

    // Resolves the legacy project override toggle state without affecting Auth, Backlog, or Results.
    private boolean resolveProjectOverrideEnabled(boolean projectAvailable, Optional<Path> currentProjectFilePath, Optional<Path> projectOverride) {
        if (!projectAvailable) {
            resetProjectOverrideUiState();
            return false;
        }

        boolean persistedOverrideEnabled = projectOverride.isPresent() && folderSettingsStore.isCurrentProjectOverrideEnabled();

        if (currentProjectFilePath.isPresent()) {
            Path projectIdentity = currentProjectFilePath.orElseThrow();
            if (projectIdentity.equals(projectOverrideUiIdentity.orElse(null))) {
                projectOverrideUiEnabled = persistedOverrideEnabled;
                return persistedOverrideEnabled;
            }
            rememberProjectOverrideUiState(projectIdentity, persistedOverrideEnabled);
            return persistedOverrideEnabled;
        }

        if (projectOverride.isPresent()) {
            projectOverrideUiEnabled = persistedOverrideEnabled;
        }
        return projectOverrideUiEnabled;
    }

    // Remembers the project identity for the legacy project override UI toggle.
    private void rememberProjectOverrideUiState(Path projectIdentity, boolean enabled) {
        projectOverrideUiIdentity = Optional.of(projectIdentity.normalize());
        projectOverrideUiEnabled = enabled;
    }

    // Clears the legacy project override UI toggle cache when project context disappears.
    private void resetProjectOverrideUiState() {
        projectOverrideUiIdentity = Optional.empty();
        projectOverrideUiEnabled = false;
    }

    // Determines whether the legacy project override controls can be shown.
    private boolean projectContextAvailable(Optional<Path> currentProjectFilePath) {
        return currentProjectFilePath.isPresent()
                || projectOverrideUiEnabled
                || folderSettingsStore.findCurrentProjectOverride().isPresent();
    }

    // Converts resolver source enums into legacy summary labels used by the current settings tab.
    private String toSummarySourceLabel(EffectiveFolderSource source) {
        return switch (source) {
            case PROJECT_OVERRIDE -> "From project override";
            case GLOBAL_DEFAULT -> "From global default";
            case FALLBACK_DEFAULT -> "From fallback default";
        };
    }

    // Placeholder probe keeps the existing permissive validation behavior until real filesystem checks are wired.
    private static boolean isWritableFolder(Path folderPath) {
        return true;
    }

    public enum Scope {
        AUTH,
        BACKLOG,
        RESULTS,
        GLOBAL,
        PROJECT
    }

    private enum FeedbackTarget {
        AUTH,
        BACKLOG,
        RESULTS
    }

    // Internal validation value object keeps the public save API compact.
    private record ValidationResult(boolean valid, Path path, String message) {
        // Creates a successful validation result with the normalized path.
        private static ValidationResult valid(Path path) {
            return new ValidationResult(true, path, "");
        }

        // Creates a failed validation result with the user-facing message.
        private static ValidationResult invalid(String message) {
            return new ValidationResult(false, null, message);
        }
    }
}
