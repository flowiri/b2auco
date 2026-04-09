package com.example.b2auco.export;

import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class RawRequestWriter {
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

        for (int suffix = 0; ; suffix++) {
            Path targetPath = outputDirectory.resolve(candidateFileName(validatedExport, suffix));
            try {
                return Files.write(targetPath, requestBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException ignored) {
                // try the next suffix
            }
        }
    }

    private String candidateFileName(PreparedExport preparedExport, int suffix) {
        if (suffix == 0) {
            return preparedExport.fileName().finalFileName();
        }
        return preparedExport.fileName().baseStem() + "-" + suffix + FILE_SUFFIX;
    }
}
