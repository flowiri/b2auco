package com.example.b2auco.settings;

import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class PreferencesFolderSettingsStore implements FolderSettingsStore {
    private static final String GLOBAL_DEFAULT_KEY = "b2auco.folder.global-default";
    private static final String PROJECT_OVERRIDE_KEY = "b2auco.folder.project-override";
    private static final String PROJECT_OVERRIDE_ENABLED_KEY = "b2auco.folder.project-override.enabled";

    private final Preferences preferences;
    private final PersistedObject extensionData;

    public PreferencesFolderSettingsStore(Preferences preferences, PersistedObject extensionData) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.extensionData = Objects.requireNonNull(extensionData, "extensionData");
    }

    @Override
    public Optional<Path> findGlobalDefault() {
        return readPath(preferences.getString(GLOBAL_DEFAULT_KEY), GLOBAL_DEFAULT_KEY);
    }

    @Override
    public void saveGlobalDefault(Path folderPath) {
        preferences.setString(GLOBAL_DEFAULT_KEY, normalizeRequiredPath(folderPath, "folderPath").toString());
    }

    @Override
    public Optional<Path> findCurrentProjectOverride() {
        return readPath(extensionData.getString(PROJECT_OVERRIDE_KEY), PROJECT_OVERRIDE_KEY);
    }

    @Override
    public boolean isCurrentProjectOverrideEnabled() {
        Optional<Path> currentProjectOverride = findCurrentProjectOverride();
        if (currentProjectOverride.isEmpty()) {
            return false;
        }
        String storedValue = extensionData.getString(PROJECT_OVERRIDE_ENABLED_KEY);
        return storedValue == null || storedValue.isBlank() || Boolean.parseBoolean(storedValue);
    }

    @Override
    public void saveCurrentProjectOverride(Path folderPath) {
        extensionData.setString(PROJECT_OVERRIDE_KEY, normalizeRequiredPath(folderPath, "folderPath").toString());
        extensionData.setString(PROJECT_OVERRIDE_ENABLED_KEY, Boolean.TRUE.toString());
    }

    @Override
    public void setCurrentProjectOverrideEnabled(boolean enabled) {
        if (findCurrentProjectOverride().isEmpty()) {
            return;
        }
        extensionData.setString(PROJECT_OVERRIDE_ENABLED_KEY, Boolean.toString(enabled));
    }

    @Override
    public void clearCurrentProjectOverride() {
        extensionData.deleteString(PROJECT_OVERRIDE_KEY);
        extensionData.deleteString(PROJECT_OVERRIDE_ENABLED_KEY);
    }

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
