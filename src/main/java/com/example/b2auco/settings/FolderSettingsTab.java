package com.example.b2auco.settings;

import com.example.b2auco.results.MarkdownHtmlRenderer;
import com.example.b2auco.results.ResultFileSummary;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class FolderSettingsTab {
    private static final int PATH_FIELD_COLUMNS = 30;
    private static final int OUTER_PADDING = 16;
    private static final int TITLE_BLOCK_PADDING = 4;
    private static final int SECTION_GAP = 16;
    private static final int TITLE_GAP = 6;
    private static final int SECTION_PADDING = 14;
    private static final int SUMMARY_PADDING = 16;
    private static final int CONTENT_WIDTH_FLOOR = 720;
    private static final int RESULTS_AUTO_REFRESH_INTERVAL_MILLIS = 2_000;
    private static final Border ACTIVE_TAB_BORDER = BorderFactory.createCompoundBorder(
            defaultBorder("Button.border"),
            BorderFactory.createEmptyBorder(7, 14, 7, 14)
    );
    private static final Border INACTIVE_TAB_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(1, 1, 1, 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
    );

    private final FolderSettingsController controller;
    private final Function<String, Optional<Path>> folderChooser;

    private final JPanel panel;
    private final JPanel contentPanel;
    private final JPanel globalSectionPanel;
    private final JPanel projectSectionPanel;
    private final JPanel authSectionPanel;
    private final JPanel backlogSectionPanel;
    private final JPanel resultsSectionPanel;
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
    private final JTextField authField;
    private final JButton authBrowseButton;
    private final JButton authSaveButton;
    private final JLabel authHelperLabel;
    private final JLabel authFeedbackLabel;
    private final JTextField backlogField;
    private final JButton backlogBrowseButton;
    private final JButton backlogSaveButton;
    private final JLabel backlogHelperLabel;
    private final JLabel backlogFeedbackLabel;
    private final JTextField resultsField;
    private final JButton resultsBrowseButton;
    private final JButton resultsSaveButton;
    private final JButton resultsRefreshButton;
    private final JComboBox<ResultReportOption> resultsReportSelector;
    private final JLabel resultsHelperLabel;
    private final JLabel resultsFeedbackLabel;
    private final JLabel resultsStatusLabel;
    private final JEditorPane resultsContentPane;
    private final Timer resultsAutoRefreshTimer;
    private final MarkdownHtmlRenderer markdownHtmlRenderer = new MarkdownHtmlRenderer();
    private boolean applyingViewState;

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
        titleBlock.setBorder(BorderFactory.createEmptyBorder(TITLE_BLOCK_PADDING, TITLE_BLOCK_PADDING, TITLE_BLOCK_PADDING, TITLE_BLOCK_PADDING));
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel();
        JLabel introLabel = new JLabel();
        styleHeadingLabel(titleLabel);
        styleMutedLabel(introLabel, false);
        JPanel tabSelectorPanel = createTabSelectorPanel();
        userSettingTabButton = createTabButton("User setting");
        projectSettingTabButton = createTabButton("Project setting");
        tabSelectorPanel.add(userSettingTabButton);
        tabSelectorPanel.add(projectSettingTabButton);
        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(tabSelectorPanel, BorderLayout.EAST);
        titleBlock.add(titleRow);
        titleBlock.add(Box.createVerticalStrut(TITLE_GAP));
        titleBlock.add(introLabel);

        JPanel effectiveSummary = createSectionPanel("effectiveSummary");
        effectiveSummary.setBorder(createSectionBorder(SUMMARY_PADDING));
        JLabel summaryLabel = new JLabel();
        styleSectionLabel(summaryLabel);
        summaryPathField = createPathField(false);
        summarySourceLabel = new JLabel();
        styleMutedLabel(summarySourceLabel, true);
        effectiveSummary.add(summaryLabel);
        effectiveSummary.add(Box.createVerticalStrut(TITLE_GAP));
        effectiveSummary.add(createFieldRow(summaryPathField));
        effectiveSummary.add(Box.createVerticalStrut(TITLE_GAP));
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
        globalSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        globalSectionPanel.add(createFieldRow(globalField, globalBrowseButton, globalSaveButton));
        globalSectionPanel.add(Box.createVerticalStrut(8));
        globalSectionPanel.add(createToggleRow(userProjectOverrideToggle));
        globalSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        globalSectionPanel.add(globalHelperLabel);
        globalSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
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
        projectSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        projectSectionPanel.add(createToggleRow(projectOverrideToggle));
        projectSectionPanel.add(Box.createVerticalStrut(8));
        projectSectionPanel.add(createFieldRow(projectField, projectBrowseButton, projectSaveButton));
        projectSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        projectSectionPanel.add(projectHelperLabel);
        projectSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        projectSectionPanel.add(projectFeedbackLabel);

        // Auth folder section backs the new context-menu Auth export action.
        authSectionPanel = createSectionPanel("authSection");
        JLabel authHeadingLabel = new JLabel();
        styleSectionLabel(authHeadingLabel);
        authField = createPathField(true);
        authBrowseButton = new JButton();
        authSaveButton = new JButton();
        authHelperLabel = new JLabel();
        styleMutedLabel(authHelperLabel, false);
        authFeedbackLabel = new JLabel();
        styleMutedLabel(authFeedbackLabel, false);
        authSectionPanel.add(authHeadingLabel);
        authSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        authSectionPanel.add(createFieldRow(authField, authBrowseButton, authSaveButton));
        authSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        authSectionPanel.add(authHelperLabel);
        authSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        authSectionPanel.add(authFeedbackLabel);

        // Backlog folder section backs the new context-menu Backlog export action.
        backlogSectionPanel = createSectionPanel("backlogSection");
        JLabel backlogHeadingLabel = new JLabel();
        styleSectionLabel(backlogHeadingLabel);
        backlogField = createPathField(true);
        backlogBrowseButton = new JButton();
        backlogSaveButton = new JButton();
        backlogHelperLabel = new JLabel();
        styleMutedLabel(backlogHelperLabel, false);
        backlogFeedbackLabel = new JLabel();
        styleMutedLabel(backlogFeedbackLabel, false);
        backlogSectionPanel.add(backlogHeadingLabel);
        backlogSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        backlogSectionPanel.add(createFieldRow(backlogField, backlogBrowseButton, backlogSaveButton));
        backlogSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        backlogSectionPanel.add(backlogHelperLabel);
        backlogSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        backlogSectionPanel.add(backlogFeedbackLabel);

        // Results section configures the folder and renders the newest markdown result as human-readable HTML.
        resultsSectionPanel = createSectionPanel("resultsSection");
        JLabel resultsHeadingLabel = new JLabel();
        styleSectionLabel(resultsHeadingLabel);
        resultsField = createPathField(true);
        resultsBrowseButton = new JButton();
        resultsSaveButton = new JButton();
        resultsRefreshButton = new JButton("Refresh");
        resultsReportSelector = new JComboBox<>();
        resultsHelperLabel = new JLabel();
        styleMutedLabel(resultsHelperLabel, false);
        resultsFeedbackLabel = new JLabel();
        styleMutedLabel(resultsFeedbackLabel, false);
        resultsStatusLabel = new JLabel();
        styleMutedLabel(resultsStatusLabel, true);
        // JEditorPane uses Swing's built-in HTML support, which keeps the extension dependency-free while rendering markdown output readably.
        resultsContentPane = new JEditorPane("text/html", "");
        resultsContentPane.setEditable(false);
        resultsContentPane.setOpaque(true);
        resultsContentPane.setBackground(defaultColor("TextArea.background", Color.WHITE));
        JScrollPane resultsScrollPane = new JScrollPane(resultsContentPane);
        resultsScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultsScrollPane.setPreferredSize(new Dimension(CONTENT_WIDTH_FLOOR, 280));
        resultsSectionPanel.add(resultsHeadingLabel);
        resultsSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        resultsSectionPanel.add(createFieldRow(resultsField, resultsBrowseButton, resultsSaveButton, resultsRefreshButton));
        resultsSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        resultsSectionPanel.add(resultsHelperLabel);
        resultsSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        resultsSectionPanel.add(resultsFeedbackLabel);
        resultsSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        resultsSectionPanel.add(resultsStatusLabel);
        resultsSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        // Report selector appears before the rendered report so users can choose older reports without reading raw filenames from content.
        resultsSectionPanel.add(createReportSelectorRow(resultsReportSelector));
        resultsSectionPanel.add(Box.createVerticalStrut(TITLE_GAP));
        resultsSectionPanel.add(resultsScrollPane);

        addSection(contentPanel, titleBlock, false);
        addSection(contentPanel, effectiveSummary, true);
        addSection(contentPanel, authSectionPanel, true);
        addSection(contentPanel, backlogSectionPanel, true);
        addSection(contentPanel, globalSectionPanel, true);
        addSection(contentPanel, projectSectionPanel, true);
        addSection(contentPanel, resultsSectionPanel, true);
        // The settings stack is taller than a typical Burp tab, so wrap it in one outer scroll pane to keep the Results viewer reachable.
        JScrollPane contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(contentScrollPane, BorderLayout.CENTER);

        userSettingTabButton.addActionListener(event -> applyViewState(controller.showUserSettings(), false, false));
        projectSettingTabButton.addActionListener(event -> applyViewState(controller.showProjectSettings(), false, false));
        userProjectOverrideToggle.addActionListener(event -> handleProjectOverrideToggle(userProjectOverrideToggle.isSelected()));
        authBrowseButton.addActionListener(event -> chooseFolder(authField));
        backlogBrowseButton.addActionListener(event -> chooseFolder(backlogField));
        resultsBrowseButton.addActionListener(event -> chooseFolder(resultsField));
        // Manual refresh reloads the newest markdown result without changing any configured folder.
        resultsRefreshButton.addActionListener(event -> refreshResultsView());
        resultsReportSelector.addActionListener(event -> selectResultReport());
        globalBrowseButton.addActionListener(event -> chooseFolder(globalField));
        projectBrowseButton.addActionListener(event -> chooseAndSaveFolder(projectField, controller::saveProjectOverride));
        authSaveButton.addActionListener(event -> applyResult(controller.saveAuthFolder(authField.getText())));
        backlogSaveButton.addActionListener(event -> applyResult(controller.saveBacklogFolder(backlogField.getText())));
        resultsSaveButton.addActionListener(event -> applyResult(controller.saveResultsFolder(resultsField.getText())));
        globalSaveButton.addActionListener(event -> applyResult(controller.saveGlobalFolder(globalField.getText())));
        projectSaveButton.addActionListener(event -> applyResult(controller.saveProjectOverride(projectField.getText())));
        projectOverrideToggle.addActionListener(event -> handleProjectOverrideToggle(projectOverrideToggle.isSelected()));

        FolderSettingsViewState initialState = controller.loadViewState();
        titleLabel.setText(initialState.title());
        introLabel.setText(initialState.introText());
        summaryLabel.setText(initialState.summaryLabel());
        globalHeadingLabel.setText(initialState.globalSection().heading());
        projectHeadingLabel.setText(initialState.projectSection().heading());
        authHeadingLabel.setText(initialState.authSection().heading());
        backlogHeadingLabel.setText(initialState.backlogSection().heading());
        resultsHeadingLabel.setText(initialState.resultsSection().heading());

        applyViewState(initialState, true, true);
        projectField.setText(initialState.projectSection().fieldValue());
        projectField.setCaretPosition(0);
        // Live refresh runs only while the tab is actually visible, avoiding background filesystem polling during tests or hidden Burp tabs.
        resultsAutoRefreshTimer = new Timer(RESULTS_AUTO_REFRESH_INTERVAL_MILLIS, event -> refreshResultsView());
        resultsAutoRefreshTimer.setRepeats(true);
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (panel.isShowing()) {
                resultsAutoRefreshTimer.start();
            } else {
                resultsAutoRefreshTimer.stop();
            }
        });
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

    // Results refresh preserves typed but unsaved legacy Global/Project fields while reloading the configured results folder.
    private void refreshResultsView() {
        applyViewState(controller.loadViewState(), false, false);
    }

    // Dropdown selection reloads the selected report content while the live refresh keeps the same report selected when present.
    private void selectResultReport() {
        if (applyingViewState) {
            return;
        }
        Object selectedItem = resultsReportSelector.getSelectedItem();
        if (selectedItem instanceof ResultReportOption option) {
            applyViewState(controller.selectResultsFile(option.fileName()), false, false);
        }
    }

    private void applyResult(FolderSaveResult result) {
        // Global and Backlog share one persisted slot, so either save must refresh both visible fields.
        boolean refreshGlobalField = (result.scope() == FolderSettingsController.Scope.GLOBAL
                || result.scope() == FolderSettingsController.Scope.BACKLOG) && result.success();
        boolean refreshProjectField = result.scope() == FolderSettingsController.Scope.PROJECT && result.success();
        // New folder save actions return the updated field values through the shared view state.
        boolean refreshAuthField = result.scope() == FolderSettingsController.Scope.AUTH && result.success();
        boolean refreshBacklogField = (result.scope() == FolderSettingsController.Scope.BACKLOG
                || result.scope() == FolderSettingsController.Scope.GLOBAL) && result.success();
        boolean refreshResultsField = result.scope() == FolderSettingsController.Scope.RESULTS && result.success();
        applyViewState(result.viewState(), refreshGlobalField, refreshProjectField);
        applySectionState(result.viewState().authSection(), null, authField, authBrowseButton, authSaveButton, authHelperLabel, authFeedbackLabel, refreshAuthField);
        applySectionState(result.viewState().backlogSection(), null, backlogField, backlogBrowseButton, backlogSaveButton, backlogHelperLabel, backlogFeedbackLabel, refreshBacklogField);
        applySectionState(result.viewState().resultsSection(), null, resultsField, resultsBrowseButton, resultsSaveButton, resultsHelperLabel, resultsFeedbackLabel, refreshResultsField);
    }

    private void applyViewState(FolderSettingsViewState state, boolean refreshGlobalField, boolean refreshProjectField) {
        applyingViewState = true;
        try {
            summaryPathField.setText(state.summaryFolderPath());
            summaryPathField.setCaretPosition(0);
            summarySourceLabel.setText(state.summarySourceLabel());
            applyActiveMode(state.activeMode());
            applySectionState(state.globalSection(), userProjectOverrideToggle, globalField, globalBrowseButton, globalSaveButton, globalHelperLabel, globalFeedbackLabel, refreshGlobalField);
            applySectionState(state.projectSection(), projectOverrideToggle, projectField, projectBrowseButton, projectSaveButton, projectHelperLabel, projectFeedbackLabel, refreshProjectField);
            // New folder sections refresh on every view rebuild so manual results refresh and tab switches cannot show stale paths.
            applySectionState(state.authSection(), null, authField, authBrowseButton, authSaveButton, authHelperLabel, authFeedbackLabel, true);
            applySectionState(state.backlogSection(), null, backlogField, backlogBrowseButton, backlogSaveButton, backlogHelperLabel, backlogFeedbackLabel, true);
            applySectionState(state.resultsSection(), null, resultsField, resultsBrowseButton, resultsSaveButton, resultsHelperLabel, resultsFeedbackLabel, true);
            resultsStatusLabel.setText(state.resultsStatusMessage());
            applyResultReportOptions(state.resultReports(), state.selectedResultFileName());
            resultsContentPane.setText(markdownHtmlRenderer.render(state.resultsContent()));
            resultsContentPane.setCaretPosition(0);
        } finally {
            applyingViewState = false;
        }
    }

    // Rebuilds the report dropdown from newest-first summaries while preserving the selected file name.
    private void applyResultReportOptions(java.util.List<ResultFileSummary> reports, String selectedFileName) {
        DefaultComboBoxModel<ResultReportOption> model = new DefaultComboBoxModel<>();
        for (ResultFileSummary report : reports) {
            model.addElement(new ResultReportOption(report.fileName(), report.displayLabel()));
        }
        resultsReportSelector.setModel(model);
        resultsReportSelector.setEnabled(!reports.isEmpty());
        for (int index = 0; index < model.getSize(); index++) {
            ResultReportOption option = model.getElementAt(index);
            if (option.fileName().equals(selectedFileName)) {
                resultsReportSelector.setSelectedIndex(index);
                return;
            }
        }
        if (model.getSize() > 0) {
            resultsReportSelector.setSelectedIndex(0);
        }
    }

    private void applyActiveMode(FolderSettingsViewState.ActiveMode activeMode) {
        // Only the legacy project override mode uses the project panel; all new folder modes stay on user settings.
        boolean userSettingActive = activeMode != FolderSettingsViewState.ActiveMode.PROJECT_SETTING;
        globalSectionPanel.setVisible(userSettingActive);
        // Auth, Backlog, and Results are user-configured folders, so they live on the user settings side.
        authSectionPanel.setVisible(userSettingActive);
        backlogSectionPanel.setVisible(userSettingActive);
        resultsSectionPanel.setVisible(userSettingActive);
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
        button.putClientProperty("b2auco.tab.active", active);
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
        button.putClientProperty("b2auco.tab.active", false);
        return button;
    }

    private static JPanel createTabSelectorPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        return panel;
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

    // Report selector row keeps the dropdown full-width while matching the compact field-row layout.
    private static JPanel createReportSelectorRow(JComboBox<ResultReportOption> selector) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setOpaque(false);
        selector.setPreferredSize(new Dimension(CONTENT_WIDTH_FLOOR, selector.getPreferredSize().height));
        row.add(selector);
        return row;
    }

    // Dropdown option keeps the stable file name separate from the human-readable created-time label.
    private record ResultReportOption(String fileName, String label) {
        @Override
        public String toString() {
            return label;
        }
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
        panel.setBorder(createSectionBorder(SECTION_PADDING));
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

    private static Border createSectionBorder(int padding) {
        return BorderFactory.createCompoundBorder(
                defaultBorder("TitledBorder.border"),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding)
        );
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
