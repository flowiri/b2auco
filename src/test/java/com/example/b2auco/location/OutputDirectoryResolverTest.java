package com.example.b2auco.location;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    }

    @Test
    void emptyProjectDirectoryFallsBackToUserHomeExportsFolder() {
        OutputDirectoryResolver resolver = new OutputDirectoryResolver();

        ResolvedOutputDirectory resolved = resolver.resolveDefaultOutputDirectory(Optional.empty());

        assertEquals(Path.of(System.getProperty("user.home"), "b2auco", "exports"), resolved.outputDirectory());
        assertTrue(resolved.usedFallback());
    }

    @Test
    void unusableProjectDirectoryFallsBackToSameHomeFolder() {
        OutputDirectoryResolver resolver = new OutputDirectoryResolver();
        Path missingParent = tempDir.resolve("missing-parent").resolve("project-dir");
        Path impossibleChild = missingParent.resolve(Path.of("bad", "..", "exports"));

        ResolvedOutputDirectory resolved = resolver.resolveDefaultOutputDirectory(Optional.of(impossibleChild));

        assertEquals(Path.of(System.getProperty("user.home"), "b2auco", "exports"), resolved.outputDirectory());
        assertTrue(resolved.usedFallback());
    }
}
