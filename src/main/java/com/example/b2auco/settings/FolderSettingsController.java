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

    public FolderSaveResult saveGlobalFolder(String folderInput) {
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.of(validation.message()), Optional.empty());
            return new FolderSaveResult(Scope.GLOBAL, false, validation.message(), viewState);
        }

        folderSettingsStore.saveGlobalDefault(validation.path());
        FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.of(SAVED), Optional.empty());
        return new FolderSaveResult(Scope.GLOBAL, true, SAVED, viewState);
    }

    public FolderSaveResult saveProjectOverride(String folderInput) {
        // Capture project identity once — project.id() may drift across calls
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        if (projectFilePath.isEmpty()) {
            resetProjectOverrideUiState();
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.of(PROJECT_HELPER_UNAVAILABLE));
            return new FolderSaveResult(Scope.PROJECT, false, PROJECT_HELPER_UNAVAILABLE, viewState);
        }

        rememberProjectOverrideUiState(projectFilePath.orElseThrow(), true);

        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.of(validation.message()));
            return new FolderSaveResult(Scope.PROJECT, false, validation.message(), viewState);
        }

        folderSettingsStore.saveProjectOverride(projectFilePath.orElseThrow(), validation.path());
        // Pass the same projectFilePath into buildViewState so the read-back uses the exact same key
        FolderSettingsViewState viewState = buildViewState(projectFilePath, Optional.empty(), Optional.of(SAVED));
        return new FolderSaveResult(Scope.PROJECT, true, SAVED, viewState);
    }

    public FolderSettingsViewState setProjectOverrideEnabled(boolean enabled) {
        Optional<Path> currentProjectFilePath = currentProjectFilePathSupplier.get();
        if (currentProjectFilePath.isEmpty()) {
            resetProjectOverrideUiState();
            return buildViewState(currentProjectFilePath, Optional.empty(), Optional.of(PROJECT_HELPER_UNAVAILABLE));
        }

        Path projectIdentity = currentProjectFilePath.orElseThrow();
        rememberProjectOverrideUiState(projectIdentity, enabled);
        if (!enabled) {
            folderSettingsStore.clearProjectOverride(projectIdentity);
            return buildViewState(currentProjectFilePath, Optional.empty(), Optional.of(OVERRIDE_DISABLED));
        }

        return buildViewState(currentProjectFilePath, Optional.empty(), Optional.empty());
    }

    private FolderSettingsViewState buildViewState(
            Optional<Path> currentProjectFilePath,
            Optional<String> globalFeedback,
            Optional<String> projectFeedback
    ) {
        Optional<Path> projectOverride = currentProjectFilePath.flatMap(folderSettingsStore::findProjectOverride);
        boolean projectAvailable = currentProjectFilePath.isPresent();
        boolean projectOverrideEnabled = resolveProjectOverrideEnabled(currentProjectFilePath, projectOverride);
        EffectiveFolderSelection effectiveFolder = effectiveFolderResolver.resolve(currentProjectFilePath, Optional.empty());

        FolderSettingsViewState.SectionState globalSection = new FolderSettingsViewState.SectionState(
                GLOBAL_HEADING,
                folderSettingsStore.findGlobalDefault().map(Path::toString).orElse(""),
                GLOBAL_HELPER,
                SAVE_LABEL,
                BROWSE_LABEL,
                "",
                false,
                false,
                false,
                true,
                globalFeedback.orElse("")
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
                projectFeedback.orElse("")
        );

        return new FolderSettingsViewState(
                TITLE,
                INTRO,
                SUMMARY_LABEL,
                effectiveFolder.folderPath().toString(),
                toSummarySourceLabel(effectiveFolder.source()),
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

    private boolean resolveProjectOverrideEnabled(Optional<Path> currentProjectFilePath, Optional<Path> projectOverride) {
        if (projectOverride.isPresent()) {
            Path projectIdentity = currentProjectFilePath.orElseThrow();
            rememberProjectOverrideUiState(projectIdentity, true);
            return true;
        }

        if (currentProjectFilePath.isEmpty()) {
            resetProjectOverrideUiState();
            return false;
        }

        Path projectIdentity = currentProjectFilePath.orElseThrow();
        if (projectIdentity.equals(projectOverrideUiIdentity.orElse(null))) {
            return projectOverrideUiEnabled;
        }

        rememberProjectOverrideUiState(projectIdentity, false);
        return false;
    }

    private void rememberProjectOverrideUiState(Path projectIdentity, boolean enabled) {
        projectOverrideUiIdentity = Optional.of(projectIdentity.normalize());
        projectOverrideUiEnabled = enabled;
    }

    private void resetProjectOverrideUiState() {
        projectOverrideUiIdentity = Optional.empty();
        projectOverrideUiEnabled = false;
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
