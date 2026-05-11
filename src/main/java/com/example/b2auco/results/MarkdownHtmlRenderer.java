package com.example.b2auco.results;

import java.awt.Color;

/**
 * Converts the small markdown subset emitted by AUCO reports into Swing-compatible HTML.
 */
public final class MarkdownHtmlRenderer {
    /**
     * Builds a complete HTML document so JEditorPane renders reports as readable headings, lists, code, and emphasis.
     */
    public String render(String markdown) {
        return render(markdown, RenderTheme.light());
    }

    /**
     * Builds themed HTML so Burp dark and light themes both render readable report content.
     */
    public String render(String markdown, RenderTheme theme) {
        String source = markdown == null ? "" : markdown;
        RenderTheme safeTheme = theme == null ? RenderTheme.light() : theme;
        StringBuilder body = new StringBuilder();
        boolean inList = false;
        boolean inCodeBlock = false;
        boolean inParagraph = false;

        for (String line : source.split("\\R", -1)) {
            if (line.startsWith("```")) {
                if (inParagraph) {
                    body.append("</p>");
                    inParagraph = false;
                }
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                if (inCodeBlock) {
                    body.append("</code></pre>");
                    inCodeBlock = false;
                } else {
                    body.append("<pre><code>");
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                body.append(escapeHtml(line)).append('\n');
                continue;
            }

            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                if (inParagraph) {
                    body.append("</p>");
                    inParagraph = false;
                }
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                continue;
            }

            if (trimmedLine.startsWith("# ")) {
                if (inParagraph) {
                    body.append("</p>");
                    inParagraph = false;
                }
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                body.append("<h1>").append(renderInline(trimmedLine.substring(2))).append("</h1>");
                continue;
            }

            if (trimmedLine.startsWith("## ")) {
                if (inParagraph) {
                    body.append("</p>");
                    inParagraph = false;
                }
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                body.append("<h2>").append(renderInline(trimmedLine.substring(3))).append("</h2>");
                continue;
            }

            if (trimmedLine.startsWith("- ")) {
                if (inParagraph) {
                    body.append("</p>");
                    inParagraph = false;
                }
                if (!inList) {
                    body.append("<ul>");
                    inList = true;
                }
                body.append("<li>").append(renderInline(trimmedLine.substring(2))).append("</li>");
                continue;
            }

            if (!inParagraph) {
                body.append("<p>");
                inParagraph = true;
            } else {
                body.append("<br>");
            }
            body.append(renderInline(trimmedLine));
        }

        if (inCodeBlock) {
            body.append("</code></pre>");
        }
        if (inParagraph) {
            body.append("</p>");
        }
        if (inList) {
            body.append("</ul>");
        }

        return """
                <html>
                <head>
                <style>
                body { font-family: sans-serif; font-size: 12px; margin: 10px; color: %s; background-color: %s; }
                h1 { font-size: 15px; margin: 0 0 10px 0; color: %s; }
                h2 { font-size: 13px; margin: 14px 0 8px 0; color: %s; }
                ul { margin: 4px 0 10px 20px; padding: 0; }
                li { margin: 4px 0; }
                code { font-family: monospace; color: %s; background-color: %s; }
                pre { color: %s; background-color: %s; padding: 8px; }
                p { margin: 8px 0; }
                </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(
                safeTheme.foregroundHex(),
                safeTheme.backgroundHex(),
                safeTheme.foregroundHex(),
                safeTheme.foregroundHex(),
                safeTheme.foregroundHex(),
                safeTheme.codeBackgroundHex(),
                safeTheme.foregroundHex(),
                safeTheme.codeBackgroundHex(),
                body
        );
    }

    /**
     * Applies inline code and bold markup after HTML escaping so report text cannot inject Swing HTML.
     */
    private String renderInline(String text) {
        String escaped = escapeHtml(text);
        escaped = escaped.replaceAll("`([^`]+)`", "<code>$1</code>");
        return escaped.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
    }

    /**
     * Escapes the HTML-sensitive characters supported by Swing's basic HTML renderer.
     */
    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Carries precomputed colors as HTML hex strings because Swing's HTML renderer does not understand UIManager keys.
     */
    public record RenderTheme(String foregroundHex, String backgroundHex, String codeBackgroundHex) {
        /**
         * Provides a readable fallback for direct renderer tests and non-Swing callers.
         */
        public static RenderTheme light() {
            return new RenderTheme("#222222", "#ffffff", "#f2f2f2");
        }

        /**
         * Converts Swing colors into a small theme with a subtle inline-code background.
         */
        public static RenderTheme fromColors(Color foreground, Color background) {
            Color safeForeground = foreground == null ? Color.BLACK : foreground;
            Color safeBackground = background == null ? Color.WHITE : background;
            return new RenderTheme(
                    toHex(safeForeground),
                    toHex(safeBackground),
                    toHex(adjustForCodeBackground(safeBackground))
            );
        }

        /**
         * Dark themes need lighter code blocks, while light themes need darker-but-still-subtle code blocks.
         */
        private static Color adjustForCodeBackground(Color background) {
            int average = (background.getRed() + background.getGreen() + background.getBlue()) / 3;
            int delta = average < 128 ? 28 : -12;
            return new Color(
                    clamp(background.getRed() + delta),
                    clamp(background.getGreen() + delta),
                    clamp(background.getBlue() + delta)
            );
        }

        /**
         * Keeps derived RGB values inside the valid CSS color range.
         */
        private static int clamp(int value) {
            return Math.max(0, Math.min(255, value));
        }

        /**
         * Serializes a Java color to a CSS hex triplet understood by Swing's basic HTML renderer.
         */
        private static String toHex(Color color) {
            return "#%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue());
        }
    }
}
