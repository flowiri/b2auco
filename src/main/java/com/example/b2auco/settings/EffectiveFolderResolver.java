package com.example.b2auco.settings;

import com.example.b2auco.location.OutputDirectoryResolver;
import com.example.b2auco.location.ResolvedOutputDirectory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class EffectiveFolderResolver {
    private final FolderSettingsStore folderSettingsStore;
    private final OutputDirectoryResolver outputDirectoryResolver;

    public EffectiveFolderResolver(FolderSettingsStore folderSettingsStore, OutputDirectoryResolver outputDirectoryResolver) {
        this.folderSettingsStore = Objects.requireNonNull(folderSettingsStore, "folderSettingsStore");
        this.outputDirectoryResolver = Objects.requireNonNull(outputDirectoryResolver, "outputDirectoryResolver");
    }

    public EffectiveFolderSelection resolve(Optional<Path> projectFilePath, Optional<Path> projectDirectory) {
        Objects.requireNonNull(projectFilePath, "projectFilePath");
        Objects.requireNonNull(projectDirectory, "projectDirectory");

        Optional<Path> projectOverride = folderSettingsStore.findCurrentProjectOverride();
        if (projectOverride.isPresent()) {
            return new EffectiveFolderSelection(projectOverride.orElseThrow(), EffectiveFolderSource.PROJECT_OVERRIDE, "PROJECT_OVERRIDE");
        }

        Optional<Path> globalDefault = folderSettingsStore.findGlobalDefault();
        if (globalDefault.isPresent()) {
            return new EffectiveFolderSelection(globalDefault.orElseThrow(), EffectiveFolderSource.GLOBAL_DEFAULT, "GLOBAL_DEFAULT");
        }

        ResolvedOutputDirectory fallback = outputDirectoryResolver.resolveDefaultOutputDirectory(projectDirectory);
        return new EffectiveFolderSelection(fallback.outputDirectory(), EffectiveFolderSource.FALLBACK_DEFAULT, fallback.reason());
    }
}
