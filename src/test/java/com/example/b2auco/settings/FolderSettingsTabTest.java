package com.example.b2auco.settings;

import com.example.b2auco.location.OutputDirectoryResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderSettingsTabTest {
    @Test
    void loadViewStateIncludesApprovedTitleSummaryAndSourceCopy() {
        FolderSettingsController controller = new FolderSettingsController(
                new InMemoryFolderSettingsStore(),
                new EffectiveFolderResolver(new InMemoryFolderSettingsStore(), new OutputDirectoryResolver()),
                () -> Optional.empty()
        );

        FolderSettingsViewState state = controller.loadViewState();

        assertEquals("Export folders", state.title());
        assertEquals(
                "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.",
                state.introText()
        );
        assertEquals("Current export folder", state.summaryLabel());
        assertEquals("From fallback default", state.summarySourceLabel());
        assertEquals("Global default folder", state.globalSection().heading());
        assertEquals("Current project override", state.projectSection().heading());
    }

    @Test
    void loadViewStateDisablesProjectSectionWhenNoProjectFileIdentityExists() {
        FolderSettingsController controller = new FolderSettingsController(
                new InMemoryFolderSettingsStore(),
                new EffectiveFolderResolver(new InMemoryFolderSettingsStore(), new OutputDirectoryResolver()),
                Optional::<Path>empty
        );

        FolderSettingsViewState state = controller.loadViewState();

        assertFalse(state.projectSection().enabled());
        assertEquals(
                "Open or save a Burp project file to set a project-specific override.",
                state.projectSection().helperText()
        );
    }

    @Test
    void saveGlobalFolderReturnsInlineValidationForBlankFolder() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                Optional::<Path>empty
        );

        FolderSaveResult result = controller.saveGlobalFolder("   ");

        assertFalse(result.success());
        assertEquals("Choose a folder before saving.", result.message());
        assertEquals(FolderSettingsController.Scope.GLOBAL, result.scope());
    }

    @Test
    void saveFolderReturnsExactInlineMessagesForInvalidAndUnwritablePaths() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(Path.of("C:/work/project.burp")),
                candidate -> !candidate.toString().contains("locked")
        );

        FolderSaveResult invalidResult = controller.saveGlobalFolder("invalid::path");
        FolderSaveResult unwritableResult = controller.saveProjectOverride("C:/locked/folder");

        assertFalse(invalidResult.success());
        assertEquals("Enter a valid folder path.", invalidResult.message());
        assertFalse(unwritableResult.success());
        assertEquals("This folder isn’t writable. Choose another location.", unwritableResult.message());
    }

    @Test
    void successfulSaveRefreshesEffectiveFolderSummaryAndReturnsSavedMessage() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(Path.of("C:/work/project.burp"))
        );

        FolderSaveResult globalResult = controller.saveGlobalFolder("C:/work/global");
        FolderSaveResult projectResult = controller.saveProjectOverride("C:/work/project");

        assertTrue(globalResult.success());
        assertEquals("Folder saved.", globalResult.message());
        assertEquals(Path.of("C:/work/global"), Path.of(globalResult.viewState().summaryFolderPath()));
        assertEquals("From global default", globalResult.viewState().summarySourceLabel());

        assertTrue(projectResult.success());
        assertEquals("Folder saved.", projectResult.message());
        assertEquals(Path.of("C:/work/project"), Path.of(projectResult.viewState().summaryFolderPath()));
        assertEquals("From project override", projectResult.viewState().summarySourceLabel());
        assertEquals(Path.of("C:/work/global"), store.findGlobalDefault().orElseThrow());
    }

    private static final class InMemoryFolderSettingsStore implements FolderSettingsStore {
        private Optional<Path> globalDefault = Optional.empty();
        private final Map<Path, Path> projectOverrides = new HashMap<>();

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
