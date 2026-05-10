package com.example.b2auco.results;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads markdown result files from a configured folder without depending on Burp or UI classes.
 */
public final class ResultsFolderReader {
    private static final int MAX_PREVIEW_CHARS = 256 * 1024;
    private static final String TRUNCATED_NOTICE = "\n\n[Result preview truncated at 262144 characters.]";

    /**
     * Lists markdown files, selects the newest file by mtime, and reads its UTF-8 text content.
     */
    public ResultsFolderViewState readNewestMarkdown(Path resultsDirectory) {
        Path directory = Objects.requireNonNull(resultsDirectory, "resultsDirectory");

        // Missing paths are expected before the user configures or creates a results folder.
        if (Files.notExists(directory)) {
            return empty(ResultsFolderViewState.Status.MISSING_DIRECTORY, "Results folder does not exist.");
        }
        // A configured file path is recoverable; report status instead of throwing into the UI.
        if (!Files.isDirectory(directory)) {
            return empty(ResultsFolderViewState.Status.NOT_A_DIRECTORY, "Results path is not a folder.");
        }

        List<ResultFileSummary> markdownFiles;
        try {
            markdownFiles = listMarkdownFiles(directory);
        } catch (IOException | SecurityException exception) {
            // Directory listing can fail from permissions or transient filesystem errors.
            return empty(ResultsFolderViewState.Status.UNREADABLE_DIRECTORY, "Results folder cannot be read.");
        }

        if (markdownFiles.isEmpty()) {
            return new ResultsFolderViewState(
                    ResultsFolderViewState.Status.EMPTY_DIRECTORY,
                    "Results folder contains no markdown files.",
                    List.of(),
                    Optional.empty(),
                    ""
            );
        }

        ResultFileSummary newestFile = markdownFiles.getFirst();
        try {
            String content = readPreview(newestFile.path());
            return new ResultsFolderViewState(
                    ResultsFolderViewState.Status.READY,
                    "Loaded newest markdown result.",
                    markdownFiles,
                    Optional.of(newestFile),
                    content
            );
        } catch (IOException | SecurityException exception) {
            // Invalid UTF-8 and unreadable files both become a non-fatal empty-content state.
            return new ResultsFolderViewState(
                    ResultsFolderViewState.Status.UNREADABLE_FILE,
                    "Newest markdown result cannot be read.",
                    markdownFiles,
                    Optional.of(newestFile),
                    ""
            );
        }
    }

    /**
     * Caps preview text so refreshing the Swing tab cannot load an unbounded result file into memory.
     */
    private String readPreview(Path path) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[8192];
        int totalCharacters = 0;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            while (totalCharacters < MAX_PREVIEW_CHARS) {
                int charactersToRead = Math.min(buffer.length, MAX_PREVIEW_CHARS - totalCharacters);
                int charactersRead = reader.read(buffer, 0, charactersToRead);
                if (charactersRead == -1) {
                    return content.toString();
                }
                content.append(buffer, 0, charactersRead);
                totalCharacters += charactersRead;
            }
            return reader.read() == -1 ? content.toString() : content.append(TRUNCATED_NOTICE).toString();
        }
    }

    /**
     * Returns regular `.md` files sorted newest first, with filename tie-breaks for stable tests.
     */
    private List<ResultFileSummary> listMarkdownFiles(Path directory) throws IOException {
        List<ResultFileSummary> markdownFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.md")) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    markdownFiles.add(summarize(path));
                }
            }
        }
        markdownFiles.sort(Comparator
                .comparing(ResultFileSummary::lastModified)
                .reversed()
                .thenComparing(ResultFileSummary::fileName));
        return markdownFiles;
    }

    /**
     * Captures filesystem metadata once so later rendering does not need more I/O.
     */
    private ResultFileSummary summarize(Path path) throws IOException {
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        return new ResultFileSummary(
                path,
                path.getFileName().toString(),
                lastModifiedTime.toInstant()
        );
    }

    /**
     * Builds a consistent empty view model for all folder-level failure states.
     */
    private ResultsFolderViewState empty(ResultsFolderViewState.Status status, String message) {
        return new ResultsFolderViewState(status, message, List.of(), Optional.empty(), "");
    }
}
