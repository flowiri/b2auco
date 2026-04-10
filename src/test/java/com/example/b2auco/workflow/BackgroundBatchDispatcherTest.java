package com.example.b2auco.workflow;

import burp.api.montoya.logging.Logging;
import com.example.b2auco.model.ExportFileName;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;
import com.example.b2auco.logging.BatchResultFormatter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundBatchDispatcherTest {
    @Test
    void dispatchSubmitsBatchWithoutRunningItInlineAndPreservesSelectionOrder() {
        RecordingExecutorService executorService = new RecordingExecutorService();
        RecordingPersister persister = new RecordingPersister();
        SaveRequestsBatchRunner runner = new SaveRequestsBatchRunner(persister);
        RecordingLogging logging = new RecordingLogging();
        BackgroundBatchDispatcher dispatcher = new BackgroundBatchDispatcher(
                executorService,
                runner,
                new BatchResultFormatter(),
                logging.proxy()
        );
        List<PreparedExport> preparedExports = List.of(
                preparedExport("example.com-first.txt"),
                preparedExport("example.com-second.txt"),
                preparedExport("example.com-third.txt")
        );

        dispatcher.dispatch(preparedExports);

        assertTrue(executorService.submitted);
        assertFalse(persister.wasCalled);
        assertNull(logging.outputMessage);

        executorService.runNext();

        assertTrue(persister.wasCalled);
        assertEquals(List.of(
                "example.com-first.txt",
                "example.com-second.txt",
                "example.com-third.txt"
        ), persister.recordedFileNames);
    }

    @Test
    void dispatchLogsFormatterOutputOnCompletion() {
        RecordingExecutorService executorService = new RecordingExecutorService();
        SaveRequestsBatchRunner runner = new SaveRequestsBatchRunner(new RecordingPersister());
        RecordingLogging logging = new RecordingLogging();
        BackgroundBatchDispatcher dispatcher = new BackgroundBatchDispatcher(
                executorService,
                runner,
                new BatchResultFormatter(),
                logging.proxy()
        );

        dispatcher.dispatch(List.of(preparedExport("example.com-api-users.txt")));
        executorService.runNext();

        assertTrue(logging.outputMessage.startsWith("b2auco save"));
        assertEquals("b2auco save complete: selected=1 saved=1 failed=0", logging.outputMessage);
    }

    private static PreparedExport preparedExport(String finalFileName) {
        String baseStem = finalFileName.substring(0, finalFileName.length() - 4);
        return new PreparedExport(
                new ExportTarget(Path.of("build", "tmp", "dispatcher-tests")),
                new ExportFileName(baseStem, finalFileName),
                new byte[]{'G', 'E', 'T'}
        );
    }

    private static final class RecordingPersister implements PreparedExportPersister {
        private final List<String> recordedFileNames = new ArrayList<>();
        private boolean wasCalled;

        @Override
        public Path write(PreparedExport preparedExport) {
            wasCalled = true;
            recordedFileNames.add(preparedExport.fileName().finalFileName());
            return preparedExport.target().outputDirectory().resolve(preparedExport.fileName().finalFileName());
        }
    }

    private static final class RecordingExecutorService extends AbstractExecutorService {
        private final List<Runnable> tasks = new ArrayList<>();
        private boolean submitted;
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown && tasks.isEmpty();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isTerminated();
        }

        @Override
        public void execute(Runnable command) {
            submitted = true;
            tasks.add(command);
        }

        @Override
        public Future<?> submit(Runnable task) {
            submitted = true;
            FutureTask<Void> futureTask = new FutureTask<>(task, null);
            tasks.add(futureTask);
            return futureTask;
        }

        private void runNext() {
            tasks.removeFirst().run();
        }
    }

    private static final class RecordingLogging {
        private String outputMessage;

        private Logging proxy() {
            return (Logging) Proxy.newProxyInstance(
                    Logging.class.getClassLoader(),
                    new Class<?>[]{Logging.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("logToOutput")) {
                            outputMessage = String.valueOf(args[0]);
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
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
}
