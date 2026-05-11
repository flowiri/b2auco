package com.example.b2auco.results;

import org.junit.jupiter.api.Test;

import java.awt.Color;

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

    /**
     * Dark-theme rendering must avoid black text on dark backgrounds and bright white inline-code boxes.
     */
    @Test
    void rendersDarkThemeWithMatchingTextAndCodeColors() {
        String html = renderer.render(
                "# Scan Report\n\n- Target: `localhost:5050`",
                MarkdownHtmlRenderer.RenderTheme.fromColors(new Color(210, 210, 210), new Color(48, 48, 48))
        );

        assertTrue(html.contains("color: #d2d2d2"));
        assertTrue(html.contains("background-color: #303030"));
        assertTrue(html.contains("background-color: #4c4c4c"));
    }
}
