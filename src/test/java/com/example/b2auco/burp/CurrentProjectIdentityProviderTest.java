package com.example.b2auco.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.project.Project;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentProjectIdentityProviderTest {
    @Test
    void prefersConcreteProjectPathOverProjectIdWhenBothAreAvailable() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        Optional<Path> projectIdentity = provider.findCurrentProjectFilePath(
                montoyaApi(new ProjectWithProjectFilePath("alpha-project", "C:/work/alpha.burp"))
        );

        assertEquals(Optional.of(Path.of("C:/work/alpha.burp")), projectIdentity);
    }

    @Test
    void preservesDistinctStableProjectIdentitiesForDifferentProjectPaths() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        Optional<Path> alphaProjectIdentity = provider.findCurrentProjectFilePath(
                montoyaApi(new ProjectWithProjectFilePath("alpha-project", "C:/work/alpha.burp"))
        );
        Optional<Path> betaProjectIdentity = provider.findCurrentProjectFilePath(
                montoyaApi(new ProjectWithProjectFilePath("beta-project", "C:/work/beta.burp"))
        );

        assertEquals(Optional.of(Path.of("C:/work/alpha.burp")), alphaProjectIdentity);
        assertEquals(Optional.of(Path.of("C:/work/beta.burp")), betaProjectIdentity);
        assertTrue(!alphaProjectIdentity.equals(betaProjectIdentity));
    }

    @Test
    void fallsBackToStableProjectIdentityFromProjectIdWhenNoProjectPathMethodExists() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        Optional<Path> projectIdentity = provider.findCurrentProjectFilePath(
                montoyaApi(new ProjectWithoutPath())
        );

        assertEquals(Optional.of(projectIdentityPath("project-id")), projectIdentity);
    }

    @Test
    void prefersStableProjectPathWhenProjectIdMayDriftAcrossCalls() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        Optional<Path> projectIdentity = provider.findCurrentProjectFilePath(
                montoyaApi(new ProjectWithProjectFilePath("project-id", "C:/work/alpha.burp"))
        );

        assertEquals(Optional.of(Path.of("C:/work/alpha.burp")), projectIdentity);
    }

    @Test
    void returnsFallbackWhenProjectPathMethodThrowsButProjectIdExists() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        Optional<Path> projectIdentity = provider.findCurrentProjectFilePath(
                montoyaApi(new ExplodingProjectPath())
        );

        assertEquals(Optional.of(projectIdentityPath("project-id")), projectIdentity);
    }

    @Test
    void returnsEmptyWhenProjectPathIsBlankInvalidOrThrowsAndNoProjectIdFallbackExists() {
        CurrentProjectIdentityProvider provider = new CurrentProjectIdentityProvider();

        assertTrue(provider.findCurrentProjectFilePath(montoyaApi(new BlankProjectId())).isEmpty());
        assertTrue(provider.findCurrentProjectFilePath(montoyaApi(new ExplodingProjectPathWithBlankId())).isEmpty());
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

    private static Path projectIdentityPath(String projectId) {
        return Path.of(".b2auco-project-id", encodedProjectId(projectId));
    }

    private static String encodedProjectId(String projectId) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(projectId.getBytes(StandardCharsets.UTF_8));
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

    private static class BlankProjectId extends ProjectWithoutPath {
        @Override
        public String id() {
            return "   ";
        }
    }

    private static final class ProjectWithProjectFilePath extends ProjectWithoutPath {
        private final String projectId;
        private final String projectFile;

        private ProjectWithProjectFilePath(String projectId, String projectFile) {
            this.projectId = projectId;
            this.projectFile = projectFile;
        }

        @Override
        public String id() {
            return projectId;
        }

        @SuppressWarnings("unused")
        public String projectFile() {
            return projectFile;
        }
    }

    private static final class ExplodingProjectPath extends ProjectWithoutPath {
        @SuppressWarnings("unused")
        public String path() {
            throw new IllegalStateException("boom");
        }
    }

    private static final class ExplodingProjectPathWithBlankId extends BlankProjectId {
        @SuppressWarnings("unused")
        public String path() {
            throw new IllegalStateException("boom");
        }
    }
}
