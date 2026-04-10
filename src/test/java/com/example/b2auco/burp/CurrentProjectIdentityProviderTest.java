package com.example.b2auco.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.project.Project;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentProjectIdentityProviderTest {
    @Test
    void returnsExactProjectFilePathInsteadOfParentDirectory() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        Optional<Path> projectFilePath = provider.findCurrentProjectFilePath(
                montoyaApi(new ProjectWithProjectFilePath("C:/work/alpha.burp"))
        );

        assertEquals(Optional.of(Path.of("C:/work/alpha.burp")), projectFilePath);
    }

    @Test
    void preservesDistinctSiblingProjectFileIdentitiesForAlphaBurpAndBetaBurp() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        Optional<Path> alphaProjectFile = provider.findCurrentProjectFilePath(
                montoyaApi(new ProjectWithProjectFilePath("C:/work/alpha.burp"))
        );
        Optional<Path> betaProjectFile = provider.findCurrentProjectFilePath(
                montoyaApi(new ProjectWithProjectFilePath("C:/work/beta.burp"))
        );

        assertEquals(Optional.of(Path.of("C:/work/alpha.burp")), alphaProjectFile);
        assertEquals(Optional.of(Path.of("C:/work/beta.burp")), betaProjectFile);
        assertTrue(!alphaProjectFile.equals(betaProjectFile));
    }

    @Test
    void returnsEmptyWhenProjectPathIsBlankInvalidOrThrows() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        assertTrue(provider.findCurrentProjectFilePath(montoyaApi(new ProjectWithProjectFilePath("   "))).isEmpty());
        assertTrue(provider.findCurrentProjectFilePath(montoyaApi(new ExplodingProjectPath())).isEmpty());
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

    private static final class ProjectWithProjectFilePath extends ProjectWithoutPath {
        private final String projectFile;

        private ProjectWithProjectFilePath(String projectFile) {
            this.projectFile = projectFile;
        }

        public String projectFile() {
            return projectFile;
        }
    }

    private static final class ExplodingProjectPath extends ProjectWithoutPath {
        public String path() {
            throw new IllegalStateException("boom");
        }
    }
}
