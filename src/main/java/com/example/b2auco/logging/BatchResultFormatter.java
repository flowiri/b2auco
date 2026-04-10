package com.example.b2auco.logging;

import com.example.b2auco.workflow.BatchSaveFailure;
import com.example.b2auco.workflow.BatchSaveResult;

public final class BatchResultFormatter {
    public String format(BatchSaveResult result) {
        String header = header(result);
        if (result.failedCount() == 0) {
            return header;
        }

        StringBuilder formatted = new StringBuilder(header);
        for (BatchSaveFailure failure : result.failures()) {
            formatted.append('\n')
                    .append("- #")
                    .append(failure.selectionIndex())
                    .append(' ')
                    .append(failure.finalFileName())
                    .append(" -> ")
                    .append(failure.reason());
        }
        return formatted.toString();
    }

    private String header(BatchSaveResult result) {
        if (result.failedCount() == 0) {
            return "b2auco save complete: selected=%d saved=%d failed=%d"
                    .formatted(result.selectedCount(), result.savedCount(), result.failedCount());
        }
        if (result.savedCount() == 0) {
            return "b2auco save failed: selected=%d saved=%d failed=%d"
                    .formatted(result.selectedCount(), result.savedCount(), result.failedCount());
        }
        return "b2auco save partial: selected=%d saved=%d failed=%d"
                .formatted(result.selectedCount(), result.savedCount(), result.failedCount());
    }
}
