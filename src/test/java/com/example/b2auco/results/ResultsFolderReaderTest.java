package com.example.b2auco.results;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the filesystem-only reader contract without depending on a real external results folder.
 */
class ResultsFolderReaderTest {
    private final ResultsFolderReader reader = new ResultsFolderReader();

    @TempDir
    Path tempDir;

    /**
     * A missing configured directory should be recoverable and render as empty content.
     */
    @Test
    void missingResultsDirectoryReturnsStatusAndEmptyContent() {
        Path missingDirectory = tempDir.resolve("missing-results");

        ResultsFolderViewState viewState = reader.readNewestMarkdown(missingDirectory);

        assertEquals(ResultsFolderViewState.Status.MISSING_DIRECTORY, viewState.status());
        assertEquals("Results folder does not exist.", viewState.statusMessage());
        assertTrue(viewState.markdownFiles().isEmpty());
        assertTrue(viewState.selectedFile().isEmpty());
        assertEquals("", viewState.content());
    }

    /**
     * A configured file path should not throw because the settings UI can surface the status.
     */
    @Test
    void filePathReturnsNotDirectoryStatusAndEmptyContent() throws IOException {
        Path resultsFile = tempDir.resolve("results.md");
        Files.writeString(resultsFile, "not a directory", StandardCharsets.UTF_8);

        ResultsFolderViewState viewState = reader.readNewestMarkdown(resultsFile);

        assertEquals(ResultsFolderViewState.Status.NOT_A_DIRECTORY, viewState.status());
        assertEquals("Results path is not a folder.", viewState.statusMessage());
        assertTrue(viewState.markdownFiles().isEmpty());
        assertTrue(viewState.selectedFile().isEmpty());
        assertEquals("", viewState.content());
    }

    /**
     * An existing directory with no markdown results should render as an empty but valid state.
     */
    @Test
    void emptyFolderReturnsStatusAndEmptyContent() {
        ResultsFolderViewState viewState = reader.readNewestMarkdown(tempDir);

        assertEquals(ResultsFolderViewState.Status.EMPTY_DIRECTORY, viewState.status());
        assertEquals("Results folder contains no markdown files.", viewState.statusMessage());
        assertTrue(viewState.markdownFiles().isEmpty());
        assertTrue(viewState.selectedFile().isEmpty());
        assertEquals("", viewState.content());
    }

    /**
     * Only `.md` files are considered result documents; raw `.log` files remain ignored.
     */
    @Test
    void nonMarkdownFilesAreIgnoredWhenListingResults() throws IOException {
        Files.writeString(tempDir.resolve("latest.log"), "plain log", StandardCharsets.UTF_8);

        ResultsFolderViewState viewState = reader.readNewestMarkdown(tempDir);

        assertEquals(ResultsFolderViewState.Status.EMPTY_DIRECTORY, viewState.status());
        assertTrue(viewState.markdownFiles().isEmpty());
        assertEquals("", viewState.content());
    }

    /**
     * The default selection is the newest markdown file and content is decoded as UTF-8 text.
     */
    @Test
    void newestMarkdownFileByMtimeIsSelectedAndReadAsUtf8Text() throws IOException {
        Path olderResult = writeMarkdown("older.md", "# Older\n\nstale");
        Path newestResult = writeMarkdown("infinite_loop.md", sampleResultContent());
        Files.setLastModifiedTime(olderResult, FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));
        Files.setLastModifiedTime(newestResult, FileTime.from(Instant.parse("2026-01-02T00:00:00Z")));

        ResultsFolderViewState viewState = reader.readNewestMarkdown(tempDir);

