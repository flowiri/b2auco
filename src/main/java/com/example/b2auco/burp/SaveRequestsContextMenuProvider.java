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

public final class SaveRequestsContextMenuProvider implements ContextMenuItemsProvider {
    private final ExportTarget target;
    private final BiFunction<HttpRequestResponse, ExportTarget, PreparedExport> mapper;
    private final BackgroundBatchDispatcher dispatcher;

    public SaveRequestsContextMenuProvider(
            ExportTarget target,
            BiFunction<HttpRequestResponse, ExportTarget, PreparedExport> mapper,
            BackgroundBatchDispatcher dispatcher
    ) {
        this.target = Objects.requireNonNull(target, "target");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<HttpRequestResponse> requestResponses = requestResponsesFor(event);
        if (requestResponses.isEmpty()) {
            return Collections.emptyList();
        }

        JMenu b2aucoMenu = new JMenu("b2auco");
        JMenuItem saveRequestsItem = new JMenuItem("Save requests");
        saveRequestsItem.addActionListener(ignored -> {
            List<PreparedExport> preparedExports = requestResponses.stream()
                    .map(requestResponse -> mapper.apply(requestResponse, target))
                    .toList();
            dispatcher.dispatch(preparedExports);
        });
        b2aucoMenu.add(saveRequestsItem);
        return List.of(b2aucoMenu);
    }

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
