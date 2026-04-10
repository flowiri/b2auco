package com.example.b2auco.logging;

import com.example.b2auco.workflow.BatchSaveFailure;
import com.example.b2auco.workflow.BatchSaveResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchResultFormatterTest {
    private final BatchResultFormatter formatter = new BatchResultFormatter();

    @Test
    void fullSuccessFormatsAsSingleOutputLine() {
        BatchSaveResult result = new BatchSaveResult(3, 3, 0, List.of());

        assertEquals(
                "b2auco save complete: selected=3 saved=3 failed=0",
                formatter.format(result)
        );
    }

    @Test
    void partialSuccessFormatsSummaryAndFailureDetails() {
        BatchSaveResult result = new BatchSaveResult(
                3,
                2,
                1,
                List.of(new BatchSaveFailure(2, "example.com-second.txt", "disk full"))
        );

        assertEquals(
                "b2auco save partial: selected=3 saved=2 failed=1\n- #2 example.com-second.txt -> disk full",
                formatter.format(result)
        );
    }

    @Test
    void fullFailureFormatsFailureHeaderAndAllFailureLines() {
        BatchSaveResult result = new BatchSaveResult(
                2,
                0,
                2,
                List.of(
                        new BatchSaveFailure(1, "example.com-first.txt", "permission denied"),
                        new BatchSaveFailure(2, "example.com-second.txt", "disk full")
                )
        );

        assertEquals(
                "b2auco save failed: selected=2 saved=0 failed=2\n"
                        + "- #1 example.com-first.txt -> permission denied\n"
                        + "- #2 example.com-second.txt -> disk full",
                formatter.format(result)
        );
    }
}
