package com.example.b2auco.settings;

import com.example.b2auco.location.OutputDirectoryResolver;
import com.example.b2auco.location.ResolvedOutputDirectory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectiveFolderResolverTest {
    @Test
    void definesSourceEnumValuesForProjectOverrideGlobalDefaultAndFallbackDefault() {
        assertEquals(
                EnumSet.of(
                        EffectiveFolderSource.PROJECT_OVERRIDE,
                        EffectiveFolderSource.GLOBAL_DEFAULT,
                        EffectiveFolderSource.FALLBACK_DEFAULT
                ),
                EnumSet.allOf(EffectiveFolderSource.class)
        );
    }

    @Test
    void resolvesProjectOverrideBeforeGlobalDefaultBeforeFallbackDefault() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        Path projectFile = Path.of("C:/work/alpha.burp");
        Path projectOverride = Path.of("C:/work/project-override");
        Path globalDefault = Path.of("C:/work/global-default");
        EffectiveFolderResolver resolver = new EffectiveFolderResolver(store, new OutputDirectoryResolver());
        store.saveGlobalDefault(globalDefault);
        store.saveProjectOverride(projectFile, projectOverride);

        EffectiveFolderSelection selection = resolver.resolve(Optional.of(projectFile), Optional.of(Path.of("C:/work")));

        assertEquals(projectOverride, selection.folderPath());
        assertEquals(EffectiveFolderSource.PROJECT_OVERRIDE, selection.source());
    }

    @Test
    void resolvesGlobalDefaultWhenProjectOverrideDoesNotExist() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        Path projectFile = Path.of("C:/work/alpha.burp");
        Path globalDefault = Path.of("C:/work/global-default");
        EffectiveFolderResolver resolver = new EffectiveFolderResolver(store, new OutputDirectoryResolver());
        store.saveGlobalDefault(globalDefault);

        EffectiveFolderSelection selection = resolver.resolve(Optional.of(projectFile), Optional.of(Path.of("C:/work")));

        assertEquals(globalDefault, selection.folderPath());
        assertEquals(EffectiveFolderSource.GLOBAL_DEFAULT, selection.source());
    }

    @Test
    void resolvesFallbackDefaultWhenNoProjectOverrideOrGlobalDefaultExists() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        OutputDirectoryResolver fallbackResolver = new OutputDirectoryResolver();
        EffectiveFolderResolver resolver = new EffectiveFolderResolver(store, fallbackResolver);

        EffectiveFolderSelection selection = resolver.resolve(Optional.of(Path.of("C:/work/alpha.burp")), Optional.empty());
        ResolvedOutputDirectory expectedFallback = fallbackResolver.resolveDefaultOutputDirectory(Optional.empty());

        assertEquals(expectedFallback.outputDirectory(), selection.folderPath());
        assertEquals(EffectiveFolderSource.FALLBACK_DEFAULT, selection.source());
        assertEquals(expectedFallback.reason(), selection.reason());
    }

    private static final class InMemoryFolderSettingsStore implements FolderSettingsStore {
        private Optional<Path> globalDefault = Optional.empty();
        private final java.util.Map<Path, Path> projectOverrides = new java.util.HashMap<>();

        @Override
        public Optional<Path> findGlobalDefault() {
            return globalDefault;
        }

        @Override
        public void saveGlobalDefault(Path folderPath) {
            globalDefault = Optional.of(folderPath.normalize());
        }

        @Override
        public Optional<Path> findProjectOverride(Path projectFilePath) {
            return Optional.ofNullable(projectOverrides.get(projectFilePath.normalize()));
        }

        @Override
        public void saveProjectOverride(Path projectFilePath, Path folderPath) {
            projectOverrides.put(projectFilePath.normalize(), folderPath.normalize());
        }
    }
}
