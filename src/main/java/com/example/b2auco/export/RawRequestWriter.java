package com.example.b2auco.export;

import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;
import com.example.b2auco.workflow.PreparedExportPersister;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class RawRequestWriter implements PreparedExportPersister {
    private static final String FILE_SUFFIX = ".txt";

    public Path write(PreparedExport preparedExport) throws IOException {
        PreparedExport validatedExport = Objects.requireNonNull(preparedExport, "preparedExport");
        ExportTarget target = Objects.requireNonNull(validatedExport.target(), "target");
        Path outputDirectory = Objects.requireNonNull(target.outputDirectory(), "outputDirectory");
        Objects.requireNonNull(validatedExport.fileName(), "fileName");

        byte[] requestBytes = Objects.requireNonNull(validatedExport.requestBytes(), "requestBytes");
        if (requestBytes.length == 0) {
            throw new IllegalArgumentException("requestBytes must not be empty");
        }

        Files.createDirectories(outputDirectory);

        // Export files must always carry a numeric suffix, so the first candidate starts at `-1`.
        for (int suffix = 1; ; suffix++) {
            Path targetPath = outputDirectory.resolve(candidateFileName(validatedExport, suffix));
            try {
                return Files.write(targetPath, requestBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException ignored) {
                // try the next suffix
            }
        }
    }

    // Builds every candidate as `<base>-<number>.txt`; unsuffixed names are never written.
    private String candidateFileName(PreparedExport preparedExport, int suffix) {
        return preparedExport.fileName().baseStem() + "-" + suffix + FILE_SUFFIX;
    }
}
