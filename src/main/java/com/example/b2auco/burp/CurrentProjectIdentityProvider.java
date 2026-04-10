package com.example.b2auco.burp;

import burp.api.montoya.MontoyaApi;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CurrentProjectIdentityProvider {
    private static final List<String> PROJECT_PATH_METHODS = List.of(
            "path",
            "projectPath",
            "projectFile",
            "file"
    );

    public Optional<Path> findCurrentProjectFilePath(MontoyaApi api) {
        MontoyaApi montoyaApi = Objects.requireNonNull(api, "api");
        try {
            return extractProjectPath(montoyaApi.project());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<Path> extractProjectPath(Object project) {
        if (project == null) {
            return Optional.empty();
        }

        for (String methodName : PROJECT_PATH_METHODS) {
            Optional<Path> discoveredPath = invokeProjectPathMethod(project, methodName);
            if (discoveredPath.isPresent()) {
                return discoveredPath;
            }
        }

        return Optional.empty();
    }

    private Optional<Path> invokeProjectPathMethod(Object project, String methodName) {
        try {
            Method method = project.getClass().getMethod(methodName);
            if (method.getParameterCount() != 0) {
                return Optional.empty();
            }
            return toPath(method.invoke(project));
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<Path> toPath(Object candidate) {
        Path path;
        if (candidate instanceof Path candidatePath) {
            path = candidatePath;
        } else if (candidate instanceof String stringPath) {
            if (stringPath.isBlank()) {
                return Optional.empty();
            }
            try {
                path = Path.of(stringPath);
            } catch (RuntimeException exception) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }

        Path normalizedPath = path.normalize();
        if (normalizedPath.toString().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(normalizedPath);
    }
}
