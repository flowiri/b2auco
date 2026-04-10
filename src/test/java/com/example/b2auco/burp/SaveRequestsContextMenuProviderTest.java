package com.example.b2auco.burp;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveRequestsContextMenuProviderTest {
    @Test
    void returnsEmptyListWhenNoRequestSourceIsAvailable() {
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                new ExportTarget(Path.of("build", "tmp", "menu-tests")),
                (requestResponse, target) -> preparedExport("unused.txt"),
                recordingBackgroundBatchDispatcher().dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(Collections.emptyList(), Optional.empty()));

        assertEquals(Collections.emptyList(), menuItems);
    }

    @Test
    void exposesB2aucoSubmenuWithSaveRequestsItemForNonEmptySelection() {
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                new ExportTarget(Path.of("build", "tmp", "menu-tests")),
                (requestResponse, target) -> preparedExport("example.com-api-users.txt"),
                recordingBackgroundBatchDispatcher().dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(List.of(httpRequestResponse("selected")), Optional.empty()));

        assertSaveRequestsSubmenu(menuItems);
    }

    @Test
    void exposesB2aucoSubmenuWithSaveRequestsItemForMessageEditorRequestResponse() {
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                new ExportTarget(Path.of("build", "tmp", "menu-tests")),
                (requestResponse, target) -> preparedExport("editor-request.txt"),
                recordingBackgroundBatchDispatcher().dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(Collections.emptyList(), Optional.of(messageEditorRequestResponse(httpRequestResponse("editor")))));

        assertSaveRequestsSubmenu(menuItems);
    }

    @Test
    void dispatchesSinglePreparedExportFromMessageEditorRequestResponse() {
        RecordingDispatcher recordingDispatcher = recordingBackgroundBatchDispatcher();
        HttpRequestResponse editorRequestResponse = httpRequestResponse("editor");
        PreparedExport preparedExport = preparedExport("editor-request.txt");
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                new ExportTarget(Path.of("build", "tmp", "menu-tests")),
                (requestResponse, target) -> {
                    assertSame(editorRequestResponse, requestResponse);
                    return preparedExport;
                },
                recordingDispatcher.dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(Collections.emptyList(), Optional.of(messageEditorRequestResponse(editorRequestResponse))));

        JMenu submenu = assertInstanceOf(JMenu.class, menuItems.getFirst());
        JMenuItem saveRequestsItem = assertInstanceOf(JMenuItem.class, submenu.getItem(0));
        saveRequestsItem.doClick();

        assertEquals(List.of(preparedExport), recordingDispatcher.dispatchedExports());
    }

    private static void assertSaveRequestsSubmenu(List<Component> menuItems) {
        assertEquals(1, menuItems.size());
        JMenu submenu = assertInstanceOf(JMenu.class, menuItems.getFirst());
        assertEquals("b2auco", submenu.getText());
        assertEquals(1, submenu.getItemCount());
        JMenuItem saveRequestsItem = assertInstanceOf(JMenuItem.class, submenu.getItem(0));
        assertEquals("Save requests", saveRequestsItem.getText());
        assertTrue(saveRequestsItem.getActionListeners().length > 0);
    }

    private static ContextMenuEvent contextMenuEvent(
            List<HttpRequestResponse> selectedRequestResponses,
            Optional<MessageEditorHttpRequestResponse> messageEditorRequestResponse
    ) {
        return (ContextMenuEvent) Proxy.newProxyInstance(
                ContextMenuEvent.class.getClassLoader(),
                new Class<?>[]{ContextMenuEvent.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectedRequestResponses" -> selectedRequestResponses;
                    case "messageEditorRequestResponse" -> messageEditorRequestResponse;
                    case "selectedIssues" -> List.of();
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static MessageEditorHttpRequestResponse messageEditorRequestResponse(HttpRequestResponse requestResponse) {
        return (MessageEditorHttpRequestResponse) Proxy.newProxyInstance(
                MessageEditorHttpRequestResponse.class.getClassLoader(),
                new Class<?>[]{MessageEditorHttpRequestResponse.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("requestResponse")) {
                        return requestResponse;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static HttpRequestResponse httpRequestResponse(String id) {
        return (HttpRequestResponse) Proxy.newProxyInstance(
                HttpRequestResponse.class.getClassLoader(),
                new Class<?>[]{HttpRequestResponse.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) {
                        return "HttpRequestResponse[" + id + "]";
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static PreparedExport preparedExport(String fileName) {
        String baseStem = fileName.substring(0, fileName.length() - 4);
        return new PreparedExport(
                new ExportTarget(Path.of("build", "tmp", "menu-tests")),
                new ExportFileName(baseStem, fileName),
                new byte[]{'G', 'E', 'T'}
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

    private static RecordingDispatcher recordingBackgroundBatchDispatcher() {
        List<PreparedExport> dispatchedExports = new ArrayList<>();
        BackgroundBatchDispatcher dispatcher = new BackgroundBatchDispatcher(
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
                loggingProxy()
        );
        return new RecordingDispatcher(dispatcher, List.copyOf(dispatchedExports), dispatchedExports);
    }

    private static burp.api.montoya.logging.Logging loggingProxy() {
        return (burp.api.montoya.logging.Logging) Proxy.newProxyInstance(
                burp.api.montoya.logging.Logging.class.getClassLoader(),
                new Class<?>[]{burp.api.montoya.logging.Logging.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private record RecordingDispatcher(
            BackgroundBatchDispatcher dispatcher,
            List<PreparedExport> snapshot,
            List<PreparedExport> mutableDispatchedExports
    ) {
        private List<PreparedExport> dispatchedExports() {
            return List.copyOf(mutableDispatchedExports);
        }
    }
}
