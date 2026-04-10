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
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
    private static final int SECTION_PADDING = 12;
    private static final int CONTENT_WIDTH_FLOOR = 720;
    private static final Border ACTIVE_TAB_BORDER = BorderFactory.createCompoundBorder(
            defaultBorder("Button.border"),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
    );
    private static final Border INACTIVE_TAB_BORDER = BorderFactory.createEmptyBorder(7, 13, 7, 13);

    private final FolderSettingsController controller;
    private final Function<String, Optional<Path>> folderChooser;

    private final JPanel panel;
    private final JPanel contentPanel;
    private final JPanel globalSectionPanel;
    private final JPanel projectSectionPanel;
    private final JButton userSettingTabButton;
    private final JButton projectSettingTabButton;
    private final JTextField summaryPathField;
    private final JLabel summarySourceLabel;
    private final JTextField globalField;
    private final JButton globalBrowseButton;
    private final JButton globalSaveButton;
    private final JLabel globalHelperLabel;
    private final JLabel globalFeedbackLabel;
    private final JCheckBox userProjectOverrideToggle;
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
        panel.setBackground(defaultColor("Panel.background", Color.LIGHT_GRAY));

        contentPanel = createVerticalPanel("contentPanel");
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        contentPanel.setBackground(panel.getBackground());

        JPanel titleBlock = createSectionPanel("titleBlock");
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel();
        JLabel introLabel = new JLabel();
        styleHeadingLabel(titleLabel);
        styleMutedLabel(introLabel, false);
        JPanel tabSelectorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tabSelectorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabSelectorPanel.setOpaque(false);
        userSettingTabButton = createTabButton("User setting");
        projectSettingTabButton = createTabButton("Project setting");
        tabSelectorPanel.add(userSettingTabButton);
        tabSelectorPanel.add(projectSettingTabButton);
        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(tabSelectorPanel, BorderLayout.EAST);
        titleBlock.add(titleRow);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(introLabel);

        JPanel effectiveSummary = createSectionPanel("effectiveSummary");
        JLabel summaryLabel = new JLabel();
        styleSectionLabel(summaryLabel);
        summaryPathField = createPathField(false);
        summarySourceLabel = new JLabel();
        styleMutedLabel(summarySourceLabel, true);
        effectiveSummary.add(summaryLabel);
        effectiveSummary.add(Box.createVerticalStrut(4));
        effectiveSummary.add(createFieldRow(summaryPathField));
        effectiveSummary.add(Box.createVerticalStrut(4));
        effectiveSummary.add(summarySourceLabel);

        globalSectionPanel = createSectionPanel("globalSection");
        JLabel globalHeadingLabel = new JLabel();
        styleSectionLabel(globalHeadingLabel);
        globalField = createPathField(true);
        globalBrowseButton = new JButton();
        globalSaveButton = new JButton();
        globalHelperLabel = new JLabel();
        styleMutedLabel(globalHelperLabel, false);
        globalFeedbackLabel = new JLabel();
        styleMutedLabel(globalFeedbackLabel, false);
        userProjectOverrideToggle = new JCheckBox();
        globalSectionPanel.add(globalHeadingLabel);
        globalSectionPanel.add(Box.createVerticalStrut(4));
        globalSectionPanel.add(createFieldRow(globalField, globalBrowseButton, globalSaveButton));
        globalSectionPanel.add(Box.createVerticalStrut(4));
        globalSectionPanel.add(createToggleRow(userProjectOverrideToggle));
        globalSectionPanel.add(Box.createVerticalStrut(4));
        globalSectionPanel.add(globalHelperLabel);
        globalSectionPanel.add(Box.createVerticalStrut(4));
        globalSectionPanel.add(globalFeedbackLabel);

        projectSectionPanel = createSectionPanel("projectSection");
        JLabel projectHeadingLabel = new JLabel();
        styleSectionLabel(projectHeadingLabel);
        projectOverrideToggle = new JCheckBox();
        projectField = createPathField(true);
        projectBrowseButton = new JButton();
        projectSaveButton = new JButton();
        projectHelperLabel = new JLabel();
        styleMutedLabel(projectHelperLabel, false);
        projectFeedbackLabel = new JLabel();
        styleMutedLabel(projectFeedbackLabel, false);
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

        userSettingTabButton.addActionListener(event -> applyViewState(controller.showUserSettings(), false, false));
        projectSettingTabButton.addActionListener(event -> applyViewState(controller.showProjectSettings(), false, false));
        userProjectOverrideToggle.addActionListener(event -> handleProjectOverrideToggle(userProjectOverrideToggle.isSelected()));
        globalBrowseButton.addActionListener(event -> chooseFolder(globalField));
        projectBrowseButton.addActionListener(event -> chooseAndSaveFolder(projectField, controller::saveProjectOverride));
        globalSaveButton.addActionListener(event -> applyResult(controller.saveGlobalFolder(globalField.getText())));
        projectSaveButton.addActionListener(event -> applyResult(controller.saveProjectOverride(projectField.getText())));
        projectOverrideToggle.addActionListener(event -> handleProjectOverrideToggle(projectOverrideToggle.isSelected()));

        FolderSettingsViewState initialState = controller.loadViewState();
        titleLabel.setText(initialState.title());
        introLabel.setText(initialState.introText());
        summaryLabel.setText(initialState.summaryLabel());
        globalHeadingLabel.setText(initialState.globalSection().heading());
        projectHeadingLabel.setText(initialState.projectSection().heading());

        applyViewState(initialState, true, true);
        projectField.setText(initialState.projectSection().fieldValue());
        projectField.setCaretPosition(0);
    }

    public JPanel panel() {
        return panel;
    }

    JPanel contentPanel() {
        return contentPanel;
    }

    JPanel globalSectionPanel() {
        return globalSectionPanel;
    }

    JPanel projectSectionPanel() {
        return projectSectionPanel;
    }

    JButton userSettingTabButton() {
        return userSettingTabButton;
    }

    JButton projectSettingTabButton() {
        return projectSettingTabButton;
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

    JCheckBox userProjectOverrideToggle() {
        return userProjectOverrideToggle;
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

    private void handleProjectOverrideToggle(boolean enabled) {
        FolderSettingsViewState viewState = controller.setProjectOverrideEnabled(enabled);
        applyViewState(viewState, false, false);
        userProjectOverrideToggle.setSelected(enabled);
        projectOverrideToggle.setSelected(enabled);
        projectField.setEnabled(enabled);
        projectBrowseButton.setEnabled(enabled);
        projectSaveButton.setEnabled(enabled);
    }

    private void applyResult(FolderSaveResult result) {
        boolean refreshGlobalField = result.scope() == FolderSettingsController.Scope.GLOBAL && result.success();
        boolean refreshProjectField = result.scope() == FolderSettingsController.Scope.PROJECT && result.success();
        applyViewState(result.viewState(), refreshGlobalField, refreshProjectField);
    }

    private void applyViewState(FolderSettingsViewState state, boolean refreshGlobalField, boolean refreshProjectField) {
        summaryPathField.setText(state.summaryFolderPath());
        summaryPathField.setCaretPosition(0);
        summarySourceLabel.setText(state.summarySourceLabel());
        applyActiveMode(state.activeMode());
        applySectionState(state.globalSection(), userProjectOverrideToggle, globalField, globalBrowseButton, globalSaveButton, globalHelperLabel, globalFeedbackLabel, refreshGlobalField);
        applySectionState(state.projectSection(), projectOverrideToggle, projectField, projectBrowseButton, projectSaveButton, projectHelperLabel, projectFeedbackLabel, refreshProjectField);
    }

    private void applyActiveMode(FolderSettingsViewState.ActiveMode activeMode) {
        boolean userSettingActive = activeMode == FolderSettingsViewState.ActiveMode.USER_SETTING;
        globalSectionPanel.setVisible(userSettingActive);
        projectSectionPanel.setVisible(!userSettingActive);
        applyTabStyle(userSettingTabButton, userSettingActive);
        applyTabStyle(projectSettingTabButton, !userSettingActive);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void applyTabStyle(JButton button, boolean active) {
        button.setBackground(active ? defaultColor("Panel.background", button.getBackground()) : defaultColor("Button.background", button.getBackground()));
        button.setForeground(active ? defaultColor("Label.foreground", button.getForeground()) : defaultColor("Button.foreground", button.getForeground()));
        button.setBorder(active ? ACTIVE_TAB_BORDER : INACTIVE_TAB_BORDER);
        button.setFont(button.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN));
    }

    private void applySectionState(
            FolderSettingsViewState.SectionState state,
            JCheckBox toggle,
            JTextField field,
            JButton browseButton,
            JButton saveButton,
            JLabel helperLabel,
            JLabel feedbackLabel,
            boolean refreshFieldValue
    ) {
        if (refreshFieldValue) {
            field.setText(state.fieldValue());
        }
        field.setCaretPosition(0);
        field.setEnabled(state.controlsEnabled());
        browseButton.setText(state.browseLabel());
        browseButton.setEnabled(state.controlsEnabled());
        saveButton.setText(state.actionLabel());
        saveButton.setEnabled(state.controlsEnabled());
        helperLabel.setText(state.helperText());
        feedbackLabel.setText(state.feedbackMessage());

        if (toggle != null) {
            boolean preserveUserToggleSelection = toggle == userProjectOverrideToggle
                    && state.mirrorsProjectOverrideToggle()
                    && userProjectOverrideToggle.isSelected()
                    && state.toggleSelected();
            toggle.setVisible(state.toggleVisible());
            toggle.setText(state.toggleLabel());
            toggle.setEnabled(state.toggleEnabled());
            if (!preserveUserToggleSelection) {
                toggle.setSelected(state.toggleSelected());
            }
        }
    }

    private static JButton createTabButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(defaultColor("Button.background", button.getBackground()));
        button.setForeground(defaultColor("Button.foreground", button.getForeground()));
        button.setBorder(INACTIVE_TAB_BORDER);
        return button;
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
        row.setOpaque(false);
        row.add(field);
        for (JButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    private static JPanel createToggleRow(JCheckBox checkBox) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setOpaque(false);
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

    private static void addSection(JPanel container, JPanel section, boolean addGapBefore) {
        if (addGapBefore) {
            container.add(Box.createVerticalStrut(SECTION_GAP));
        }
        container.add(section);
    }

    private static JPanel createSectionPanel(String name) {
        JPanel panel = createVerticalPanel(name);
        panel.setMinimumSize(new Dimension(CONTENT_WIDTH_FLOOR, panel.getMinimumSize().height));
        panel.setOpaque(true);
        panel.setBackground(defaultColor("Panel.background", panel.getBackground()));
        panel.setBorder(BorderFactory.createCompoundBorder(
                defaultBorder("TitledBorder.border"),
                BorderFactory.createEmptyBorder(SECTION_PADDING, SECTION_PADDING, SECTION_PADDING, SECTION_PADDING)
        ));
        return panel;
    }

    private static JPanel createVerticalPanel(String name) {
        JPanel panel = new JPanel();
        panel.setName(name);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(defaultColor("Panel.background", panel.getBackground()));
        return panel;
    }

    private static void styleHeadingLabel(JLabel label) {
        label.setForeground(defaultColor("Label.foreground", label.getForeground()));
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 2.0f));
    }

    private static void styleSectionLabel(JLabel label) {
        label.setForeground(defaultColor("Label.foreground", label.getForeground()));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
    }

    private static void styleMutedLabel(JLabel label, boolean italic) {
        label.setForeground(defaultColor("Label.disabledForeground", defaultColor("Label.foreground", label.getForeground())));
        int style = italic ? Font.ITALIC : Font.PLAIN;
        label.setFont(label.getFont().deriveFont(style));
    }

    private static Color defaultColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }

    private static Border defaultBorder(String key) {
        Border border = UIManager.getBorder(key);
        return border != null ? border : BorderFactory.createLineBorder(defaultColor("Separator.foreground", Color.GRAY));
    }
}
