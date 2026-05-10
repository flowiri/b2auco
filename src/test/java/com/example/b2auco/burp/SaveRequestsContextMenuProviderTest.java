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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveRequestsContextMenuProviderTest {
    // Verifies no menu is exposed when Burp provides neither selected messages nor a message-editor request.
    @Test
    void returnsEmptyListWhenNoRequestSourceIsAvailable() {
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                targetSupplier(new ExportTarget(Path.of("build", "tmp", "menu-tests"))),
                targetSupplier(new ExportTarget(Path.of("build", "tmp", "menu-tests"))),
                (requestResponse, target) -> preparedExport("unused.txt", target),
                recordingBackgroundBatchDispatcher().dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(Collections.emptyList(), Optional.empty()));

        assertEquals(Collections.emptyList(), menuItems);
    }

    // Verifies the selection context shows the b2auco submenu with separate Auth and Backlog actions.
    @Test
    void exposesB2aucoSubmenuWithSplitTargetItemsForNonEmptySelection() {
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                targetSupplier(new ExportTarget(Path.of("build", "tmp", "menu-tests"))),
                targetSupplier(new ExportTarget(Path.of("build", "tmp", "menu-tests"))),
                (requestResponse, target) -> preparedExport("example.com-api-users.txt", target),
                recordingBackgroundBatchDispatcher().dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(List.of(httpRequestResponse("selected")), Optional.empty()));

        assertSplitTargetSubmenu(menuItems);
    }

    // Verifies the message-editor context uses the same split submenu when no selection exists.
    @Test
    void exposesB2aucoSubmenuWithSplitTargetItemsForMessageEditorRequestResponse() {
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                targetSupplier(new ExportTarget(Path.of("build", "tmp", "menu-tests"))),
                targetSupplier(new ExportTarget(Path.of("build", "tmp", "menu-tests"))),
                (requestResponse, target) -> preparedExport("editor-request.txt", target),
                recordingBackgroundBatchDispatcher().dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(Collections.emptyList(), Optional.of(messageEditorRequestResponse(httpRequestResponse("editor")))));

        assertSplitTargetSubmenu(menuItems);
    }

    // Verifies Send to Auth resolves its target lazily at click time and dispatches with that resolved target.
    @Test
    void resolvesAuthExportTargetWhenSendToAuthIsClickedNotWhenProviderIsConstructed() {
        RecordingDispatcher recordingDispatcher = recordingBackgroundBatchDispatcher();
        HttpRequestResponse editorRequestResponse = httpRequestResponse("editor");
        ExportTarget initialTarget = new ExportTarget(Path.of("build", "tmp", "initial-target"));
        ExportTarget updatedTarget = new ExportTarget(Path.of("build", "tmp", "updated-target"));
        AtomicReference<ExportTarget> currentAuthTarget = new AtomicReference<>(initialTarget);
        AtomicReference<ExportTarget> mappedTarget = new AtomicReference<>();
        AtomicInteger resolveCalls = new AtomicInteger();
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                () -> {
                    resolveCalls.incrementAndGet();
                    return currentAuthTarget.get();
                },
                targetSupplier(new ExportTarget(Path.of("build", "tmp", "backlog-target"))),
                (requestResponse, target) -> {
                    assertSame(editorRequestResponse, requestResponse);
                    mappedTarget.set(target);
                    return preparedExport("editor-request.txt", target);
                },
                recordingDispatcher.dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(Collections.emptyList(), Optional.of(messageEditorRequestResponse(editorRequestResponse))));
        currentAuthTarget.set(updatedTarget);

        JMenu submenu = assertInstanceOf(JMenu.class, menuItems.getFirst());
        JMenuItem sendToAuthItem = assertInstanceOf(JMenuItem.class, submenu.getItem(0));
        sendToAuthItem.doClick();

        assertEquals(1, resolveCalls.get());
        assertSame(updatedTarget, mappedTarget.get());
        assertEquals(updatedTarget.outputDirectory(), recordingDispatcher.dispatchedExports().getFirst().target().outputDirectory());
    }

    // Verifies repeated Send to Backlog clicks use the latest folder without rebuilding the provider.
    @Test
    void secondBacklogClickUsesUpdatedFolderWithoutRebuildingProvider() {
        RecordingDispatcher recordingDispatcher = recordingBackgroundBatchDispatcher();
        HttpRequestResponse selectedRequest = httpRequestResponse("selected");
        ExportTarget firstTarget = new ExportTarget(Path.of("build", "tmp", "first-target"));
        ExportTarget secondTarget = new ExportTarget(Path.of("build", "tmp", "second-target"));
        AtomicReference<ExportTarget> currentBacklogTarget = new AtomicReference<>(firstTarget);
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                targetSupplier(new ExportTarget(Path.of("build", "tmp", "auth-target"))),
                currentBacklogTarget::get,
                (requestResponse, target) -> preparedExport("dynamic-target.txt", target),
                recordingDispatcher.dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(List.of(selectedRequest), Optional.empty()));
        JMenu submenu = assertInstanceOf(JMenu.class, menuItems.getFirst());
        JMenuItem sendToBacklogItem = assertInstanceOf(JMenuItem.class, submenu.getItem(1));

        sendToBacklogItem.doClick();
        currentBacklogTarget.set(secondTarget);
        sendToBacklogItem.doClick();

        assertEquals(List.of(firstTarget.outputDirectory(), secondTarget.outputDirectory()),
                recordingDispatcher.dispatchedExports().stream().map(export -> export.target().outputDirectory()).toList());
    }

    // Verifies the two submenu actions dispatch prepared exports to independent Auth and Backlog targets.
    @Test
    void dispatchesAuthAndBacklogActionsToTheirOwnTargets() {
        RecordingDispatcher recordingDispatcher = recordingBackgroundBatchDispatcher();
        HttpRequestResponse editorRequestResponse = httpRequestResponse("editor");
        ExportTarget authTarget = new ExportTarget(Path.of("build", "tmp", "auth-target"));
        ExportTarget backlogTarget = new ExportTarget(Path.of("build", "tmp", "backlog-target"));
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                targetSupplier(authTarget),
                targetSupplier(backlogTarget),
                (requestResponse, target) -> {
                    assertSame(editorRequestResponse, requestResponse);
                    return preparedExport(target.outputDirectory().getFileName() + ".txt", target);
                },
                recordingDispatcher.dispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(Collections.emptyList(), Optional.of(messageEditorRequestResponse(editorRequestResponse))));

        JMenu submenu = assertInstanceOf(JMenu.class, menuItems.getFirst());
        JMenuItem sendToAuthItem = assertInstanceOf(JMenuItem.class, submenu.getItem(0));
        JMenuItem sendToBacklogItem = assertInstanceOf(JMenuItem.class, submenu.getItem(1));
        sendToAuthItem.doClick();
        sendToBacklogItem.doClick();

        assertEquals(List.of(authTarget.outputDirectory(), backlogTarget.outputDirectory()),
                recordingDispatcher.dispatchedExports().stream().map(export -> export.target().outputDirectory()).toList());
    }

    // Verifies submenu structure, item labels, ordering, and listener wiring for the split context menu.
    private static void assertSplitTargetSubmenu(List<Component> menuItems) {
        assertEquals(1, menuItems.size());
        JMenu submenu = assertInstanceOf(JMenu.class, menuItems.getFirst());
        assertEquals("b2auco", submenu.getText());
        assertEquals(2, submenu.getItemCount());
        JMenuItem sendToAuthItem = assertInstanceOf(JMenuItem.class, submenu.getItem(0));
        assertEquals("Send to Auth", sendToAuthItem.getText());
        assertTrue(sendToAuthItem.getActionListeners().length > 0);
        JMenuItem sendToBacklogItem = assertInstanceOf(JMenuItem.class, submenu.getItem(1));
        assertEquals("Send to Backlog", sendToBacklogItem.getText());
        assertTrue(sendToBacklogItem.getActionListeners().length > 0);
    }

    private static Supplier<ExportTarget> targetSupplier(ExportTarget target) {
        return () -> target;
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

    private static PreparedExport preparedExport(String fileName, ExportTarget exportTarget) {
        String baseStem = fileName.substring(0, fileName.length() - 4);
        return new PreparedExport(
                exportTarget,
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
        return new RecordingDispatcher(dispatcher, dispatchedExports);
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
            List<PreparedExport> mutableDispatchedExports
    ) {
        private List<PreparedExport> dispatchedExports() {
            return List.copyOf(mutableDispatchedExports);
        }
    }
}
