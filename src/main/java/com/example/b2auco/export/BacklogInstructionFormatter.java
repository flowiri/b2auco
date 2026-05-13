package com.example.b2auco.export;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Prefixes backlog exports with pentester guidance while preserving the raw HTTP request bytes exactly.
 */
public final class BacklogInstructionFormatter {
    private static final String DEFAULT_INSTRUCTIONS = """
            No custom instructions provided. Perform a focused security review of this exact HTTP request.
            Prioritize exploitable, reproducible findings and include concise validation steps.
            """.trim();

    /**
     * Builds the saved backlog task format as UTF-8 guidance bytes followed by the unchanged raw request bytes.
     */
    public byte[] format(byte[] rawRequestBytes, String customInstructions) {
        byte[] requestBytes = Arrays.copyOf(Objects.requireNonNull(rawRequestBytes, "rawRequestBytes"), rawRequestBytes.length);
        if (requestBytes.length == 0) {
            throw new IllegalArgumentException("rawRequestBytes must not be empty");
        }

        byte[] guidanceBytes = guidanceBlock(customInstructions).getBytes(StandardCharsets.UTF_8);
        byte[] formattedBytes = new byte[guidanceBytes.length + requestBytes.length];
        System.arraycopy(guidanceBytes, 0, formattedBytes, 0, guidanceBytes.length);
        System.arraycopy(requestBytes, 0, formattedBytes, guidanceBytes.length, requestBytes.length);
        return formattedBytes;
    }

    /**
     * Keeps the text format simple for pentesters and explicit enough for AUCO to separate guidance from request bytes.
     */
    private String guidanceBlock(String customInstructions) {
        String instructions = normalizedInstructions(customInstructions);
        return """
                INSTRUCTIONS:
                %s

                AUCO GUIDANCE:
                - Treat the HTTP request below as the exact target.
                - Focus on exploitable, reproducible security findings.
                - Prefer evidence-backed attack paths over generic best practices.
                - Keep findings concise and include validation steps.

                =============
                RAW HTTP REQUEST:
                """.formatted(instructions);
    }

    /**
     * Blank prompts fall back to useful default guidance so every backlog task remains actionable.
     */
    private String normalizedInstructions(String customInstructions) {
        String trimmedInstructions = Objects.requireNonNullElse(customInstructions, "").trim();
        return trimmedInstructions.isEmpty() ? DEFAULT_INSTRUCTIONS : trimmedInstructions;
    }
}
