package com.example.b2auco.burp;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;
import com.example.b2auco.workflow.BackgroundBatchDispatcher;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class SaveRequestsContextMenuProvider implements ContextMenuItemsProvider {
    private final Supplier<ExportTarget> authTargetResolver;
    private final Supplier<ExportTarget> backlogTargetResolver;
    private final BiFunction<HttpRequestResponse, ExportTarget, PreparedExport> mapper;
    private final BackgroundBatchDispatcher dispatcher;

    /**
     * Keeps existing integrations source-compatible until the extension wiring is updated to provide
     * separate Auth and Backlog target resolvers. Both actions intentionally resolve through the same
     * supplier in this compatibility path, preserving lazy folder lookup behavior for old callers.
     */
    public SaveRequestsContextMenuProvider(
            Supplier<ExportTarget> targetResolver,
            BiFunction<HttpRequestResponse, ExportTarget, PreparedExport> mapper,
            BackgroundBatchDispatcher dispatcher
    ) {
        this(targetResolver, targetResolver, mapper, dispatcher);
    }

    /**
     * Accepts independent lazy target resolvers so each context-menu action can pick up the latest
     * configured Auth or Backlog folder at click time, without rebuilding the provider instance.
     */
    public SaveRequestsContextMenuProvider(
            Supplier<ExportTarget> authTargetResolver,
            Supplier<ExportTarget> backlogTargetResolver,
            BiFunction<HttpRequestResponse, ExportTarget, PreparedExport> mapper,
            BackgroundBatchDispatcher dispatcher
    ) {
        this.authTargetResolver = Objects.requireNonNull(authTargetResolver, "authTargetResolver");
        this.backlogTargetResolver = Objects.requireNonNull(backlogTargetResolver, "backlogTargetResolver");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    /**
     * Builds the b2auco submenu only when Burp provides selected request-responses or a message-editor
     * request-response. The returned action listeners close over the request list, but defer folder
     * resolution until the user chooses Auth or Backlog.
     */
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<HttpRequestResponse> requestResponses = requestResponsesFor(event);
        if (requestResponses.isEmpty()) {
            return Collections.emptyList();
        }

        JMenu b2aucoMenu = new JMenu("b2auco");
        b2aucoMenu.add(menuItem("Send to Auth", authTargetResolver, requestResponses));
        b2aucoMenu.add(menuItem("Send to Backlog", backlogTargetResolver, requestResponses));
        return List.of(b2aucoMenu);
    }

    /**
     * Creates one target-specific dispatch action. The target resolver is invoked inside the click
     * handler so repeated clicks can observe settings changes made after the menu provider was created.
     */
    private JMenuItem menuItem(
            String text,
            Supplier<ExportTarget> targetResolver,
            List<HttpRequestResponse> requestResponses
    ) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(ignored -> {
            ExportTarget currentTarget = targetResolver.get();
            List<PreparedExport> preparedExports = requestResponses.stream()
                    .map(requestResponse -> mapper.apply(requestResponse, currentTarget))
                    .toList();
            dispatcher.dispatch(preparedExports);
        });
        return item;
    }

    /**
     * Normalizes Burp's two request sources into one list so both Auth and Backlog actions use the
     * same selection logic for table selections and message-editor context menus.
     */
    private List<HttpRequestResponse> requestResponsesFor(ContextMenuEvent event) {
        List<HttpRequestResponse> selectedRequestResponses = List.copyOf(event.selectedRequestResponses());
        if (!selectedRequestResponses.isEmpty()) {
            return selectedRequestResponses;
        }
        return event.messageEditorRequestResponse()
                .map(messageEditorRequestResponse -> List.of(messageEditorRequestResponse.requestResponse()))
                .orElseGet(Collections::emptyList);
    }
}
