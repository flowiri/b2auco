package com.example.b2auco.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.Extension;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.project.Project;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class B2aucoExtensionTest {
    @Test
    void initializeUsesProjectLocalExportsWhenProjectDirectoryIsAvailable() {
        RecordingUserInterface userInterface = new RecordingUserInterface();
        B2aucoExtension extension = new B2aucoExtension();

        extension.initialize(montoyaApi(userInterface, projectWithPath("C:/burp/project-dir")));

        SaveRequestsContextMenuProvider provider = (SaveRequestsContextMenuProvider) userInterface.provider();
        assertNotNull(provider);
        assertEquals(Path.of("C:/burp/project-dir", ".b2auco", "exports"), extractTarget(provider).outputDirectory());
    }

    @Test
    void initializeUsesHomeFallbackWhenProjectDirectoryIsUnavailable() {
        RecordingUserInterface userInterface = new RecordingUserInterface();
        B2aucoExtension extension = new B2aucoExtension();

        extension.initialize(montoyaApi(userInterface, projectWithoutPath()));

        SaveRequestsContextMenuProvider provider = (SaveRequestsContextMenuProvider) userInterface.provider();
        assertNotNull(provider);
        assertEquals(Path.of(System.getProperty("user.home"), "b2auco", "exports"), extractTarget(provider).outputDirectory());
    }

    private static MontoyaApi montoyaApi(RecordingUserInterface userInterface, Project project) {
        Extension extension = (Extension) Proxy.newProxyInstance(
                Extension.class.getClassLoader(),
                new Class<?>[]{Extension.class},
                (proxy, method, args) -> null
        );
        Logging logging = (Logging) Proxy.newProxyInstance(
                Logging.class.getClassLoader(),
                new Class<?>[]{Logging.class},
                (proxy, method, args) -> null
        );
        return (MontoyaApi) Proxy.newProxyInstance(
                MontoyaApi.class.getClassLoader(),
                new Class<?>[]{MontoyaApi.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "userInterface" -> userInterface.proxy();
                    case "extension" -> extension;
                    case "logging" -> logging;
                    case "project" -> project;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Project projectWithoutPath() {
        return new Project() {
            @Override
            public String name() {
                return "temporary";
            }

            @Override
            public String id() {
                return "project-id";
            }
        };
    }

    private static Project projectWithPath(String path) {
        class ProjectWithPath implements Project {
            @Override
            public String name() {
                return "disk-backed";
            }

            @Override
            public String id() {
                return "project-id";
            }

            public String path() {
                return path;
            }
        }
        return new ProjectWithPath();
    }

    private static com.example.b2auco.model.ExportTarget extractTarget(SaveRequestsContextMenuProvider provider) {
        try {
            java.lang.reflect.Field field = SaveRequestsContextMenuProvider.class.getDeclaredField("target");
            field.setAccessible(true);
            return (com.example.b2auco.model.ExportTarget) field.get(provider);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
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

    private static final class RecordingUserInterface {
        private final AtomicReference<ContextMenuItemsProvider> provider = new AtomicReference<>();

        private UserInterface proxy() {
            return (UserInterface) Proxy.newProxyInstance(
                    UserInterface.class.getClassLoader(),
                    new Class<?>[]{UserInterface.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("registerContextMenuItemsProvider")) {
                            provider.set((ContextMenuItemsProvider) args[0]);
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private ContextMenuItemsProvider provider() {
            return provider.get();
        }
    }
}
