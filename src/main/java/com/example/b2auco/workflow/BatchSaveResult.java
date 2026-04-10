package com.example.b2auco.workflow;

import java.util.List;

public record BatchSaveResult(int selectedCount, int savedCount, int failedCount, List<BatchSaveFailure> failures) {
}
