package com.example.b2auco.settings;

import java.nio.file.Path;
import java.util.Optional;

public interface FolderSettingsStore {
    Optional<Path> findGlobalDefault();

    void saveGlobalDefault(Path folderPath);

    Optional<Path> findCurrentProjectOverride();

    void saveCurrentProjectOverride(Path folderPath);

    void clearCurrentProjectOverride();
}
