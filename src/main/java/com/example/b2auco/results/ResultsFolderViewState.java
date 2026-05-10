package com.example.b2auco.results;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Complete no-dependency view model for rendering the current results-folder state.
 */
public record ResultsFolderViewState(
        Status status,
        String statusMessage,
        List<ResultFileSummary> markdownFiles,
        Optional<ResultFileSummary> selectedFile,
        String content
) {
    /**
     * Defensively copies list state so callers cannot mutate the computed view model.
     */
    public ResultsFolderViewState {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(statusMessage, "statusMessage");
        markdownFiles = List.copyOf(Objects.requireNonNull(markdownFiles, "markdownFiles"));
        Objects.requireNonNull(selectedFile, "selectedFile");
        Objects.requireNonNull(content, "content");
    }

    /**
     * Stable status values used by future UI integration to choose display behavior.
     */
    public enum Status {
        READY,
        MISSING_DIRECTORY,
        NOT_A_DIRECTORY,
        EMPTY_DIRECTORY,
        UNREADABLE_DIRECTORY,
        UNREADABLE_FILE
    }
}
