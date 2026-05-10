package com.example.b2auco.settings;

import com.example.b2auco.location.OutputDirectoryResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderSettingsControllerTest {
    // Controller view state must expose all three configurable folders for the parent UI wiring slice.
    @Test
    void loadViewStateExposesAuthBacklogAndResultsSections() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        store.saveAuthFolder(Path.of("C:/work/auth"));
        store.saveBacklogFolder(Path.of("C:/work/backlog"));
        store.saveResultsFolder(Path.of("C:/work/results"));
        FolderSettingsController controller = controller(store);

        FolderSettingsViewState state = controller.loadViewState();

        assertEquals("Auth folder", state.authSection().heading());
        assertEquals("C:/work/auth", state.authSection().fieldValue());
        assertEquals("Backlog folder", state.backlogSection().heading());
        assertEquals("C:/work/backlog", state.backlogSection().fieldValue());
        assertEquals("Results folder", state.resultsSection().heading());
        assertEquals("C:/work/results", state.resultsSection().fieldValue());
    }

    // Save actions must route each input to its own store slot while preserving normalization and feedback.
    @Test
    void saveActionsPersistAuthBacklogAndResultsFolders() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = controller(store);

        FolderSaveResult authResult = controller.saveAuthFolder("C:/work/auth/./requests");
        FolderSaveResult backlogResult = controller.saveBacklogFolder("C:/work/backlog/../backlog");
        FolderSaveResult resultsResult = controller.saveResultsFolder("C:/work/results/./latest");

        assertTrue(authResult.success());
        assertEquals(FolderSettingsController.Scope.AUTH, authResult.scope());
        assertTrue(backlogResult.success());
        assertEquals(FolderSettingsController.Scope.BACKLOG, backlogResult.scope());
        assertTrue(resultsResult.success());
        assertEquals(FolderSettingsController.Scope.RESULTS, resultsResult.scope());
        assertEquals(Path.of("C:/work/auth/requests"), store.findAuthFolder().orElseThrow());
        assertEquals(Path.of("C:/work/backlog"), store.findBacklogFolder().orElseThrow());
        assertEquals(Path.of("C:/work/results/latest"), store.findResultsFolder().orElseThrow());
    }

    // Validation behavior remains shared so each folder rejects blank input without mutating stored values.
    @Test
    void saveActionsRejectBlankInputWithoutMutatingStore() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = controller(store);

        FolderSaveResult authResult = controller.saveAuthFolder("   ");
        FolderSaveResult backlogResult = controller.saveBacklogFolder("   ");
        FolderSaveResult resultsResult = controller.saveResultsFolder("   ");

        assertFalse(authResult.success());
        assertFalse(backlogResult.success());
        assertFalse(resultsResult.success());
        assertEquals("Choose a folder before saving.", authResult.message());
        assertEquals("Choose a folder before saving.", backlogResult.message());
        assertEquals("Choose a folder before saving.", resultsResult.message());
        assertTrue(store.findAuthFolder().isEmpty());
        assertTrue(store.findBacklogFolder().isEmpty());
        assertTrue(store.findResultsFolder().isEmpty());
    }

    // Keeps controller setup focused on settings behavior, not Burp project integration.
    private static FolderSettingsController controller(InMemoryFolderSettingsStore store) {
        return new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                Optional::<Path>empty,
                path -> true
        );
    }

    private static final class InMemoryFolderSettingsStore implements FolderSettingsStore {
        private Optional<Path> authFolder = Optional.empty();
        private Optional<Path> backlogFolder = Optional.empty();
        private Optional<Path> resultsFolder = Optional.empty();
        private Optional<Path> currentProjectOverride = Optional.empty();
        private boolean currentProjectOverrideEnabled = true;

        // Stores auth as an independent user-wide folder slot.
        @Override
        public Optional<Path> findAuthFolder() {
            return authFolder;
        }

        // Normalizes auth paths the same way the production preferences-backed store does.
        @Override
        public void saveAuthFolder(Path folderPath) {
            authFolder = Optional.of(folderPath.normalize());
        }

        // Legacy global default reads from the backlog slot for migration compatibility.
        @Override
        public Optional<Path> findGlobalDefault() {
            return backlogFolder;
        }

        // Legacy global default writes to the backlog slot for migration compatibility.
        @Override
        public void saveGlobalDefault(Path folderPath) {
            saveBacklogFolder(folderPath);
        }

        // Backlog owns the existing export-folder behavior.
        @Override
        public Optional<Path> findBacklogFolder() {
            return backlogFolder;
        }

        // Normalizes backlog paths the same way the production preferences-backed store does.
        @Override
        public void saveBacklogFolder(Path folderPath) {
            backlogFolder = Optional.of(folderPath.normalize());
        }

        // Stores results as an independent user-wide folder slot.
        @Override
        public Optional<Path> findResultsFolder() {
            return resultsFolder;
        }

        // Normalizes results paths the same way the production preferences-backed store does.
        @Override
        public void saveResultsFolder(Path folderPath) {
            resultsFolder = Optional.of(folderPath.normalize());
        }

        // Keeps the legacy project override API available for existing resolver and tab tests.
        @Override
        public Optional<Path> findCurrentProjectOverride() {
            return currentProjectOverride;
        }

        // Keeps the legacy project override enabled flag available for existing resolver and tab tests.
        @Override
        public boolean isCurrentProjectOverrideEnabled() {
            return currentProjectOverride.isPresent() && currentProjectOverrideEnabled;
        }

        // Keeps the legacy project override write path available for existing resolver and tab tests.
        @Override
        public void saveCurrentProjectOverride(Path folderPath) {
            currentProjectOverride = Optional.of(folderPath.normalize());
            currentProjectOverrideEnabled = true;
        }

        // Keeps the legacy project override toggle available for existing resolver and tab tests.
        @Override
        public void setCurrentProjectOverrideEnabled(boolean enabled) {
            currentProjectOverrideEnabled = enabled;
        }

        // Keeps the legacy project override clear path available for existing resolver and tab tests.
        @Override
        public void clearCurrentProjectOverride() {
            currentProjectOverride = Optional.empty();
            currentProjectOverrideEnabled = false;
        }
    }
}
