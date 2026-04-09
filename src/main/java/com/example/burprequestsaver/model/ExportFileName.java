package com.example.burprequestsaver.model;

import java.util.Objects;

public final class ExportFileName {
    private final String baseStem;
    private final String finalFileName;

    public ExportFileName(String baseStem, String finalFileName) {
        this.baseStem = Objects.requireNonNull(baseStem, "baseStem");
        this.finalFileName = Objects.requireNonNull(finalFileName, "finalFileName");
    }

    public String baseStem() {
        return baseStem;
    }

    public String finalFileName() {
        return finalFileName;
    }
}
