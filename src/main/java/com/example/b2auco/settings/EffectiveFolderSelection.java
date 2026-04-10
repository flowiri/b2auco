package com.example.b2auco.settings;

import java.nio.file.Path;
import java.util.Objects;

public record EffectiveFolderSelection(Path folderPath, EffectiveFolderSource source, String reason) {
    public EffectiveFolderSelection {
        Objects.requireNonNull(folderPath, "folderPath");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(reason, "reason");
    }
}
