package com.example.b2auco.results;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the dependency-free markdown renderer used by the Swing results viewer.
 */
class MarkdownHtmlRendererTest {
    private final MarkdownHtmlRenderer renderer = new MarkdownHtmlRenderer();

    /**
     * AUCO markdown reports should become readable HTML with headings, lists, inline code, and bold findings.
     */
    @Test
    void rendersAucoReportMarkdownAsHumanReadableHtml() {
        String html = renderer.render("""
                # Scan Report: localhost-graphql-1-1

                - Target: `localhost:5050`

                ## Validated Findings
                - **GraphQL issue** (`high`)
                """);

        assertTrue(html.contains("<h1>Scan Report: localhost-graphql-1-1</h1>"));
        assertTrue(html.contains("<li>Target: <code>localhost:5050</code></li>"));
        assertTrue(html.contains("<h2>Validated Findings</h2>"));
        assertTrue(html.contains("<strong>GraphQL issue</strong>"));
    }

    /**
     * Escaping happens before inline markdown conversion so report content cannot inject arbitrary HTML.
     */
    @Test
    void escapesHtmlBeforeRenderingInlineMarkdown() {
        String html = renderer.render("- **<script>alert(1)</script>**");

        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }
}
