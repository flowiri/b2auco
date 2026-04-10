package com.example.b2auco.location;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputDirectoryResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void concreteProjectDirectoryResolvesToHiddenProjectExportsFolder() {
        OutputDirectoryResolver resolver = new OutputDirectoryResolver();

        ResolvedOutputDirectory resolved = resolver.resolveDefaultOutputDirectory(Optional.of(tempDir));

        assertEquals(tempDir.resolve(".b2auco").resolve("exports"), resolved.outputDirectory());
        assertFalse(resolved.usedFallback());
        assertEquals("PROJECT_DIRECTORY_AVAILABLE", resolved.reason());
    }

    @Test
    void emptyProjectDirectoryFallsBackToUserHomeExportsFolder() {
        OutputDirectoryResolver resolver = new OutputDirectoryResolver();

        ResolvedOutputDirectory resolved = resolver.resolveDefaultOutputDirectory(Optional.empty());

        assertEquals(Path.of(System.getProperty("user.home"), ".b2auco", "exports"), resolved.outputDirectory());
        assertTrue(resolved.usedFallback());
        assertEquals("NO_PROJECT_DIRECTORY", resolved.reason());
    }

    @Test
    void unusableProjectDirectoryFallsBackToSameHomeFolder() throws IOException {
        OutputDirectoryResolver resolver = new OutputDirectoryResolver();
        Path projectFile = tempDir.resolve("project-file.burp");
        Files.writeString(projectFile, "not a directory");

        ResolvedOutputDirectory resolved = resolver.resolveDefaultOutputDirectory(Optional.of(projectFile));

        assertEquals(Path.of(System.getProperty("user.home"), ".b2auco", "exports"), resolved.outputDirectory());
        assertTrue(resolved.usedFallback());
        assertTrue(resolved.reason().startsWith("PROJECT_DIRECTORY_UNUSABLE:"));
    }
}
