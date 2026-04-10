package com.example.b2auco.settings;

import burp.api.montoya.persistence.Preferences;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

public final class PreferencesFolderSettingsStore implements FolderSettingsStore {
    private static final String GLOBAL_DEFAULT_KEY = "b2auco.folder.global-default";
    private static final String PROJECT_OVERRIDE_PREFIX = "b2auco.folder.project-override.";

    private final Preferences preferences;

    public PreferencesFolderSettingsStore(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    @Override
    public Optional<Path> findGlobalDefault() {
        return readPath(GLOBAL_DEFAULT_KEY);
    }

    @Override
    public void saveGlobalDefault(Path folderPath) {
        preferences.setString(GLOBAL_DEFAULT_KEY, normalizeRequiredPath(folderPath, "folderPath").toString());
    }

    @Override
    public Optional<Path> findProjectOverride(Path projectFilePath) {
        return readPath(projectOverrideKey(projectFilePath));
    }

    @Override
    public void saveProjectOverride(Path projectFilePath, Path folderPath) {
        preferences.setString(
                projectOverrideKey(projectFilePath),
                normalizeRequiredPath(folderPath, "folderPath").toString()
        );
    }

    @Override
    public void clearProjectOverride(Path projectFilePath) {
        preferences.deleteString(projectOverrideKey(projectFilePath));
    }

    private Optional<Path> readPath(String key) {
        String storedValue = preferences.getString(key);
        if (storedValue == null || storedValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(normalizeRequiredPath(Path.of(storedValue), key));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String projectOverrideKey(Path projectFilePath) {
        Path normalizedProjectFilePath = normalizeRequiredPath(projectFilePath, "projectFilePath");
        return PROJECT_OVERRIDE_PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalizedProjectFilePath.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
