package com.example.b2auco.location;

import java.nio.file.Path;

public record ResolvedOutputDirectory(Path outputDirectory, boolean usedFallback, String reason) {
}
