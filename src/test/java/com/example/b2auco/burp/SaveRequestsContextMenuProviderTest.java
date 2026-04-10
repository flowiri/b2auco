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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveRequestsContextMenuProviderTest {
    @Test
    void returnsEmptyListWhenSelectedRequestResponsesIsEmpty() {
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                new ExportTarget(Path.of("build", "tmp", "menu-tests")),
                (requestResponse, target) -> preparedExport("unused.txt"),
                new NoOpBackgroundBatchDispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(Collections.emptyList()));

        assertEquals(Collections.emptyList(), menuItems);
    }

    @Test
    void exposesB2aucoSubmenuWithSaveRequestsItemForNonEmptySelection() {
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                new ExportTarget(Path.of("build", "tmp", "menu-tests")),
                (requestResponse, target) -> preparedExport("example.com-api-users.txt"),
                new NoOpBackgroundBatchDispatcher()
        );

        List<Component> menuItems = provider.provideMenuItems(contextMenuEvent(List.of(httpRequestResponse())));

        assertEquals(1, menuItems.size());
        JMenu submenu = assertInstanceOf(JMenu.class, menuItems.getFirst());
        assertEquals("b2auco", submenu.getText());
        assertEquals(1, submenu.getItemCount());
        JMenuItem saveRequestsItem = assertInstanceOf(JMenuItem.class, submenu.getItem(0));
        assertEquals("Save requests", saveRequestsItem.getText());
        assertTrue(saveRequestsItem.getActionListeners().length > 0);
    }

    private static ContextMenuEvent contextMenuEvent(List<HttpRequestResponse> selectedRequestResponses) {
        return (ContextMenuEvent) Proxy.newProxyInstance(
                ContextMenuEvent.class.getClassLoader(),
                new Class<?>[]{ContextMenuEvent.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectedRequestResponses" -> selectedRequestResponses;
                    case "messageEditorRequestResponse" -> java.util.Optional.empty();
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

    private static final class NoOpBackgroundBatchDispatcher extends BackgroundBatchDispatcher {
        private NoOpBackgroundBatchDispatcher() {
            super(null, null, null, null);
        }
    }
}
