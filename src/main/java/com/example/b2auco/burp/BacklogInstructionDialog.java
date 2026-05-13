package com.example.b2auco.burp;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Optional;

/**
 * Collects optional AUCO backlog instructions from the Burp user before backlog exports are written.
 */
public final class BacklogInstructionDialog {
    private static final int PROMPT_ROWS = 8;
    private static final int PROMPT_COLUMNS = 72;

    /**
     * Utility class only exposes a static prompt method because the dialog has no retained state.
     */
    private BacklogInstructionDialog() {
    }

    /**
     * Shows one prompt for the whole selected batch so multi-request backlog exports share the same focus guidance.
     */
    public static Optional<String> prompt() {
        JTextArea instructionsArea = new JTextArea(PROMPT_ROWS, PROMPT_COLUMNS);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);

        JScrollPane instructionsScrollPane = new JScrollPane(instructionsArea);
        instructionsScrollPane.setPreferredSize(new Dimension(720, 180));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("AUCO focus instructions for the selected backlog request(s):"), BorderLayout.NORTH);
        panel.add(instructionsScrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Send to Backlog",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        return Optional.of(instructionsArea.getText());
    }
}
