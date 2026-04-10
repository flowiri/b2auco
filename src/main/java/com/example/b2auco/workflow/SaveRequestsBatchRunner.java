package com.example.b2auco.workflow;

import com.example.b2auco.model.PreparedExport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SaveRequestsBatchRunner {
    private final PreparedExportPersister persister;

    public SaveRequestsBatchRunner(PreparedExportPersister persister) {
        this.persister = Objects.requireNonNull(persister, "persister");
    }

    public BatchSaveResult runBatch(List<PreparedExport> preparedExports) {
        List<PreparedExport> exports = Objects.requireNonNull(preparedExports, "preparedExports");
        List<BatchSaveFailure> failures = new ArrayList<>();
        int savedCount = 0;

        for (int index = 0; index < exports.size(); index++) {
            PreparedExport preparedExport = exports.get(index);
            try {
                persister.write(preparedExport);
                savedCount++;
            } catch (IOException exception) {
                failures.add(new BatchSaveFailure(
                        index + 1,
                        preparedExport.fileName().finalFileName(),
                        failureReason(exception)
                ));
            }
        }

        return new BatchSaveResult(exports.size(), savedCount, failures.size(), List.copyOf(failures));
    }

    private String failureReason(IOException exception) {
        String message = exception.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return exception.getClass().getSimpleName();
    }
}
