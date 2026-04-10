package com.example.b2auco.settings;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class FolderSettingsTab {
    private static final int PATH_FIELD_COLUMNS = 30;
    private static final int OUTER_PADDING = 16;
    private static final int SECTION_GAP = 16;
    private static final int CONTENT_WIDTH_FLOOR = 720;

    private final FolderSettingsController controller;
    private final Function<String, Optional<Path>> folderChooser;

    private final JPanel panel;
    private final JPanel contentPanel;
    private final JPanel globalSectionPanel;
    private final JPanel projectSectionPanel;
    private final JTextField summaryPathField;
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
    private final JCheckBox projectOverrideToggle;

    public FolderSettingsTab(FolderSettingsController controller) {
        this(controller, FolderSettingsTab::showDirectoryChooser);
    }

    FolderSettingsTab(FolderSettingsController controller, Function<String, Optional<Path>> folderChooser) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.folderChooser = Objects.requireNonNull(folderChooser, "folderChooser");

        panel = new JPanel(new BorderLayout());
        panel.setName("rootPanel");
        panel.setBorder(BorderFactory.createEmptyBorder(OUTER_PADDING, OUTER_PADDING, OUTER_PADDING, OUTER_PADDING));

        contentPanel = createVerticalPanel("contentPanel");
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel titleBlock = createSectionPanel("titleBlock");
        JLabel titleLabel = new JLabel();
        JLabel introLabel = new JLabel();
        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(introLabel);

        JPanel effectiveSummary = createSectionPanel("effectiveSummary");
        JLabel summaryLabel = new JLabel();
        summaryPathField = createPathField(false);
        summarySourceLabel = new JLabel();
        effectiveSummary.add(summaryLabel);
        effectiveSummary.add(Box.createVerticalStrut(4));
        effectiveSummary.add(createFieldRow(summaryPathField));
        effectiveSummary.add(Box.createVerticalStrut(4));
        effectiveSummary.add(summarySourceLabel);

        globalSectionPanel = createSectionPanel("globalSection");
        JLabel globalHeadingLabel = new JLabel();
        globalField = createPathField(true);
        globalBrowseButton = new JButton();
        globalSaveButton = new JButton();
        globalHelperLabel = new JLabel();
        globalFeedbackLabel = new JLabel();
        globalSectionPanel.add(globalHeadingLabel);
        globalSectionPanel.add(Box.createVerticalStrut(4));
        globalSectionPanel.add(createFieldRow(globalField, globalBrowseButton, globalSaveButton));
        globalSectionPanel.add(Box.createVerticalStrut(4));
        globalSectionPanel.add(globalHelperLabel);
        globalSectionPanel.add(Box.createVerticalStrut(4));
        globalSectionPanel.add(globalFeedbackLabel);

        projectSectionPanel = createSectionPanel("projectSection");
        JLabel projectHeadingLabel = new JLabel();
        projectOverrideToggle = new JCheckBox();
        projectField = createPathField(true);
        projectBrowseButton = new JButton();
        projectSaveButton = new JButton();
        projectHelperLabel = new JLabel();
        projectFeedbackLabel = new JLabel();
        projectSectionPanel.add(projectHeadingLabel);
        projectSectionPanel.add(Box.createVerticalStrut(4));
        projectSectionPanel.add(createToggleRow(projectOverrideToggle));
        projectSectionPanel.add(Box.createVerticalStrut(4));
        projectSectionPanel.add(createFieldRow(projectField, projectBrowseButton, projectSaveButton));
        projectSectionPanel.add(Box.createVerticalStrut(4));
        projectSectionPanel.add(projectHelperLabel);
        projectSectionPanel.add(Box.createVerticalStrut(4));
        projectSectionPanel.add(projectFeedbackLabel);

        addSection(contentPanel, titleBlock, false);
        addSection(contentPanel, effectiveSummary, true);
        addSection(contentPanel, globalSectionPanel, true);
        addSection(contentPanel, projectSectionPanel, true);
        panel.add(contentPanel, BorderLayout.NORTH);

        globalBrowseButton.addActionListener(event -> chooseFolder(globalField));
        projectBrowseButton.addActionListener(event -> chooseAndSaveFolder(projectField, controller::saveProjectOverride));
        globalSaveButton.addActionListener(event -> applyResult(controller.saveGlobalFolder(globalField.getText())));
        projectSaveButton.addActionListener(event -> applyResult(controller.saveProjectOverride(projectField.getText())));
        projectOverrideToggle.addActionListener(event -> applyViewState(controller.setProjectOverrideEnabled(projectOverrideToggle.isSelected())));

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

    JLabel projectFeedbackLabel() {
        return projectFeedbackLabel;
    }

    JCheckBox projectOverrideToggle() {
        return projectOverrideToggle;
    }

    JTextField summaryPathField() {
        return summaryPathField;
    }

    JLabel summarySourceLabel() {
        return summarySourceLabel;
    }

    private void chooseFolder(JTextField field) {
        folderChooser.apply(field.getText())
                .map(Path::toString)
                .ifPresent(field::setText);
    }

    private void chooseAndSaveFolder(JTextField field, Function<String, FolderSaveResult> saveAction) {
        folderChooser.apply(field.getText())
                .map(Path::toString)
                .ifPresent(selectedPath -> {
                    field.setText(selectedPath);
                    applyResult(saveAction.apply(selectedPath));
                });
    }

    private static JTextField createPathField(boolean editable) {
        JTextField field = new JTextField(PATH_FIELD_COLUMNS);
        field.setEditable(editable);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension preferredSize = field.getPreferredSize();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));
        field.setMinimumSize(new Dimension(160, preferredSize.height));
        return field;
    }

    private static JPanel createFieldRow(JTextField field, JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(field);
        for (JButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    private static JPanel createToggleRow(JCheckBox checkBox) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(checkBox);
        return row;
    }

    private static Optional<Path> showDirectoryChooser(String currentValue) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        initialDirectory(currentValue).ifPresent(directory -> {
            chooser.setCurrentDirectory(directory.toFile());
            chooser.setSelectedFile(directory.toFile());
        });

        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }

        File selectedFile = chooser.getSelectedFile();
        return selectedFile == null ? Optional.empty() : Optional.of(selectedFile.toPath());
    }

    private static Optional<Path> initialDirectory(String currentValue) {
        if (currentValue == null || currentValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(currentValue));
        } catch (InvalidPathException exception) {
            return Optional.empty();
        }
    }

    private void applyResult(FolderSaveResult result) {
        applyViewState(result.viewState());
    }

    private void applyViewState(FolderSettingsViewState state) {
        summaryPathField.setText(state.summaryFolderPath());
        summaryPathField.setCaretPosition(0);
        summarySourceLabel.setText(state.summarySourceLabel());
        applySectionState(state.globalSection(), null, globalField, globalBrowseButton, globalSaveButton, globalHelperLabel, globalFeedbackLabel);
        applySectionState(state.projectSection(), projectOverrideToggle, projectField, projectBrowseButton, projectSaveButton, projectHelperLabel, projectFeedbackLabel);
    }

    private void applySectionState(
            FolderSettingsViewState.SectionState state,
            JCheckBox toggle,
            JTextField field,
            JButton browseButton,
            JButton saveButton,
            JLabel helperLabel,
            JLabel feedbackLabel
    ) {
        field.setText(state.fieldValue());
        field.setCaretPosition(0);
        field.setEnabled(state.controlsEnabled());
        browseButton.setText(state.browseLabel());
        browseButton.setEnabled(state.controlsEnabled());
        saveButton.setText(state.actionLabel());
        saveButton.setEnabled(state.controlsEnabled());
        helperLabel.setText(state.helperText());
        feedbackLabel.setText(state.feedbackMessage());

        if (toggle != null) {
            toggle.setVisible(state.toggleVisible());
            toggle.setText(state.toggleLabel());
            toggle.setEnabled(state.toggleEnabled());
            toggle.setSelected(state.toggleSelected());
        }
    }

    JPanel contentPanel() {
        return contentPanel;
    }

    private static void addSection(JPanel container, JPanel section, boolean addGapBefore) {
        if (addGapBefore) {
            container.add(Box.createVerticalStrut(SECTION_GAP));
        }
        container.add(section);
    }

    private static JPanel createSectionPanel(String name) {
        JPanel panel = createVerticalPanel(name);
        panel.setMinimumSize(new Dimension(CONTENT_WIDTH_FLOOR, panel.getMinimumSize().height));
        return panel;
    }

    private static JPanel createVerticalPanel(String name) {
        JPanel panel = new JPanel();
        panel.setName(name);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
}
