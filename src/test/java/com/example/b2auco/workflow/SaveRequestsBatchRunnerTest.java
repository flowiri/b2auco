package com.example.b2auco.workflow;

import com.example.b2auco.model.ExportFileName;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.model.PreparedExport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveRequestsBatchRunnerTest {
    private final List<PreparedExport> preparedExports = List.of(
            preparedExport("example.com-first.txt"),
            preparedExport("example.com-second.txt"),
            preparedExport("example.com-third.txt")
    );

    @Test
    void batchRunsPersisterForEveryPreparedExportInOriginalSelectionOrder() throws IOException {
        RecordingPersister persister = new RecordingPersister();
        SaveRequestsBatchRunner runner = new SaveRequestsBatchRunner(persister);

        runner.runBatch(preparedExports);

        assertEquals(List.of(
                "example.com-first.txt",
                "example.com-second.txt",
                "example.com-third.txt"
        ), persister.recordedFileNames());
    }

    @Test
    void batchContinuesAfterIOExceptionAndReturnsAccurateCounts() {
        RecordingPersister persister = new RecordingPersister(2);
        SaveRequestsBatchRunner runner = new SaveRequestsBatchRunner(persister);

        BatchSaveResult result = runner.runBatch(preparedExports);

        assertEquals(List.of(
                "example.com-first.txt",
                "example.com-second.txt",
                "example.com-third.txt"
        ), persister.recordedFileNames());
        assertEquals(3, result.selectedCount());
        assertEquals(2, result.savedCount());
        assertEquals(1, result.failedCount());
        assertEquals(List.of(new BatchSaveFailure(2, "example.com-second.txt", "disk full")), result.failures());
    }

    private PreparedExport preparedExport(String finalFileName) {
        String baseStem = finalFileName.substring(0, finalFileName.length() - 4);
        return new PreparedExport(
                new ExportTarget(Path.of("build", "tmp", "batch-tests")),
                new ExportFileName(baseStem, finalFileName),
                new byte[]{'G', 'E', 'T'}
        );
    }

    private static final class RecordingPersister implements PreparedExportPersister {
        private final List<String> recordedFileNames = new ArrayList<>();
        private final int failureSelectionIndex;
        private int invocationCount;

        private RecordingPersister() {
            this(-1);
        }

        private RecordingPersister(int failureSelectionIndex) {
            this.failureSelectionIndex = failureSelectionIndex;
        }

        @Override
        public Path write(PreparedExport preparedExport) throws IOException {
            invocationCount++;
            recordedFileNames.add(preparedExport.fileName().finalFileName());
            if (invocationCount == failureSelectionIndex) {
                throw new IOException("disk full");
            }
            return preparedExport.target().outputDirectory().resolve(preparedExport.fileName().finalFileName());
        }

        private List<String> recordedFileNames() {
            return recordedFileNames;
        }
    }
}
