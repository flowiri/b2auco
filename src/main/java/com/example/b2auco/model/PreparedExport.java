package com.example.b2auco.model;

import java.util.Arrays;
import java.util.Objects;

public final class PreparedExport {
    private final ExportTarget target;
    private final ExportFileName fileName;
    private final byte[] requestBytes;

    public PreparedExport(ExportTarget target, ExportFileName fileName, byte[] requestBytes) {
        this.target = Objects.requireNonNull(target, "target");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.requestBytes = Arrays.copyOf(Objects.requireNonNull(requestBytes, "requestBytes"), requestBytes.length);
    }

    public ExportTarget target() {
        return target;
    }

    public ExportFileName fileName() {
        return fileName;
    }

    public byte[] requestBytes() {
        return Arrays.copyOf(requestBytes, requestBytes.length);
    }
}
