package com.example.b2auco.workflow;

public record BatchSaveFailure(int selectionIndex, String finalFileName, String reason) {
}
