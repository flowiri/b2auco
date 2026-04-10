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
    private static final String PROJECT_HELPER_ENABLED = "Used only for this Burp project file and overrides the global folder.";
    private static final String PROJECT_HELPER_DISABLED = "Open or save a Burp project file to set a project-specific override.";
    private static final String SAVE_LABEL = "Save folder";
    private static final String BROWSE_LABEL = "Browse…";
    private static final String SAVED = "Folder saved.";
    private static final String BLANK = "Choose a folder before saving.";
    private static final String INVALID = "Enter a valid folder path.";
    private static final String UNWRITABLE = "This folder isn’t writable. Choose another location.";

    private final FolderSettingsStore folderSettingsStore;
    private final EffectiveFolderResolver effectiveFolderResolver;
    private final Supplier<Optional<Path>> currentProjectFilePathSupplier;
    private final Predicate<Path> writableFolderProbe;

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
        return buildViewState(Optional.empty(), Optional.empty());
    }

    public FolderSaveResult saveGlobalFolder(String folderInput) {
        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            FolderSettingsViewState viewState = buildViewState(Optional.of(validation.message()), Optional.empty());
            return new FolderSaveResult(Scope.GLOBAL, false, validation.message(), viewState);
        }

        folderSettingsStore.saveGlobalDefault(validation.path());
        FolderSettingsViewState viewState = buildViewState(Optional.of(SAVED), Optional.empty());
        return new FolderSaveResult(Scope.GLOBAL, true, SAVED, viewState);
    }

    public FolderSaveResult saveProjectOverride(String folderInput) {
        Optional<Path> projectFilePath = currentProjectFilePathSupplier.get();
        if (projectFilePath.isEmpty()) {
            FolderSettingsViewState viewState = buildViewState(Optional.empty(), Optional.of(PROJECT_HELPER_DISABLED));
            return new FolderSaveResult(Scope.PROJECT, false, PROJECT_HELPER_DISABLED, viewState);
        }

        ValidationResult validation = validate(folderInput);
        if (!validation.valid()) {
            FolderSettingsViewState viewState = buildViewState(Optional.empty(), Optional.of(validation.message()));
            return new FolderSaveResult(Scope.PROJECT, false, validation.message(), viewState);
        }

        folderSettingsStore.saveProjectOverride(projectFilePath.orElseThrow(), validation.path());
        FolderSettingsViewState viewState = buildViewState(Optional.empty(), Optional.of(SAVED));
        return new FolderSaveResult(Scope.PROJECT, true, SAVED, viewState);
    }

    private FolderSettingsViewState buildViewState(Optional<String> globalFeedback, Optional<String> projectFeedback) {
        Optional<Path> currentProjectFilePath = currentProjectFilePathSupplier.get();
        EffectiveFolderSelection effectiveFolder = effectiveFolderResolver.resolve(currentProjectFilePath, Optional.empty());

        FolderSettingsViewState.SectionState globalSection = new FolderSettingsViewState.SectionState(
                GLOBAL_HEADING,
                folderSettingsStore.findGlobalDefault().map(Path::toString).orElse(""),
                GLOBAL_HELPER,
                SAVE_LABEL,
                BROWSE_LABEL,
                true,
                globalFeedback.orElse("")
        );

        FolderSettingsViewState.SectionState projectSection = new FolderSettingsViewState.SectionState(
                PROJECT_HEADING,
                currentProjectFilePath.flatMap(folderSettingsStore::findProjectOverride).map(Path::toString).orElse(""),
                currentProjectFilePath.isPresent() ? PROJECT_HELPER_ENABLED : PROJECT_HELPER_DISABLED,
                SAVE_LABEL,
                BROWSE_LABEL,
                currentProjectFilePath.isPresent(),
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
