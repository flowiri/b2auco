package com.example.b2auco.settings;

import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class PreferencesFolderSettingsStore implements FolderSettingsStore {
    private static final String AUTH_FOLDER_KEY = "b2auco.folder.auth";
    private static final String GLOBAL_DEFAULT_KEY = "b2auco.folder.global-default";
    private static final String RESULTS_FOLDER_KEY = "b2auco.folder.results";
    private static final String PROJECT_OVERRIDE_KEY = "b2auco.folder.project-override";
    private static final String PROJECT_OVERRIDE_ENABLED_KEY = "b2auco.folder.project-override.enabled";

    private final Preferences preferences;
    private final PersistedObject extensionData;

    public PreferencesFolderSettingsStore(Preferences preferences, PersistedObject extensionData) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.extensionData = Objects.requireNonNull(extensionData, "extensionData");
    }

    // Auth folder uses a new user-wide preference key because it is a new export target.
    @Override
    public Optional<Path> findAuthFolder() {
        return readPath(preferences.getString(AUTH_FOLDER_KEY), AUTH_FOLDER_KEY);
    }

    // Auth folder writes through the same path normalization as existing folder settings.
    @Override
    public void saveAuthFolder(Path folderPath) {
        preferences.setString(AUTH_FOLDER_KEY, normalizeRequiredPath(folderPath, "folderPath").toString());
    }

    // Backlog folder aliases the old global default key so existing users keep their configured folder.
    @Override
    public Optional<Path> findBacklogFolder() {
        return findGlobalDefault();
    }

    // Backlog saves intentionally overwrite the old global default destination.
    @Override
    public void saveBacklogFolder(Path folderPath) {
        saveGlobalDefault(folderPath);
    }

    // Results folder is independent from request export destinations.
    @Override
    public Optional<Path> findResultsFolder() {
        return readPath(preferences.getString(RESULTS_FOLDER_KEY), RESULTS_FOLDER_KEY);
    }

    // Results folder persists user-wide so the live view survives Burp restarts.
    @Override
    public void saveResultsFolder(Path folderPath) {
        preferences.setString(RESULTS_FOLDER_KEY, normalizeRequiredPath(folderPath, "folderPath").toString());
    }

    // Legacy global default reads from the same preference key that now represents Backlog.
    @Override
    public Optional<Path> findGlobalDefault() {
        return readPath(preferences.getString(GLOBAL_DEFAULT_KEY), GLOBAL_DEFAULT_KEY);
    }

    // Legacy global default writes to the same preference key that now represents Backlog.
    @Override
    public void saveGlobalDefault(Path folderPath) {
        preferences.setString(GLOBAL_DEFAULT_KEY, normalizeRequiredPath(folderPath, "folderPath").toString());
    }

    // Project override lookup is retained only for existing compatibility surfaces.
    @Override
    public Optional<Path> findCurrentProjectOverride() {
        return readPath(extensionData.getString(PROJECT_OVERRIDE_KEY), PROJECT_OVERRIDE_KEY);
    }

    // Project override enabled behavior stays unchanged for existing compatibility surfaces.
    @Override
    public boolean isCurrentProjectOverrideEnabled() {
        Optional<Path> currentProjectOverride = findCurrentProjectOverride();
        if (currentProjectOverride.isEmpty()) {
            return false;
        }
        String storedValue = extensionData.getString(PROJECT_OVERRIDE_ENABLED_KEY);
        return storedValue == null || storedValue.isBlank() || Boolean.parseBoolean(storedValue);
    }

    // Project override saving stays project-scoped for existing compatibility surfaces.
    @Override
    public void saveCurrentProjectOverride(Path folderPath) {
        extensionData.setString(PROJECT_OVERRIDE_KEY, normalizeRequiredPath(folderPath, "folderPath").toString());
        extensionData.setString(PROJECT_OVERRIDE_ENABLED_KEY, Boolean.TRUE.toString());
    }

    // Project override toggling stays project-scoped for existing compatibility surfaces.
    @Override
    public void setCurrentProjectOverrideEnabled(boolean enabled) {
        if (findCurrentProjectOverride().isEmpty()) {
            return;
        }
        extensionData.setString(PROJECT_OVERRIDE_ENABLED_KEY, Boolean.toString(enabled));
    }

    // Project override clearing stays project-scoped for existing compatibility surfaces.
    @Override
    public void clearCurrentProjectOverride() {
        extensionData.deleteString(PROJECT_OVERRIDE_KEY);
        extensionData.deleteString(PROJECT_OVERRIDE_ENABLED_KEY);
    }

    // Corrupt stored values are ignored to preserve the prior optional-read contract.
    private Optional<Path> readPath(String storedValue, String fieldName) {
        if (storedValue == null || storedValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(normalizeRequiredPath(Path.of(storedValue), fieldName));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    // Path normalization and blank rejection remain shared across every persisted folder.
    private Path normalizeRequiredPath(Path path, String fieldName) {
        Objects.requireNonNull(path, fieldName);
        try {
            Path normalizedPath = path.normalize();
            if (normalizedPath.toString().isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
            return normalizedPath;
        } catch (InvalidPathException | UnsupportedOperationException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid path", exception);
        }
    }
}
