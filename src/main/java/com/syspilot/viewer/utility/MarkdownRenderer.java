package com.syspilot.viewer.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple markdown-to-HTML converter for trajectory message content.
 * Handles code blocks, inline code, bold, italic, headers, lists, and links.
 */
public class MarkdownRenderer {

    private final boolean darkMode;

    public MarkdownRenderer(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public String render(String reasoning, String message, String result) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>")
           .append(buildCss())
           .append("</style></head><body>");

        if (reasoning != null && !reasoning.isEmpty()) {
            html.append("<div class='section-title'>Reasoning</div>");
            html.append("<div class='reasoning'>").append(mdToHtml(reasoning)).append("</div>");
        }

        if (message != null && !message.isEmpty()) {
            html.append("<div class='section-title'>Message</div>");
            html.append("<div class='message'>").append(mdToHtml(message)).append("</div>");
        }

        if (result != null && !result.isEmpty()) {
            html.append("<div class='result-box'><div class='result-title'>Final Result</div>");
            html.append("<div class='result-content'>").append(mdToHtml(result)).append("</div></div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private String mdToHtml(String md) {
        if (md == null || md.isEmpty()) return "";
        String html = escapeHtml(md);

        // Code blocks (``` ... ```)
        html = html.replaceAll("(?s)```(\\w*)\\s*\\n?(.*?)```",
                "<pre><code>$2</code></pre>");

        // Inline code (`...`)
        html = html.replaceAll("`([^`]+)`", "<code>$1</code>");

        // Bold (**...** or __...__)
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("__(.+?)__", "<strong>$1</strong>");

        // Italic (*...* or _..._)
        html = html.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        html = html.replaceAll("_(.+?)_", "<em>$1</em>");

        // Headers
        html = html.replaceAll("(?m)^#### (.+)$", "<h4>$1</h4>");
        html = html.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");

        // Unordered lists
        html = html.replaceAll("(?m)^[*-] (.+)$", "<li>$1</li>");
        html = html.replaceAll("((<li>.*</li>\\s*)+)", "<ul>$1</ul>");

        // Ordered lists
        html = html.replaceAll("(?m)^\\d+\\.\\s+(.+)$", "<li>$1</li>");

        // Links [text](url)
        html = html.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href='$2'>$1</a>");

        // Line breaks (double newline → paragraph, single → <br>)
        html = html.replaceAll("\n\n+", "</p><p>");
        html = html.replaceAll("\n", "<br>");

        // Wrap in paragraph
        html = "<p>" + html + "</p>";

        // Clean up empty paragraphs
        html = html.replaceAll("<p>\\s*</p>", "");
        html = html.replaceAll("<p><(h|ul|pre)", "<$1");
        html = html.replaceAll("</(h\\d|ul|pre)></p>", "</$1>");

        return html;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String buildCss() {
        if (darkMode) {
            return """
                body { font-family: 'Segoe UI', 'Microsoft YaHei', Consolas, monospace;
                       font-size: 13px; background: #1a1b2e; color: #c8c8dc;
                       margin: 12px 16px; line-height: 1.6; }
                .section-title { font-size: 12px; color: #7a7a95; font-weight: bold;
                                 margin-top: 12px; margin-bottom: 4px; }
                .reasoning { margin-bottom: 8px; }
                .message { margin-bottom: 8px; }
                h1, h2, h3, h4 { color: #e0e0f0; margin: 8px 0 4px; }
                h1 { font-size: 18px; } h2 { font-size: 16px; }
                h3 { font-size: 14px; } h4 { font-size: 13px; }
                pre { background: #11121e; border: 1px solid #252640;
                      border-radius: 4px; padding: 10px 14px; overflow-x: auto;
                      font-family: Consolas, 'Microsoft YaHei', monospace; font-size: 12px; }
                code { background: #11121e; padding: 1px 5px; border-radius: 3px;
                       font-family: Consolas, 'Microsoft YaHei', monospace; font-size: 12px; }
                pre code { background: transparent; padding: 0; }
                ul { margin: 4px 0; padding-left: 24px; }
                li { margin: 1px 0; }
                a { color: #5dade2; }
                strong { color: #e0e0f0; }
                .result-box { background: #0d2818; border: 1.5px solid #27ae60;
                              border-radius: 6px; padding: 10px 14px; margin-top: 12px; }
                .result-title { font-size: 13px; color: #4cdf80; font-weight: bold;
                                margin-bottom: 4px; }
                .result-content { color: #a0d8b0; }
                """;
        } else {
            return """
                body { font-family: 'Segoe UI', 'Microsoft YaHei', Consolas, monospace;
                       font-size: 13px; background: #f5f5f5; color: #333333;
                       margin: 12px 16px; line-height: 1.6; }
                .section-title { font-size: 12px; color: #666666; font-weight: bold;
                                 margin-top: 12px; margin-bottom: 4px; }
                .reasoning { margin-bottom: 8px; }
                .message { margin-bottom: 8px; }
                h1, h2, h3, h4 { color: #222222; margin: 8px 0 4px; }
                h1 { font-size: 18px; } h2 { font-size: 16px; }
                h3 { font-size: 14px; } h4 { font-size: 13px; }
                pre { background: #ffffff; border: 1px solid #cccccc;
                      border-radius: 4px; padding: 10px 14px; overflow-x: auto;
                      font-family: Consolas, 'Microsoft YaHei', monospace; font-size: 12px; }
                code { background: #e8e8e8; padding: 1px 5px; border-radius: 3px;
                       font-family: Consolas, 'Microsoft YaHei', monospace; font-size: 12px; }
                pre code { background: transparent; padding: 0; }
                ul { margin: 4px 0; padding-left: 24px; }
                li { margin: 1px 0; }
                a { color: #1a6d9f; }
                strong { color: #222222; }
                .result-box { background: #e6f4ea; border: 1.5px solid #27ae60;
                              border-radius: 6px; padding: 10px 14px; margin-top: 12px; }
                .result-title { font-size: 13px; color: #1a6d20; font-weight: bold;
                                margin-bottom: 4px; }
                .result-content { color: #2d5a2d; }
                """;
        }
    }
}
