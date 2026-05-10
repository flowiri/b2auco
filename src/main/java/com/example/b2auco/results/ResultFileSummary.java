package com.example.b2auco.results;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Lightweight display metadata for one markdown result file.
 */
public record ResultFileSummary(
        Path path,
        String fileName,
        Instant lastModified
) {
    /**
     * Validates required metadata before any UI/controller layer receives it.
     */
    public ResultFileSummary {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(lastModified, "lastModified");
    }
}
