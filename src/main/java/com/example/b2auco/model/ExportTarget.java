package com.example.b2auco.model;

import java.nio.file.Path;
import java.util.Objects;

public final class ExportTarget {
    private final Path outputDirectory;

    public ExportTarget(Path outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
    }

    public Path outputDirectory() {
        return outputDirectory;
    }
}
