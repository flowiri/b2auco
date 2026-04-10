package com.example.b2auco.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.project.Project;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BurpProjectPathProviderTest {
    @Test
    void returnsEmptyWhenProjectExposesNoConcreteDirectory() {
        BurpProjectPathProvider provider = new BurpProjectPathProvider();

        Optional<Path> projectDirectory = provider.findProjectDirectory(montoyaApi(new ProjectWithoutPath()));

        assertTrue(projectDirectory.isEmpty());
    }

    @Test
    void returnsExactPathWhenConcreteProjectDirectoryIsExposed() {
        BurpProjectPathProvider provider = new BurpProjectPathProvider();

        Optional<Path> projectDirectory = provider.findProjectDirectory(
                montoyaApi(new ProjectWithPath("C:/work/burp/project-dir"))
        );

        assertEquals(Optional.of(Path.of("C:/work/burp/project-dir")), projectDirectory);
    }

    @Test
    void returnsEmptyWhenProjectPathDiscoveryThrows() {
        BurpProjectPathProvider provider = new BurpProjectPathProvider();

        Optional<Path> projectDirectory = provider.findProjectDirectory(montoyaApi(new ExplodingProjectPath()));

        assertTrue(projectDirectory.isEmpty());
    }

    private static MontoyaApi montoyaApi(Project project) {
        return (MontoyaApi) Proxy.newProxyInstance(
                MontoyaApi.class.getClassLoader(),
                new Class<?>[]{MontoyaApi.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("project")) {
                        return project;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static class ProjectWithoutPath implements Project {
        @Override
        public String name() {
            return "temporary-project";
        }

        @Override
        public String id() {
            return "project-id";
        }
    }

    private static final class ProjectWithPath extends ProjectWithoutPath {
        private final String path;

        private ProjectWithPath(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    private static final class ExplodingProjectPath extends ProjectWithoutPath {
        public String path() {
            throw new IllegalStateException("boom");
        }
    }
}
