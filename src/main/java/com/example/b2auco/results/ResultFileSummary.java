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
        Instant created,
        Instant lastModified
) {
    /**
     * Validates required metadata before any UI/controller layer receives it.
     */
    public ResultFileSummary {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(created, "created");
        Objects.requireNonNull(lastModified, "lastModified");
    }

    /**
     * Formats report options as newest-first dropdown labels with creation time visible to the user.
     */
    public String displayLabel() {
        return "%s - %s".formatted(created, fileName);
    }
}
