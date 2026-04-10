package com.example.b2auco.location;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class OutputDirectoryResolver {
    private static final String PROJECT_DIRECTORY_AVAILABLE = "PROJECT_DIRECTORY_AVAILABLE";
    private static final String NO_PROJECT_DIRECTORY = "NO_PROJECT_DIRECTORY";
    private static final String PROJECT_DIRECTORY_UNUSABLE = "PROJECT_DIRECTORY_UNUSABLE:";

    public ResolvedOutputDirectory resolveDefaultOutputDirectory(Optional<Path> projectDirectory) {
        Optional<Path> validatedProjectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory");
        Path fallbackDirectory = Path.of(System.getProperty("user.home"), ".b2auco", "exports");

        if (validatedProjectDirectory.isEmpty()) {
            return prepareFallbackDirectory(fallbackDirectory, NO_PROJECT_DIRECTORY);
        }

        Path preferredProjectOutput = validatedProjectDirectory.get()
                .resolve(".b2auco")
                .resolve("exports");

        try {
            Files.createDirectories(preferredProjectOutput);
            return new ResolvedOutputDirectory(preferredProjectOutput, false, PROJECT_DIRECTORY_AVAILABLE);
        } catch (IOException | SecurityException exception) {
            return prepareFallbackDirectory(fallbackDirectory, PROJECT_DIRECTORY_UNUSABLE + exception.getClass().getSimpleName());
        }
    }

    private ResolvedOutputDirectory prepareFallbackDirectory(Path fallbackDirectory, String reason) {
        try {
            Files.createDirectories(fallbackDirectory);
            return new ResolvedOutputDirectory(fallbackDirectory, true, reason);
        } catch (IOException | SecurityException exception) {
            throw new IllegalStateException("Unable to prepare fallback output directory", exception);
        }
    }
}
