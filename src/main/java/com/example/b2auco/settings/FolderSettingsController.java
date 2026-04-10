package com.example.b2auco.settings;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FolderSettingsController {
    private static final String TITLE = "Export folders";
    private static final String INTRO = "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.";
    private static final String SUMMARY_LABEL = "Current export folder";
    private static final String GLOBAL_HEADING = "Global default folder";
    private static final String GLOBAL_HELPER = "Used for all exports unless the current Burp project has its own override.";
    private static final String PROJECT_HEADING = "Current project override";
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

    private Optional<Path> projectOverrideUiIdentity = Optional.empty();
    private boolean projectOverrideUiEnabled;
    private FolderSettingsViewState.ActiveMode activeMode = FolderSettingsViewState.ActiveMode.USER_SETTING;
    private String globalFeedbackMessage = "";
    private String projectFeedbackMessage = "";

    public FolderSettingsController(
            FolderSettingsStore folderSettingsStore,
            EffectiveFolderResolver effectiveFolderResolver,
            Supplier<Optional<Path>> currentProjectFilePathSupplier
    ) {
        this(folderSettingsStore, effectiveFolderResolver, currentProjectFilePathSupplier, FolderSettingsController::isWritableFolder);
    }

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
    }

    public FolderSettingsViewState loadViewState() {
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        return buildViewState(projectFilePath, Optional.empty(), Optional.empty());
    }

    public FolderSettingsViewState showUserSettings() {
        activeMode = FolderSettingsViewState.ActiveMode.USER_SETTING;
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        return buildViewState(projectFilePath, Optional.empty(), Optional.empty());
    }

    public FolderSettingsViewState showProjectSettings() {
        activeMode = FolderSettingsViewState.ActiveMode.PROJECT_SETTING;
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        return buildViewState(projectFilePath, Optional.empty(), Optional.empty());
    }

    public FolderSaveResult saveGlobalFolder(String folderInput) {
        activeMode = FolderSettingsViewState.ActiveMode.USER_SETTING;
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            globalFeedbackMessage = validation.message();
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.of(validation.message()), Optional.empty());
            return new FolderSaveResult(Scope.GLOBAL, false, validation.message(), viewState);
        }

        folderSettingsStore.saveGlobalDefault(validation.path());
        globalFeedbackMessage = SAVED;
        FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.of(SAVED), Optional.empty());
        return new FolderSaveResult(Scope.GLOBAL, true, SAVED, viewState);
    }

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

        return new FolderSettingsViewState(
                TITLE,
                INTRO,
                SUMMARY_LABEL,
                effectiveFolder.folderPath().toString(),
                toSummarySourceLabel(effectiveFolder.source()),
                activeMode,
                globalSection,
                projectSection
        );
    }

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

    private void rememberProjectOverrideUiState(Path projectIdentity, boolean enabled) {
        projectOverrideUiIdentity = Optional.of(projectIdentity.normalize());
        projectOverrideUiEnabled = enabled;
    }

    private void resetProjectOverrideUiState() {
        projectOverrideUiIdentity = Optional.empty();
        projectOverrideUiEnabled = false;
    }

    private boolean projectContextAvailable(Optional<Path> currentProjectFilePath) {
        return currentProjectFilePath.isPresent()
                || projectOverrideUiEnabled
                || folderSettingsStore.findCurrentProjectOverride().isPresent();
    }

    private String toSummarySourceLabel(EffectiveFolderSource source) {
        return switch (source) {
            case PROJECT_OVERRIDE -> "From project override";
            case GLOBAL_DEFAULT -> "From global default";
            case FALLBACK_DEFAULT -> "From fallback default";
        };
    }

    private static boolean isWritableFolder(Path folderPath) {
        return true;
    }

    public enum Scope {
        GLOBAL,
        PROJECT
    }

    private record ValidationResult(boolean valid, Path path, String message) {
        private static ValidationResult valid(Path path) {
            return new ValidationResult(true, path, "");
        }

        private static ValidationResult invalid(String message) {
            return new ValidationResult(false, null, message);
        }
    }
}
