package com.example.b2auco.results;

/**
 * Converts the small markdown subset emitted by AUCO reports into Swing-compatible HTML.
 */
public final class MarkdownHtmlRenderer {
    /**
     * Builds a complete HTML document so JEditorPane renders reports as readable headings, lists, code, and emphasis.
     */
    public String render(String markdown) {
        String source = markdown == null ? "" : markdown;
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
                body { font-family: sans-serif; font-size: 12px; margin: 10px; color: #222; }
                h1 { font-size: 18px; margin: 0 0 10px 0; }
                h2 { font-size: 15px; margin: 14px 0 8px 0; }
                ul { margin: 4px 0 10px 20px; padding: 0; }
                li { margin: 4px 0; }
                code { font-family: monospace; background: #f2f2f2; }
                pre { background: #f2f2f2; padding: 8px; }
                p { margin: 8px 0; }
                </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(body);
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
}
