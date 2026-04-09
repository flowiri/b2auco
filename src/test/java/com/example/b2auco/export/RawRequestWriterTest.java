package com.example.b2auco.export;

import com.example.b2auco.model.ExportFileName;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawRequestWriterTest {
    private static final byte[] RAW_REQUEST = "GET /api/users HTTP/1.1\r\nHost: example.com\r\n\r\n"
            .getBytes(StandardCharsets.ISO_8859_1);

    private final RawRequestWriter writer = new RawRequestWriter();

    @TempDir
    Path tempDir;

    @Test
    void writingPreparedExportReturnsPathToNewTxtFileUnderTargetDirectory() throws IOException {
        PreparedExport preparedExport = preparedExport(tempDir);

        Path savedPath = writer.write(preparedExport);

        assertEquals(tempDir.resolve("example.com-api-users.txt"), savedPath);
        assertTrue(Files.exists(savedPath));
        assertTrue(savedPath.getFileName().toString().endsWith(".txt"));
    }

    @Test
    void writingPreparedExportPreservesExactRequestBytes() throws IOException {
        PreparedExport preparedExport = preparedExport(tempDir);

        Path savedPath = writer.write(preparedExport);

        assertArrayEquals(RAW_REQUEST, Files.readAllBytes(savedPath));
    }

    @Test
    void collisionsAllocateIncrementingSuffixesWithoutOverwritingEarlierExports() throws IOException {
        PreparedExport preparedExport = preparedExport(tempDir);

        Path firstPath = writer.write(preparedExport);
        Path secondPath = writer.write(preparedExport);
        Path thirdPath = writer.write(preparedExport);

        assertEquals(tempDir.resolve("example.com-api-users.txt"), firstPath);
        assertEquals(tempDir.resolve("example.com-api-users-1.txt"), secondPath);
        assertEquals(tempDir.resolve("example.com-api-users-2.txt"), thirdPath);
        assertArrayEquals(RAW_REQUEST, Files.readAllBytes(firstPath));
        assertArrayEquals(RAW_REQUEST, Files.readAllBytes(secondPath));
        assertArrayEquals(RAW_REQUEST, Files.readAllBytes(thirdPath));
    }

    @Test
    void missingNestedOutputDirectoriesAreCreatedAutomaticallyBeforeWriting() throws IOException {
        Path nestedOutputDirectory = tempDir.resolve("exports/requests/raw");
        PreparedExport preparedExport = preparedExport(nestedOutputDirectory);

        Path savedPath = writer.write(preparedExport);

        assertEquals(nestedOutputDirectory.resolve("example.com-api-users.txt"), savedPath);
        assertTrue(Files.isDirectory(nestedOutputDirectory));
        assertArrayEquals(RAW_REQUEST, Files.readAllBytes(savedPath));
    }

    @Test
    void nullFilenameObjectIsRejectedBeforeWriting() throws Exception {
        PreparedExport preparedExport = preparedExport(tempDir);
        setField(preparedExport, "fileName", null);

        NullPointerException exception = assertThrows(NullPointerException.class, () -> writer.write(preparedExport));

        assertEquals("fileName", exception.getMessage());
    }

    @Test
    void nullOutputDirectoryIsRejectedBeforeWriting() throws Exception {
        PreparedExport preparedExport = preparedExport(tempDir);
        setField(preparedExport.target(), "outputDirectory", null);

        NullPointerException exception = assertThrows(NullPointerException.class, () -> writer.write(preparedExport));

        assertEquals("outputDirectory", exception.getMessage());
    }

    @Test
    void emptyRequestByteArrayIsRejectedBeforeWriting() {
        PreparedExport preparedExport = new PreparedExport(
                new ExportTarget(tempDir),
                new ExportFileName("example.com-api-users", "example.com-api-users.txt"),
                new byte[0]
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> writer.write(preparedExport));

        assertEquals("requestBytes must not be empty", exception.getMessage());
    }

    private PreparedExport preparedExport(Path outputDirectory) {
        return new PreparedExport(
                new ExportTarget(outputDirectory),
                new ExportFileName("example.com-api-users", "example.com-api-users.txt"),
                RAW_REQUEST
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
