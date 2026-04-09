package com.example.b2auco.naming;

import com.example.b2auco.model.ExportFileName;

import java.util.Objects;
import java.util.regex.Pattern;

public final class FilenamePolicy {
    private static final String ROOT_SEGMENT = "root";
    private static final String FILE_SUFFIX = ".txt";
    private static final Pattern DISALLOWED_CHARACTERS = Pattern.compile("[<>:\"/\\|?*\\p{Cntrl}\\s]+");
    private static final Pattern REPEATED_SEPARATORS = Pattern.compile("-+");

    public ExportFileName deriveFileName(String host, String pathWithoutQuery) {
        String normalizedHost = sanitizeSegment(Objects.requireNonNullElse(host, "").trim());
        String normalizedPath = normalizePathSegment(pathWithoutQuery);
        String baseStem = trimSeparators(normalizedHost + "-" + normalizedPath);
        if (baseStem.isBlank()) {
            baseStem = ROOT_SEGMENT;
        }
        return new ExportFileName(baseStem, baseStem + FILE_SUFFIX);
    }

    private String normalizePathSegment(String pathWithoutQuery) {
        if (pathWithoutQuery == null) {
            return ROOT_SEGMENT;
        }

        String trimmedPath = pathWithoutQuery.trim();
        if (trimmedPath.isEmpty()) {
            return ROOT_SEGMENT;
        }

        int queryIndex = trimmedPath.indexOf('?');
        String queryFreePath = queryIndex >= 0 ? trimmedPath.substring(0, queryIndex) : trimmedPath;
        String flattenedPath = queryFreePath.replace('/', '-').replace('\\', '-');
        String sanitizedPath = sanitizeSegment(flattenedPath);
        return sanitizedPath.isBlank() ? ROOT_SEGMENT : sanitizedPath;
    }

    private String sanitizeSegment(String input) {
        String replaced = DISALLOWED_CHARACTERS.matcher(input).replaceAll("-");
        String collapsed = REPEATED_SEPARATORS.matcher(replaced).replaceAll("-");
        return trimSeparators(collapsed);
    }

    private String trimSeparators(String value) {
        return value.replaceAll("^-+", "").replaceAll("-+$", "");
    }
}
