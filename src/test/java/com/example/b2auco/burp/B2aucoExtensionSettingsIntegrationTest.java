package com.example.b2auco.burp;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import com.example.b2auco.model.ExportFileName;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;
import com.example.b2auco.workflow.BackgroundBatchDispatcher;
import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class B2aucoExtensionSettingsIntegrationTest {
    @Test
    void settingsEditChangesNextSaveTargetWithoutRebuildingProvider() {
        AtomicReference<ExportTarget> currentTarget = new AtomicReference<>(new ExportTarget(Path.of("build", "tmp", "global-folder")));
        List<PreparedExport> dispatchedExports = new ArrayList<>();
        BackgroundBatchDispatcher dispatcher = dispatcher(dispatchedExports);
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                currentTarget::get,
                (requestResponse, target) -> preparedExport("phase4-target.txt", target),
                dispatcher
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(List.of(httpRequestResponse()), Optional.empty()));
        JMenu submenu = assertInstanceOf(JMenu.class, menuItems.getFirst());
        JMenuItem saveRequestsItem = assertInstanceOf(JMenuItem.class, submenu.getItem(0));

        saveRequestsItem.doClick();
        currentTarget.set(new ExportTarget(Path.of("build", "tmp", "project-override-folder")));
        saveRequestsItem.doClick();

        assertEquals(List.of(
                Path.of("build", "tmp", "global-folder"),
                Path.of("build", "tmp", "project-override-folder")
        ), dispatchedExports.stream().map(export -> export.target().outputDirectory()).toList());
    }

    private static PreparedExport preparedExport(String fileName, ExportTarget target) {
        String baseStem = fileName.substring(0, fileName.length() - 4);
        return new PreparedExport(target, new ExportFileName(baseStem, fileName), new byte[]{'G', 'E', 'T'});
    }

    private static BackgroundBatchDispatcher dispatcher(List<PreparedExport> dispatchedExports) {
        return new BackgroundBatchDispatcher(
                new AbstractExecutorService() {
                    @Override
                    public void shutdown() {
                    }

                    @Override
                    public List<Runnable> shutdownNow() {
                        return List.of();
                    }

                    @Override
                    public boolean isShutdown() {
                        return false;
                    }

                    @Override
                    public boolean isTerminated() {
                        return false;
                    }

                    @Override
                    public boolean awaitTermination(long timeout, TimeUnit unit) {
                        return true;
                    }

                    @Override
                    public void execute(Runnable command) {
                        command.run();
                    }
                },
                new com.example.b2auco.workflow.SaveRequestsBatchRunner(preparedExport -> {
                    dispatchedExports.add(preparedExport);
                    return preparedExport.target().outputDirectory().resolve(preparedExport.fileName().finalFileName());
                }),
                new com.example.b2auco.logging.BatchResultFormatter(),
                (burp.api.montoya.logging.Logging) Proxy.newProxyInstance(
                        burp.api.montoya.logging.Logging.class.getClassLoader(),
                        new Class<?>[]{burp.api.montoya.logging.Logging.class},
                        (proxy, method, args) -> null
                )
        );
    }

    private static ContextMenuEvent contextMenuEvent(List<HttpRequestResponse> selectedRequestResponses, Optional<?> ignored) {
        return (ContextMenuEvent) Proxy.newProxyInstance(
                ContextMenuEvent.class.getClassLoader(),
                new Class<?>[]{ContextMenuEvent.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectedRequestResponses" -> selectedRequestResponses;
                    case "messageEditorRequestResponse" -> Optional.empty();
                    case "selectedIssues" -> List.of();
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static HttpRequestResponse httpRequestResponse() {
        return (HttpRequestResponse) Proxy.newProxyInstance(
                HttpRequestResponse.class.getClassLoader(),
                new Class<?>[]{HttpRequestResponse.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
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
}
