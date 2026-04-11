package com.example.b2auco.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.Extension;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.project.Project;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.core.Registration;
import com.example.b2auco.model.ExportTarget;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class B2aucoExtensionTest {
    @Test
    void initializeRegistersB2aucoSuiteTabAndContextMenuProvider() {
        RecordingUserInterface userInterface = new RecordingUserInterface();
        B2aucoExtension extension = new B2aucoExtension();

        extension.initialize(montoyaApi(userInterface, projectWithoutPath(), persistenceState(Map.of(), Map.of())));

        assertEquals("b2auco", userInterface.suiteTabTitle());
        assertInstanceOf(JPanel.class, userInterface.suiteTabComponent());
        assertNotNull(userInterface.provider());
    }

    @Test
    void initializeUsesPhaseThreeFallbackWhenNoSettingsExist() {
        RecordingUserInterface userInterface = new RecordingUserInterface();
        B2aucoExtension extension = new B2aucoExtension();

        extension.initialize(montoyaApi(userInterface, projectWithPath("C:/burp/project-file.burp"), persistenceState(Map.of(), Map.of())));

        SaveRequestsContextMenuProvider provider = (SaveRequestsContextMenuProvider) userInterface.provider();
        assertNotNull(provider);
        assertEquals(Path.of("C:/burp/.b2auco/exports"), extractResolvedTarget(provider).outputDirectory());
    }

    @Test
    void initializeUsesProjectOverrideForCurrentProject() {
        RecordingUserInterface userInterface = new RecordingUserInterface();
        PersistenceState persistenceState = persistenceState(
                Map.of(),
                Map.of("b2auco.folder.project-override", "C:/exports/project-override")
        );
        B2aucoExtension extension = new B2aucoExtension();

        extension.initialize(montoyaApi(userInterface, projectWithPath("C:/burp/current-project.burp"), persistenceState));

        SaveRequestsContextMenuProvider provider = (SaveRequestsContextMenuProvider) userInterface.provider();
        assertNotNull(provider);
        assertEquals(Path.of("C:/exports/project-override"), extractResolvedTarget(provider).outputDirectory());
    }

    @Test
    void initializeUsesCurrentProjectOverrideWhenProjectBecomesAvailableAfterStartup() {
        RecordingUserInterface userInterface = new RecordingUserInterface();
        PersistenceState persistenceState = persistenceState(
                Map.of("b2auco.folder.global-default", "C:/exports/global-default"),
                Map.of("b2auco.folder.project-override", "C:/exports/project-override")
        );
        B2aucoExtension extension = new B2aucoExtension();

        extension.initialize(montoyaApi(userInterface, sequenceProjectSupplier(
                projectWithId("   "),
                projectWithId("project-id")
        ), persistenceState));

        SaveRequestsContextMenuProvider provider = (SaveRequestsContextMenuProvider) userInterface.provider();
        assertNotNull(provider);
        assertEquals(Path.of("C:/exports/project-override"), extractResolvedTarget(provider).outputDirectory());
    }

    @Test
    void initializeKeepsUsingCurrentProjectOverrideWhenProjectIdentityChangesAcrossCalls() {
        RecordingUserInterface userInterface = new RecordingUserInterface();
        PersistenceState persistenceState = persistenceState(
                Map.of("b2auco.folder.global-default", "C:/exports/global-default"),
                Map.of("b2auco.folder.project-override", "C:/exports/project-override")
        );
        B2aucoExtension extension = new B2aucoExtension();

        extension.initialize(montoyaApi(userInterface, unstableProjectWithPathSequence(
                "C:/burp/first-project.burp",
                "C:/burp/second-project.burp"
        ), persistenceState));

        SaveRequestsContextMenuProvider provider = (SaveRequestsContextMenuProvider) userInterface.provider();
        assertNotNull(provider);
        assertEquals(Path.of("C:/exports/project-override"), extractResolvedTarget(provider).outputDirectory());
    }

    @Test
    void initializeUsesCurrentProjectOverrideWhenProjectIdChangesAcrossCalls() {
        RecordingUserInterface userInterface = new RecordingUserInterface();
        PersistenceState persistenceState = persistenceState(
                Map.of("b2auco.folder.global-default", "C:/exports/global-default"),
                Map.of("b2auco.folder.project-override", "C:/exports/project-override")
        );
        B2aucoExtension extension = new B2aucoExtension();

        extension.initialize(montoyaApi(userInterface, unstableProjectWithIdSequence(
                new String[]{"project-id-1", "project-id-2", "project-id-3"},
                "C:/burp/current-project.burp"
        ), persistenceState));

        SaveRequestsContextMenuProvider provider = (SaveRequestsContextMenuProvider) userInterface.provider();
        assertNotNull(provider);
        assertEquals(Path.of("C:/exports/project-override"), extractResolvedTarget(provider).outputDirectory());
    }

    private static MontoyaApi montoyaApi(RecordingUserInterface userInterface, Project project, PersistenceState persistenceState) {
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
        Persistence persistence = (Persistence) Proxy.newProxyInstance(
                Persistence.class.getClassLoader(),
                new Class<?>[]{Persistence.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "preferences" -> persistenceState.preferences();
                    case "extensionData" -> persistenceState.extensionData();
                    default -> defaultValue(method.getReturnType());
                }
        );
        return (MontoyaApi) Proxy.newProxyInstance(
                MontoyaApi.class.getClassLoader(),
                new Class<?>[]{MontoyaApi.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "userInterface" -> userInterface.proxy();
                    case "extension" -> extension;
                    case "logging" -> logging;
                    case "persistence" -> persistence;
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

    private static Project projectWithId(String projectId) {
        return new Project() {
            @Override
            public String name() {
                return "dynamic";
            }

            @Override
            public String id() {
                return projectId;
            }
        };
    }

    private static Project sequenceProjectSupplier(Project firstProject, Project secondProject) {
        return (Project) Proxy.newProxyInstance(
                Project.class.getClassLoader(),
                new Class<?>[]{Project.class},
                new java.lang.reflect.InvocationHandler() {
                    private int callCount;

                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                        Project delegate = callCount++ == 0 ? firstProject : secondProject;
                        return method.invoke(delegate, args);
                    }
                }
        );
    }

    private static Project unstableProjectWithPathSequence(String firstPath, String secondPath) {
        class UnstableProjectWithPath implements Project {
            private int callCount;

            @Override
            public String name() {
                return "disk-backed";
            }

            @Override
            public String id() {
                return "project-id";
            }

            public String path() {
                callCount++;
                return callCount == 1 ? firstPath : secondPath;
            }
        }
        return new UnstableProjectWithPath();
    }

    private static Project unstableProjectWithIdSequence(String[] projectIds, String stablePath) {
        class UnstableProjectWithId implements Project {
            private int idCallCount;

            @Override
            public String name() {
                return "disk-backed";
            }

            @Override
            public String id() {
                int index = Math.min(idCallCount, projectIds.length - 1);
                idCallCount++;
                return projectIds[index];
            }

            public String path() {
                return stablePath;
            }
        }
        return new UnstableProjectWithId();
    }

    private static ExportTarget extractResolvedTarget(SaveRequestsContextMenuProvider provider) {
        try {
            Field field = SaveRequestsContextMenuProvider.class.getDeclaredField("targetResolver");
            field.setAccessible(true);
            Supplier<?> supplier = Supplier.class.cast(field.get(provider));
            return ExportTarget.class.cast(supplier.get());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static PersistenceState persistenceState(Map<String, String> preferenceValues, Map<String, String> extensionDataValues) {
        Preferences preferences = (Preferences) Proxy.newProxyInstance(
                Preferences.class.getClassLoader(),
                new Class<?>[]{Preferences.class},
                mapBackedHandler(new HashMap<>(preferenceValues))
        );
        PersistedObject extensionData = (PersistedObject) Proxy.newProxyInstance(
                PersistedObject.class.getClassLoader(),
                new Class<?>[]{PersistedObject.class},
                mapBackedHandler(new HashMap<>(extensionDataValues))
        );
        return new PersistenceState(preferences, extensionData);
    }

    private static java.lang.reflect.InvocationHandler mapBackedHandler(Map<String, String> values) {
        return (proxy, method, args) -> switch (method.getName()) {
            case "getString" -> values.get((String) args[0]);
            case "setString" -> {
                values.put((String) args[0], (String) args[1]);
                yield null;
            }
            case "deleteString" -> {
                values.remove((String) args[0]);
                yield null;
            }
            case "stringKeys" -> new HashSet<>(values.keySet());
            default -> defaultValue(method.getReturnType());
        };
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

    private record PersistenceState(Preferences preferences, PersistedObject extensionData) {
    }

    private static final class RecordingUserInterface {
        private final java.util.concurrent.atomic.AtomicReference<ContextMenuItemsProvider> provider = new java.util.concurrent.atomic.AtomicReference<>();
        private final java.util.concurrent.atomic.AtomicReference<String> suiteTabTitle = new java.util.concurrent.atomic.AtomicReference<>();
        private final java.util.concurrent.atomic.AtomicReference<java.awt.Component> suiteTabComponent = new java.util.concurrent.atomic.AtomicReference<>();

        private UserInterface proxy() {
            return (UserInterface) Proxy.newProxyInstance(
                    UserInterface.class.getClassLoader(),
                    new Class<?>[]{UserInterface.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("registerContextMenuItemsProvider")) {
                            provider.set((ContextMenuItemsProvider) args[0]);
                            return null;
                        }
                        if (method.getName().equals("registerSuiteTab")) {
                            suiteTabTitle.set((String) args[0]);
                            suiteTabComponent.set((java.awt.Component) args[1]);
                            return Proxy.newProxyInstance(
                                    Registration.class.getClassLoader(),
                                    new Class<?>[]{Registration.class},
                                    (registrationProxy, registrationMethod, registrationArgs) -> null
                            );
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private ContextMenuItemsProvider provider() {
            return provider.get();
        }

        private String suiteTabTitle() {
            return suiteTabTitle.get();
        }

        private java.awt.Component suiteTabComponent() {
            return suiteTabComponent.get();
        }
    }
}
