package com.example.b2auco.settings;

import java.nio.file.Path;
import java.util.Optional;

public interface FolderSettingsStore {
    // Auth exports need their own configured destination without changing the existing backlog/global key.
    default Optional<Path> findAuthFolder() {
        return Optional.empty();
    }

    // Auth exports are user-wide for now, matching the new tab-level folder configuration.
    default void saveAuthFolder(Path folderPath) {
        throw new UnsupportedOperationException("Auth folder persistence is not implemented");
    }

    // Backlog exports keep using the previous global default semantics for migration compatibility.
    default Optional<Path> findBacklogFolder() {
        return findGlobalDefault();
    }

    // Backlog saves intentionally feed the old global default storage.
    default void saveBacklogFolder(Path folderPath) {
        saveGlobalDefault(folderPath);
    }

    // Results viewing needs a separate folder because it reads test output instead of writing requests.
    default Optional<Path> findResultsFolder() {
        return Optional.empty();
    }

    // Results folder is user-wide so Burp project changes do not hide the live test view.
    default void saveResultsFolder(Path folderPath) {
        throw new UnsupportedOperationException("Results folder persistence is not implemented");
    }

    // Legacy global default remains abstract so older in-memory stores keep defining their own backlog-compatible behavior.
    Optional<Path> findGlobalDefault();

    // Legacy global default writes remain abstract so older in-memory stores keep defining their own backlog-compatible behavior.
    void saveGlobalDefault(Path folderPath);

    // Legacy project override lookup remains abstract for existing resolver tests and current settings UI.
    Optional<Path> findCurrentProjectOverride();

    // Legacy project override enabled flag remains abstract for existing resolver tests and current settings UI.
    boolean isCurrentProjectOverrideEnabled();

    // Legacy project override save remains abstract for existing resolver tests and current settings UI.
    void saveCurrentProjectOverride(Path folderPath);

    // Legacy project override toggle remains abstract for existing resolver tests and current settings UI.
    void setCurrentProjectOverrideEnabled(boolean enabled);

    // Legacy project override clear remains abstract for existing resolver tests and current settings UI.
    void clearCurrentProjectOverride();
}
