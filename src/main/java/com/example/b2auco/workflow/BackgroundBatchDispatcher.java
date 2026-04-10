package com.example.b2auco.workflow;

import burp.api.montoya.logging.Logging;
import com.example.b2auco.logging.BatchResultFormatter;
import com.example.b2auco.model.PreparedExport;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public final class BackgroundBatchDispatcher {
    private final ExecutorService executorService;
    private final SaveRequestsBatchRunner runner;
    private final BatchResultFormatter formatter;
    private final Logging logging;

    public BackgroundBatchDispatcher(
            ExecutorService executorService,
            SaveRequestsBatchRunner runner,
            BatchResultFormatter formatter,
            Logging logging
    ) {
        this.executorService = Objects.requireNonNull(executorService, "executorService");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.logging = Objects.requireNonNull(logging, "logging");
    }

    public void dispatch(List<PreparedExport> preparedExports) {
        List<PreparedExport> exports = List.copyOf(Objects.requireNonNull(preparedExports, "preparedExports"));
        executorService.submit(() -> {
            try {
                BatchSaveResult result = runner.runBatch(exports);
                logging.logToOutput(formatter.format(result));
            } catch (Throwable throwable) {
                logging.logToError("b2auco batch crashed", throwable);
            }
        });
    }
}
