package com.example.b2auco.settings;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class FolderSettingsTab {
    private final FolderSettingsController controller;
    private final Function<String, Optional<Path>> folderChooser;

    private final JPanel panel;
    private final JPanel globalSectionPanel;
    private final JPanel projectSectionPanel;
    private final JLabel summaryPathLabel;
    private final JLabel summarySourceLabel;
    private final JTextField globalField;
    private final JButton globalBrowseButton;
    private final JButton globalSaveButton;
    private final JLabel globalHelperLabel;
    private final JLabel globalFeedbackLabel;
    private final JTextField projectField;
    private final JButton projectBrowseButton;
    private final JButton projectSaveButton;
    private final JLabel projectHelperLabel;
    private final JLabel projectFeedbackLabel;

    public FolderSettingsTab(FolderSettingsController controller) {
        this(controller, ignored -> Optional.empty());
    }

    FolderSettingsTab(FolderSettingsController controller, Function<String, Optional<Path>> folderChooser) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.folderChooser = Objects.requireNonNull(folderChooser, "folderChooser");

        panel = createPanel("rootPanel");
        panel.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JPanel titleBlock = createPanel("titleBlock");
        JLabel titleLabel = new JLabel();
        JLabel introLabel = new JLabel();
        titleBlock.add(titleLabel);
        titleBlock.add(introLabel);

        JPanel effectiveSummary = createPanel("effectiveSummary");
        JLabel summaryLabel = new JLabel();
        summaryPathLabel = new JLabel();
        summarySourceLabel = new JLabel();
        effectiveSummary.add(summaryLabel);
        effectiveSummary.add(summaryPathLabel);
        effectiveSummary.add(summarySourceLabel);

        globalSectionPanel = createPanel("globalSection");
        JLabel globalHeadingLabel = new JLabel();
        globalField = new JTextField(30);
        globalBrowseButton = new JButton();
        globalHelperLabel = new JLabel();
        globalSaveButton = new JButton();
        globalFeedbackLabel = new JLabel();
        globalSectionPanel.add(globalHeadingLabel);
        globalSectionPanel.add(globalField);
        globalSectionPanel.add(globalBrowseButton);
        globalSectionPanel.add(globalHelperLabel);
        globalSectionPanel.add(globalSaveButton);
        globalSectionPanel.add(globalFeedbackLabel);

        projectSectionPanel = createPanel("projectSection");
        JLabel projectHeadingLabel = new JLabel();
        projectField = new JTextField(30);
        projectBrowseButton = new JButton();
        projectHelperLabel = new JLabel();
        projectSaveButton = new JButton();
        projectFeedbackLabel = new JLabel();
        projectSectionPanel.add(projectHeadingLabel);
        projectSectionPanel.add(projectField);
        projectSectionPanel.add(projectBrowseButton);
        projectSectionPanel.add(projectHelperLabel);
        projectSectionPanel.add(projectSaveButton);
        projectSectionPanel.add(projectFeedbackLabel);

        panel.add(titleBlock);
        panel.add(effectiveSummary);
        panel.add(globalSectionPanel);
        panel.add(projectSectionPanel);

        globalBrowseButton.addActionListener(event -> chooseFolder(globalField));
        projectBrowseButton.addActionListener(event -> chooseFolder(projectField));
        globalSaveButton.addActionListener(event -> applyResult(controller.saveGlobalFolder(globalField.getText())));
        projectSaveButton.addActionListener(event -> applyResult(controller.saveProjectOverride(projectField.getText())));

        FolderSettingsViewState initialState = controller.loadViewState();
        titleLabel.setText(initialState.title());
        introLabel.setText(initialState.introText());
        summaryLabel.setText(initialState.summaryLabel());
        globalHeadingLabel.setText(initialState.globalSection().heading());
        projectHeadingLabel.setText(initialState.projectSection().heading());

        applyViewState(initialState);
    }

    public JPanel panel() {
        return panel;
    }

    JPanel globalSectionPanel() {
        return globalSectionPanel;
    }

    JPanel projectSectionPanel() {
        return projectSectionPanel;
    }

    JTextField globalField() {
        return globalField;
    }

    JButton globalBrowseButton() {
        return globalBrowseButton;
    }

    JButton globalSaveButton() {
        return globalSaveButton;
    }

    JLabel globalHelperLabel() {
        return globalHelperLabel;
    }

    JLabel globalFeedbackLabel() {
        return globalFeedbackLabel;
    }

    JTextField projectField() {
        return projectField;
    }

    JButton projectBrowseButton() {
        return projectBrowseButton;
    }

    JButton projectSaveButton() {
        return projectSaveButton;
    }

    JLabel projectHelperLabel() {
        return projectHelperLabel;
    }

    JLabel summaryPathLabel() {
        return summaryPathLabel;
    }

    JLabel summarySourceLabel() {
        return summarySourceLabel;
    }

    private void chooseFolder(JTextField field) {
        folderChooser.apply(field.getText())
                .map(Path::toString)
                .ifPresent(field::setText);
    }

    private void applyResult(FolderSaveResult result) {
        applyViewState(result.viewState());
    }

    private void applyViewState(FolderSettingsViewState state) {
        summaryPathLabel.setText(state.summaryFolderPath());
        summarySourceLabel.setText(state.summarySourceLabel());
        applySectionState(state.globalSection(), globalField, globalBrowseButton, globalSaveButton, globalHelperLabel, globalFeedbackLabel);
        applySectionState(state.projectSection(), projectField, projectBrowseButton, projectSaveButton, projectHelperLabel, projectFeedbackLabel);
    }

    private void applySectionState(
            FolderSettingsViewState.SectionState state,
            JTextField field,
            JButton browseButton,
            JButton saveButton,
            JLabel helperLabel,
            JLabel feedbackLabel
    ) {
        field.setText(state.fieldValue());
        field.setEnabled(state.enabled());
        browseButton.setText(state.browseLabel());
        browseButton.setEnabled(state.enabled());
        saveButton.setText(state.actionLabel());
        saveButton.setEnabled(state.enabled());
        helperLabel.setText(state.helperText());
        feedbackLabel.setText(state.feedbackMessage());
    }

    private static JPanel createPanel(String name) {
        JPanel panel = new JPanel();
        panel.setName(name);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
}
