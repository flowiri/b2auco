package com.example.b2auco.settings;

import java.util.Objects;

public record FolderSaveResult(
        FolderSettingsController.Scope scope,
        boolean success,
        String message,
        FolderSettingsViewState viewState
) {
    public FolderSaveResult {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(viewState, "viewState");
    }
}
