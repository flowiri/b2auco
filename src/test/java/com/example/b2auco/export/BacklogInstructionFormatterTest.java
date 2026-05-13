package com.example.b2auco.export;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the backlog task envelope that AUCO receives before the raw request bytes.
 */
class BacklogInstructionFormatterTest {
    private static final byte[] RAW_REQUEST = "POST /graphql HTTP/1.1\r\nHost: localhost\r\n\r\n{\"query\":\"{x}\"}"
            .getBytes(StandardCharsets.ISO_8859_1);

    private final BacklogInstructionFormatter formatter = new BacklogInstructionFormatter();

    /**
     * Custom pentester prompts should be preserved above the delimiter while raw HTTP bytes remain exact after it.
     */
    @Test
    void customInstructionsArePrependedWithoutChangingRawRequestBytes() {
        byte[] formattedBytes = formatter.format(RAW_REQUEST, "Focus on GraphQL injection and auth bypass.");
        String formattedText = new String(formattedBytes, StandardCharsets.ISO_8859_1);

        assertTrue(formattedText.startsWith("INSTRUCTIONS:\nFocus on GraphQL injection and auth bypass."));
        assertTrue(formattedText.contains("AUCO GUIDANCE:"));
        assertTrue(formattedText.contains("=============\nRAW HTTP REQUEST:\n"));
        assertArrayEquals(RAW_REQUEST, rawBytesAfterDelimiter(formattedBytes));
    }

    /**
     * Blank prompts still produce useful default guidance so backlog files are never context-free.
     */
    @Test
    void blankInstructionsUseDefaultPentestGuidance() {
        String formattedText = new String(formatter.format(RAW_REQUEST, "   "), StandardCharsets.UTF_8);

        assertTrue(formattedText.contains("No custom instructions provided."));
        assertTrue(formattedText.contains("Prioritize exploitable, reproducible findings"));
    }

    /**
     * Empty requests stay invalid because AUCO backlog tasks need a real raw request body.
     */
    @Test
    void emptyRawRequestBytesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> formatter.format(new byte[0], "Focus here"));
    }

    /**
     * Extracts only the raw request segment so the exact-byte preservation assertion is independent of header text.
     */
    private byte[] rawBytesAfterDelimiter(byte[] formattedBytes) {
        byte[] delimiter = "=============\nRAW HTTP REQUEST:\n".getBytes(StandardCharsets.UTF_8);
        for (int index = 0; index <= formattedBytes.length - delimiter.length; index++) {
            if (matchesAt(formattedBytes, delimiter, index)) {
                int rawStart = index + delimiter.length;
                return java.util.Arrays.copyOfRange(formattedBytes, rawStart, formattedBytes.length);
            }
        }
        throw new AssertionError("Missing raw request delimiter");
    }

    /**
     * Compares a delimiter candidate without decoding the formatted byte array.
     */
    private boolean matchesAt(byte[] formattedBytes, byte[] delimiter, int index) {
        for (int offset = 0; offset < delimiter.length; offset++) {
            if (formattedBytes[index + offset] != delimiter[offset]) {
                return false;
            }
        }
        return true;
    }
}