        assertEquals(ResultsFolderViewState.Status.READY, viewState.status());
        assertEquals("Loaded newest markdown result.", viewState.statusMessage());
        assertEquals(2, viewState.markdownFiles().size());
        assertTrue(viewState.selectedFile().isPresent());
        assertEquals("infinite_loop.md", viewState.selectedFile().get().fileName());
        assertEquals(sampleResultContent(), viewState.content());
    }

    /**
     * The AUCO report shape uses a numeric request suffix plus `-report.md`, and must load as a normal markdown result.
     */
    @Test
    void aucoNumericReportMarkdownIsSelectedAndDisplayed() throws IOException {
        Path report = writeMarkdown("localhost-graphql-1-1-report.md", sampleAucoReportContent());
        Files.setLastModifiedTime(report, FileTime.from(Instant.parse("2026-05-12T02:45:00Z")));

        ResultsFolderViewState viewState = reader.readNewestMarkdown(tempDir);

        assertEquals(ResultsFolderViewState.Status.READY, viewState.status());
        assertEquals("localhost-graphql-1-1-report.md", viewState.selectedFile().orElseThrow().fileName());
        assertTrue(viewState.content().contains("# Scan Report: localhost-graphql-1-1"));
        assertTrue(viewState.content().contains("GraphQL systemCheckHost resolver allows OS command injection via host argument"));
    }

    /**
     * Sorted summaries let future UI controls show the latest result at the top.
     */
    @Test
    void listedMarkdownFilesAreSortedNewestFirst() throws IOException {
        Path newestResult = writeMarkdown("z-new.md", "# New");
        Path oldestResult = writeMarkdown("a-old.md", "# Old");
        Files.setLastModifiedTime(newestResult, FileTime.from(Instant.parse("2026-01-02T00:00:00Z")));
        Files.setLastModifiedTime(oldestResult, FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));

        ResultsFolderViewState viewState = reader.readNewestMarkdown(tempDir);

        assertEquals("z-new.md", viewState.markdownFiles().get(0).fileName());
        assertEquals("a-old.md", viewState.markdownFiles().get(1).fileName());
    }

    /**
     * The selected file summary should expose the same path and mtime read from disk.
     */
    @Test
    void selectedFileSummaryUsesActualPathAndMtime() throws IOException {
        Path resultFile = writeMarkdown("single.md", "# Single");
        Instant lastModified = Instant.parse("2026-01-03T00:00:00Z");
        Files.setLastModifiedTime(resultFile, FileTime.from(lastModified));

        ResultsFolderViewState viewState = reader.readNewestMarkdown(tempDir);

        ResultFileSummary selectedFile = viewState.selectedFile().orElseThrow();
        assertEquals(resultFile, selectedFile.path());
        assertEquals("single.md", selectedFile.fileName());
        assertEquals(lastModified, selectedFile.lastModified());
        assertFalse(viewState.content().isBlank());
    }

    /**
     * Bad UTF-8 is treated like an unreadable result so future UI can show a safe status.
     */
    @Test
    void invalidUtf8MarkdownFileReturnsUnreadableFileStatusAndEmptyContent() throws IOException {
        Path resultFile = tempDir.resolve("invalid.md");
        Files.write(resultFile, new byte[]{(byte) 0xC3, 0x28});

        ResultsFolderViewState viewState = reader.readNewestMarkdown(tempDir);

        assertEquals(ResultsFolderViewState.Status.UNREADABLE_FILE, viewState.status());
        assertEquals("Newest markdown result cannot be read.", viewState.statusMessage());
        assertTrue(viewState.selectedFile().isPresent());
        assertEquals(resultFile, viewState.selectedFile().get().path());
        assertEquals("", viewState.content());
    }

    /**
     * Large result files should be capped so refreshing the Swing tab cannot load unbounded content.
     */
    @Test
    void largeMarkdownFileIsTruncatedForPreview() throws IOException {
        Path resultFile = writeMarkdown("large.md", "x".repeat(300_000));

        ResultsFolderViewState viewState = reader.readNewestMarkdown(tempDir);

        assertEquals(ResultsFolderViewState.Status.READY, viewState.status());
        assertEquals(resultFile, viewState.selectedFile().orElseThrow().path());
        assertTrue(viewState.content().startsWith("x".repeat(100)));
        assertTrue(viewState.content().endsWith("[Result preview truncated at 262144 characters.]"));
    }

    /**
     * Writes a local markdown fixture so tests never depend on absolute developer paths.
     */
    private Path writeMarkdown(String fileName, String content) throws IOException {
        Path path = tempDir.resolve(fileName);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    /**
     * Mirrors the shape of a markdown/log result file while staying self-contained.
     */
    private String sampleResultContent() {
        return """
                # Test Result: Infinite Loop

                ## Request
                ```http
                GET /api/jobs/42 HTTP/1.1
                Host: example.test
                ```

                ## Log
                - status: failed
                - reason: loop detected after 1000 iterations
                - note: UTF-8 check: cafe
                """;
    }

    /**
     * Mirrors the user-provided AUCO markdown report without depending on the absolute developer-machine fixture path.
     */
    private String sampleAucoReportContent() {
        return """
                # Scan Report: localhost-graphql-1-1

                - Request file: `/Users/z/Desktop/auco/workspace/project1/tasks/wip/localhost-graphql-1-1.txt`
                - Target: `localhost:5050`
                - Status: `completed`
                - Confidence: `0.99`
                - Artifact files: `0`

                ## Validated Findings
                - **GraphQL systemCheckHost resolver allows OS command injection via host argument** (`high`)
                """;
    }
}
