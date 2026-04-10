package com.example.b2auco.settings;

import com.example.b2auco.location.OutputDirectoryResolver;
import org.junit.jupiter.api.Test;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderSettingsTabTest {
    @Test
    void loadViewStateIncludesApprovedTitleSummaryAndSourceCopy() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.empty()
        );

        FolderSettingsViewState state = controller.loadViewState();

        assertEquals("Export folders", state.title());
        assertEquals(
                "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.",
                state.introText()
        );
        assertEquals("Current export folder", state.summaryLabel());
        assertEquals("From fallback default", state.summarySourceLabel());
        assertEquals(FolderSettingsViewState.ActiveMode.USER_SETTING, state.activeMode());
        assertEquals("Global default folder", state.globalSection().heading());
        assertEquals("Current project override", state.projectSection().heading());
    }

    @Test
    void loadViewStateShowsProjectOverrideToggleDisabledWhenNoProjectFileIdentityExists() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                Optional::<Path>empty
        );

        FolderSettingsViewState state = controller.loadViewState();

        assertTrue(state.projectSection().toggleVisible());
        assertFalse(state.projectSection().toggleEnabled());
        assertFalse(state.projectSection().toggleSelected());
        assertFalse(state.projectSection().controlsEnabled());
        assertEquals(
                "Open or save a Burp project file to set a project-specific override.",
                state.projectSection().helperText()
        );
    }

    @Test
    void loadViewStateLeavesProjectControlsDisabledUntilOverrideIsEnabled() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        Path projectIdentity = Path.of(".b2auco-project-id", "project-id-token");
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(projectIdentity)
        );

        FolderSettingsViewState state = controller.loadViewState();

        assertTrue(state.projectSection().toggleEnabled());
        assertFalse(state.projectSection().toggleSelected());
        assertFalse(state.projectSection().controlsEnabled());
        assertEquals(
                "Enable the override to save a project-specific folder for this Burp project file.",
                state.projectSection().helperText()
        );
    }

    @Test
    void loadViewStateLoadsToggleAsSelectedWhenProjectOverrideAlreadyExists() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        Path projectIdentity = Path.of("C:/work/project.burp");
        store.saveCurrentProjectOverride(Path.of("C:/work/project-exports"));
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(projectIdentity)
        );

        FolderSettingsViewState state = controller.loadViewState();

        assertTrue(state.projectSection().toggleSelected());
        assertTrue(state.projectSection().controlsEnabled());
        assertEquals(FolderSettingsViewState.ActiveMode.USER_SETTING, state.activeMode());
        assertEquals(Path.of("C:/work/project-exports").toString(), state.projectSection().fieldValue());
        assertEquals("From project override", state.summarySourceLabel());
    }

    @Test
    void enablingProjectOverrideImmediatelySelectsToggleEnablesControlsAndSwitchesActiveMode() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        Path projectIdentity = Path.of("C:/work/project.burp");
        store.saveGlobalDefault(Path.of("C:/work/global-exports"));
        store.saveCurrentProjectOverride(Path.of("C:/work/project-exports"));
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(projectIdentity)
        );

        FolderSettingsViewState state = controller.setProjectOverrideEnabled(true);

        assertTrue(state.projectSection().toggleSelected());
        assertTrue(state.projectSection().controlsEnabled());
        assertEquals(FolderSettingsViewState.ActiveMode.PROJECT_SETTING, state.activeMode());
        assertEquals(Path.of("C:/work/project-exports").toString(), state.projectSection().fieldValue());
        assertEquals(Path.of("C:/work/project-exports").toString(), state.summaryFolderPath());
        assertEquals("From project override", state.summarySourceLabel());
    }

    @Test
    void turningProjectOverrideOffRemovesStoredOverrideRefreshesSummaryAndSwitchesActiveMode() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        Path projectIdentity = Path.of("C:/work/project.burp");
        store.saveGlobalDefault(Path.of("C:/work/global-exports"));
        store.saveCurrentProjectOverride(Path.of("C:/work/project-exports"));
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(projectIdentity)
        );

        FolderSettingsViewState state = controller.setProjectOverrideEnabled(false);

        assertEquals(Path.of("C:/work/project-exports"), store.findCurrentProjectOverride().orElseThrow());
        assertTrue(!store.isCurrentProjectOverrideEnabled());
        assertEquals(FolderSettingsViewState.ActiveMode.USER_SETTING, state.activeMode());
        assertFalse(state.projectSection().toggleSelected());
        assertFalse(state.projectSection().controlsEnabled());
        assertEquals(Path.of("C:/work/global-exports").toString(), state.summaryFolderPath());
        assertEquals("From global default", state.summarySourceLabel());
        assertEquals("Project override removed.", state.projectSection().feedbackMessage());
    }

    @Test
    void saveGlobalFolderReturnsInlineValidationForBlankFolder() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                Optional::<Path>empty
        );

        FolderSaveResult result = controller.saveGlobalFolder("   ");

        assertFalse(result.success());
        assertEquals("Choose a folder before saving.", result.message());
        assertEquals(FolderSettingsController.Scope.GLOBAL, result.scope());
    }

    @Test
    void saveFolderReturnsExactInlineMessagesForInvalidAndUnwritablePaths() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(Path.of("C:/work/project.burp")),
                candidate -> !candidate.toString().contains("locked")
        );

        FolderSaveResult invalidResult = controller.saveGlobalFolder("invalid::path");
        FolderSaveResult unwritableResult = controller.saveProjectOverride("C:/locked/folder");

        assertFalse(invalidResult.success());
        assertEquals("Enter a valid folder path.", invalidResult.message());
        assertFalse(unwritableResult.success());
        assertEquals("This folder isn’t writable. Choose another location.", unwritableResult.message());
    }

    @Test
    void successfulSaveRefreshesEffectiveFolderSummaryAndReturnsSavedMessage() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(Path.of("C:/work/project.burp"))
        );

        FolderSaveResult globalResult = controller.saveGlobalFolder("C:/work/global");
        controller.setProjectOverrideEnabled(true);
        FolderSaveResult projectResult = controller.saveProjectOverride("C:/work/project");

        assertTrue(globalResult.success());
        assertEquals("Folder saved.", globalResult.message());
        assertEquals(Path.of("C:/work/global"), Path.of(globalResult.viewState().summaryFolderPath()));
        assertEquals("From global default", globalResult.viewState().summarySourceLabel());
        assertEquals(FolderSettingsViewState.ActiveMode.USER_SETTING, globalResult.viewState().activeMode());

        assertTrue(projectResult.success());
        assertEquals("Folder saved.", projectResult.message());
        assertEquals(Path.of("C:/work/project"), Path.of(projectResult.viewState().summaryFolderPath()));
        assertEquals("From project override", projectResult.viewState().summarySourceLabel());
        assertEquals(FolderSettingsViewState.ActiveMode.PROJECT_SETTING, projectResult.viewState().activeMode());
        assertEquals(Path.of("C:/work/global"), store.findGlobalDefault().orElseThrow());
    }

    @Test
    void tabRendersSectionsInApprovedOrder() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertInstanceOf(BorderLayout.class, tab.panel().getLayout());
        List<String> panelNames = new ArrayList<>();
        for (Component component : tab.contentPanel().getComponents()) {
            if (component instanceof JComponent child && child.getName() != null) {
                panelNames.add(child.getName());
            }
        }

        assertEquals(List.of("titleBlock", "effectiveSummary", "globalSection", "projectSection"), panelNames);
    }

    @Test
    void tabShowsUserAndProjectSettingTabsWithUserTabHighlightedByDefault() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertEquals("User setting", tab.userSettingTabButton().getText());
        assertEquals("Project setting", tab.projectSettingTabButton().getText());
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.USER_SETTING);
    }

    @Test
    void tabButtonsUseLookAndFeelAwareDefaultsAndReadableDistinctStates() {
        Border expectedActiveBorder = BorderFactory.createCompoundBorder(
                UIManager.getBorder("Button.border"),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        );
        Border expectedInactiveBorder = BorderFactory.createEmptyBorder(7, 13, 7, 13);
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        JButton activeButton = tab.userSettingTabButton();
        JButton inactiveButton = tab.projectSettingTabButton();

        assertSame(UIManager.getColor("Panel.background"), activeButton.getBackground());
        assertSame(UIManager.getColor("Label.foreground"), activeButton.getForeground());
        assertEquals(expectedActiveBorder.getClass(), activeButton.getBorder().getClass());

        assertSame(UIManager.getColor("Button.background"), inactiveButton.getBackground());
        assertSame(UIManager.getColor("Button.foreground"), inactiveButton.getForeground());
        assertEquals(expectedInactiveBorder.getClass(), inactiveButton.getBorder().getClass());

        assertNotEquals(activeButton.getBorder().getClass(), inactiveButton.getBorder().getClass());
        assertTrue(activeButton.getFont().isBold());
        assertFalse(inactiveButton.getFont().isBold());
    }

    @Test
    void layoutAddsThemeAwareHierarchyWithoutChangingLabels() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertTrue(tab.panel().getBorder() != null);
        assertTrue(tab.globalSectionPanel().getBorder() != null);
        assertTrue(tab.projectSectionPanel().getBorder() != null);
        assertTrue(tab.summarySourceLabel().getFont().isItalic());
        assertEquals("From project override", tab.summarySourceLabel().getText());
    }

    @Test
    void tabSwitchingReappliesReadableThemeAwareStateForBothButtons() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        tab.projectSettingTabButton().doClick();
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.PROJECT_SETTING);
        assertTrue(tab.projectSettingTabButton().getFont().isBold());
        assertFalse(tab.userSettingTabButton().getFont().isBold());
        assertNotEquals(tab.projectSettingTabButton().getBorder().getClass(), tab.userSettingTabButton().getBorder().getClass());

        tab.userSettingTabButton().doClick();
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.USER_SETTING);
        assertTrue(tab.userSettingTabButton().getFont().isBold());
        assertFalse(tab.projectSettingTabButton().getFont().isBold());
    }

    @Test
    void readableStylingSurvivesBehavioralInteractions() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(Path.of("C:/work/project.burp"))
        );
        FolderSettingsTab tab = new FolderSettingsTab(controller);

        tab.projectOverrideToggle().doClick();
        tab.projectField().setText("C:/work/project");
        tab.projectSaveButton().doClick();
        tab.userSettingTabButton().doClick();

        assertEquals("Folder saved.", tab.projectFeedbackLabel().getText());
        assertEquals(Path.of("C:/work/project").toString(), tab.summaryPathField().getText());
        assertEquals("From project override", tab.summarySourceLabel().getText());
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.USER_SETTING);
        assertTrue(tab.userSettingTabButton().getFont().isBold());
        assertFalse(tab.projectSettingTabButton().getFont().isBold());
    }

    @Test
    void bothFolderSectionsExposeCompactFieldRowsAndProjectToggle() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertCompactSectionStructure(tab.globalSectionPanel(), true, "Used for all exports unless the current Burp project has its own override.");
        assertCompactSectionStructure(tab.projectSectionPanel(), true, "Used only for this Burp project file and overrides the global folder.");
        assertEquals("Override folder for this project only", tab.userProjectOverrideToggle().getText());
        assertEquals("Override folder for this project only", tab.projectOverrideToggle().getText());
        assertTrue(tab.userProjectOverrideToggle().isSelected());
        assertTrue(tab.projectOverrideToggle().isSelected());
    }

    @Test
    void disabledProjectSectionKeepsToggleVisibleAndDisablesControls() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.disabledProjectState()));

        assertTrue(tab.userProjectOverrideToggle().isVisible());
        assertTrue(tab.userProjectOverrideToggle().isEnabled());
        assertFalse(tab.userProjectOverrideToggle().isSelected());
        assertTrue(tab.projectOverrideToggle().isVisible());
        assertTrue(tab.projectOverrideToggle().isEnabled());
        assertFalse(tab.projectOverrideToggle().isSelected());
        assertFalse(tab.projectField().isEnabled());
        assertFalse(tab.projectSaveButton().isEnabled());
        assertFalse(tab.projectBrowseButton().isEnabled());
        assertEquals(
                "Enable the override to save a project-specific folder for this Burp project file.",
                tab.projectHelperLabel().getText()
        );
    }

    @Test
    void manualTabSwitchingPreservesTypedValuesWithoutRebuildingPanel() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(Path.of("C:/work/project.burp"))
        );
        FolderSettingsTab tab = new FolderSettingsTab(controller);
        JPanel originalPanel = tab.panel();

        tab.globalField().setText("C:/typed/global");
        tab.projectOverrideToggle().doClick();
        tab.projectField().setText("C:/typed/project");
        tab.userSettingTabButton().doClick();
        tab.projectSettingTabButton().doClick();

        assertEquals("C:/typed/global", tab.globalField().getText());
        assertEquals("C:/typed/project", tab.projectField().getText());
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.PROJECT_SETTING);
        assertTrue(originalPanel == tab.panel());
    }

    @Test
    void toggleOnImmediatelyEnablesProjectControlsInTabAndSwitchesToProjectTab() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        store.saveGlobalDefault(Path.of("C:/work/global-exports"));
        store.saveCurrentProjectOverride(Path.of("C:/work/project-exports"));
        store.setCurrentProjectOverrideEnabled(false);
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(Path.of("C:/work/project.burp"))
        );
        FolderSettingsTab tab = new FolderSettingsTab(controller);

        assertFalse(tab.userProjectOverrideToggle().isSelected());
        assertFalse(store.isCurrentProjectOverrideEnabled());
        assertEquals(Path.of("C:/work/global-exports").toString(), tab.summaryPathField().getText());
        assertEquals("From global default", tab.summarySourceLabel().getText());

        tab.userProjectOverrideToggle().doClick();

        assertTrue(store.isCurrentProjectOverrideEnabled());
        assertTrue(tab.projectOverrideToggle().isSelected());
        assertTrue(tab.projectField().isEnabled());
        assertTrue(tab.projectBrowseButton().isEnabled());
        assertTrue(tab.projectSaveButton().isEnabled());
        assertEquals(Path.of("C:/work/project-exports").toString(), tab.summaryPathField().getText());
        assertEquals("From project override", tab.summarySourceLabel().getText());
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.PROJECT_SETTING);
    }

    @Test
    void toggleOffImmediatelyRemovesOverrideRefreshesSummaryAndSwitchesBackToUserTab() {
        ToggleController controller = new ToggleController();
        FolderSettingsTab tab = new FolderSettingsTab(controller);

        tab.projectOverrideToggle().doClick();

        assertFalse(controller.lastToggleValue);
        assertFalse(tab.projectOverrideToggle().isSelected());
        assertFalse(tab.projectField().isEnabled());
        assertEquals("C:/global/exports", tab.summaryPathField().getText());
        assertEquals("From global default", tab.summarySourceLabel().getText());
        assertEquals("Project override removed.", tab.projectFeedbackLabel().getText());
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.USER_SETTING);
    }

    @Test
    void browseSelectionUpdatesFieldAndCancelLeavesItUnchanged() {
        FolderSettingsTab tab = new FolderSettingsTab(
                new FakeController(FolderSettingsFixtures.enabledState()),
                new SequencedFolderChooser(Optional.of(Path.of("C:/selected/global")), Optional.empty())
        );

        tab.globalField().setText("C:/starting/global");
        tab.globalBrowseButton().doClick();
        assertEquals(Path.of("C:/selected/global").toString(), tab.globalField().getText());

        tab.globalBrowseButton().doClick();
        assertEquals(Path.of("C:/selected/global").toString(), tab.globalField().getText());
    }

    @Test
    void projectBrowseImmediatelyPersistsOverrideRefreshesSummaryAndKeepsProjectTabActive() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        Path projectIdentity = Path.of("C:/work/project.burp");
        store.saveGlobalDefault(Path.of("C:/work/global-exports"));
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(projectIdentity)
        );
        FolderSettingsTab tab = new FolderSettingsTab(
                controller,
                new SequencedFolderChooser(Optional.of(Path.of("C:/work/project-override")))
        );

        tab.projectOverrideToggle().doClick();
        tab.projectBrowseButton().doClick();

        assertEquals(Path.of("C:/work/project-override"), store.findCurrentProjectOverride().orElseThrow());
        assertEquals(Path.of("C:/work/project-override").toString(), tab.summaryPathField().getText());
        assertEquals("From project override", tab.summarySourceLabel().getText());
        assertEquals("Folder saved.", tab.projectFeedbackLabel().getText());
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.PROJECT_SETTING);
    }

    @Test
    void pathFieldsUseCompactSingleLineSizingAndSummaryIsReadOnly() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertCompactSingleLineField(tab.globalField(), true);
        assertCompactSingleLineField(tab.projectField(), true);
        assertCompactSingleLineField(tab.summaryPathField(), false);
    }

    @Test
    void pathRowsUseFlowLayoutToKeepControlsCompact() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertInstanceOf(FlowLayout.class, findAll(tab.globalSectionPanel(), JPanel.class).get(0).getLayout());
        assertInstanceOf(FlowLayout.class, findAll(tab.projectSectionPanel(), JPanel.class).get(0).getLayout());
        assertInstanceOf(FlowLayout.class, findAll(tab.projectSectionPanel(), JPanel.class).get(1).getLayout());
    }

    @Test
    void tabAnchorsContentAtTopWithCompactStackedSections() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertInstanceOf(BorderLayout.class, tab.panel().getLayout());
        assertInstanceOf(BoxLayout.class, tab.contentPanel().getLayout());
        assertEquals(Component.LEFT_ALIGNMENT, tab.contentPanel().getAlignmentX());
        assertTrue(tab.contentPanel().getComponentCount() > 4);
        assertEquals("titleBlock", assertInstanceOf(JComponent.class, tab.contentPanel().getComponent(0)).getName());
        assertEquals("effectiveSummary", assertInstanceOf(JComponent.class, tab.contentPanel().getComponent(2)).getName());
        assertEquals("globalSection", assertInstanceOf(JComponent.class, tab.contentPanel().getComponent(4)).getName());
        assertEquals("projectSection", assertInstanceOf(JComponent.class, tab.contentPanel().getComponent(6)).getName());
    }

    @Test
    void successfulSaveUpdatesSummaryWithoutRebuildingTab() {
        SequencedController controller = new SequencedController();
        FolderSettingsTab tab = new FolderSettingsTab(controller);
        JPanel originalPanel = tab.panel();

        tab.globalField().setText("C:/updated/global");
        tab.globalSaveButton().doClick();

        assertTrue(controller.globalSaveCalled);
        assertEquals("C:/updated/global", tab.summaryPathField().getText());
        assertEquals(0, tab.summaryPathField().getCaretPosition());
        assertEquals("From global default", tab.summarySourceLabel().getText());
        assertEquals("Folder saved.", tab.globalFeedbackLabel().getText());
        assertTabSelection(tab, FolderSettingsViewState.ActiveMode.USER_SETTING);
        assertTrue(originalPanel == tab.panel());
    }

    private static void assertTabSelection(FolderSettingsTab tab, FolderSettingsViewState.ActiveMode activeMode) {
        boolean userActive = activeMode == FolderSettingsViewState.ActiveMode.USER_SETTING;
        JButton userButton = tab.userSettingTabButton();
        JButton projectButton = tab.projectSettingTabButton();

        assertEquals(userActive, tab.globalSectionPanel().isVisible());
        assertEquals(!userActive, tab.projectSectionPanel().isVisible());
        assertEquals(userActive ? UIManager.getColor("Panel.background") : UIManager.getColor("Button.background"), userButton.getBackground());
        assertEquals(userActive ? UIManager.getColor("Label.foreground") : UIManager.getColor("Button.foreground"), userButton.getForeground());
        assertEquals(userActive ? UIManager.getColor("Button.background") : UIManager.getColor("Panel.background"), projectButton.getBackground());
        assertEquals(userActive ? UIManager.getColor("Button.foreground") : UIManager.getColor("Label.foreground"), projectButton.getForeground());
    }

    private static void assertCompactSectionStructure(JPanel sectionPanel, boolean expectsToggle, String helperText) {
        List<JTextField> fields = findAll(sectionPanel, JTextField.class);
        List<JButton> buttons = findAll(sectionPanel, JButton.class);
        List<JLabel> labels = findAll(sectionPanel, JLabel.class);
        List<JCheckBox> toggles = findAll(sectionPanel, JCheckBox.class);

        assertEquals(1, fields.size());
        assertEquals(2, buttons.size());
        assertEquals(expectsToggle ? 1 : 0, toggles.size());
        assertTrue(buttons.stream().anyMatch(button -> button.getText().equals("Browse…")));
        assertTrue(buttons.stream().anyMatch(button -> button.getText().equals("Save")));
        assertTrue(labels.stream().anyMatch(label -> label.getText().equals(helperText)));
        assertTrue(labels.stream().anyMatch(label -> label.getText().isEmpty()));
    }

    private static void assertCompactSingleLineField(JTextField field, boolean editable) {
        Dimension preferredSize = field.getPreferredSize();
        Dimension maximumSize = field.getMaximumSize();
        Dimension minimumSize = field.getMinimumSize();

        assertNotNull(preferredSize);
        assertNotNull(maximumSize);
        assertNotNull(minimumSize);
        assertEquals(preferredSize.height, maximumSize.height);
        assertEquals(preferredSize.height, minimumSize.height);
        assertTrue(preferredSize.height > 0);
        assertTrue(maximumSize.width >= preferredSize.width);
        assertEquals(editable, field.isEditable());
    }

    private static <T> List<T> findAll(JPanel panel, Class<T> type) {
        List<T> results = new ArrayList<>();
        collectAll(panel, type, results);
        return results;
    }

    private static <T> void collectAll(java.awt.Container container, Class<T> type, List<T> results) {
        for (java.awt.Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                results.add(type.cast(component));
            }
            if (component instanceof java.awt.Container childContainer) {
                collectAll(childContainer, type, results);
            }
        }
    }

    private static final class FolderSettingsFixtures {
        private static FolderSettingsViewState enabledState() {
            return new FolderSettingsViewState(
                    "Export folders",
                    "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.",
                    "Current export folder",
                    "C:/project/exports",
                    "From project override",
                    FolderSettingsViewState.ActiveMode.USER_SETTING,
                    new FolderSettingsViewState.SectionState(
                            "Global default folder",
                            "C:/global/exports",
                            "Used for all exports unless the current Burp project has its own override.",
                            "Save",
                            "Browse…",
                            "Override folder for this project only",
                            true,
                            true,
                            true,
                            true,
                            "",
                            true
                    ),
                    new FolderSettingsViewState.SectionState(
                            "Current project override",
                            "C:/project/exports",
                            "Used only for this Burp project file and overrides the global folder.",
                            "Save",
                            "Browse…",
                            "Override folder for this project only",
                            true,
                            true,
                            true,
                            true,
                            "",
                            false
                    )
            );
        }

        private static FolderSettingsViewState disabledProjectState() {
            return disabledProjectState("");
        }

        private static FolderSettingsViewState disabledProjectState(String feedbackMessage) {
            return new FolderSettingsViewState(
                    "Export folders",
                    "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.",
                    "Current export folder",
                    "C:/global/exports",
                    "From global default",
                    FolderSettingsViewState.ActiveMode.USER_SETTING,
                    new FolderSettingsViewState.SectionState(
                            "Global default folder",
                            "C:/global/exports",
                            "Used for all exports unless the current Burp project has its own override.",
                            "Save",
                            "Browse…",
                            "Override folder for this project only",
                            true,
                            true,
                            false,
                            true,
                            "",
                            true
                    ),
                    new FolderSettingsViewState.SectionState(
                            "Current project override",
                            "",
                            "Enable the override to save a project-specific folder for this Burp project file.",
                            "Save",
                            "Browse…",
                            "Override folder for this project only",
                            true,
                            true,
                            false,
                            false,
                            feedbackMessage,
                            false
                    )
            );
        }
    }

    private static class FakeController extends FolderSettingsController {
        private final FolderSettingsViewState state;

        private FakeController(FolderSettingsViewState state) {
            super(new InMemoryFolderSettingsStore(), new EffectiveFolderResolver(new InMemoryFolderSettingsStore(), new OutputDirectoryResolver()), Optional::<Path>empty);
            this.state = state;
        }

        @Override
        public FolderSettingsViewState loadViewState() {
            return state;
        }

        @Override
        public FolderSettingsViewState showUserSettings() {
            return new FolderSettingsViewState(
                    state.title(),
                    state.introText(),
                    state.summaryLabel(),
                    state.summaryFolderPath(),
                    state.summarySourceLabel(),
                    FolderSettingsViewState.ActiveMode.USER_SETTING,
                    state.globalSection(),
                    state.projectSection()
            );
        }

        @Override
        public FolderSettingsViewState showProjectSettings() {
            return new FolderSettingsViewState(
                    state.title(),
                    state.introText(),
                    state.summaryLabel(),
                    state.summaryFolderPath(),
                    state.summarySourceLabel(),
                    FolderSettingsViewState.ActiveMode.PROJECT_SETTING,
                    state.globalSection(),
                    state.projectSection()
            );
        }
    }

    private static final class SequencedController extends FolderSettingsController {
        private boolean globalSaveCalled;

        private SequencedController() {
            super(new InMemoryFolderSettingsStore(), new EffectiveFolderResolver(new InMemoryFolderSettingsStore(), new OutputDirectoryResolver()), Optional::<Path>empty);
        }

        @Override
        public FolderSettingsViewState loadViewState() {
            return FolderSettingsFixtures.enabledState();
        }

        @Override
        public FolderSaveResult saveGlobalFolder(String folderInput) {
            globalSaveCalled = true;
            return new FolderSaveResult(
                    Scope.GLOBAL,
                    true,
                    "Folder saved.",
                    new FolderSettingsViewState(
                            "Export folders",
                            "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.",
                            "Current export folder",
                            folderInput,
                            "From global default",
                            FolderSettingsViewState.ActiveMode.USER_SETTING,
                            new FolderSettingsViewState.SectionState(
                                    "Global default folder",
                                    folderInput,
                                    "Used for all exports unless the current Burp project has its own override.",
                                    "Save",
                                    "Browse…",
                                    "Override folder for this project only",
                                    true,
                                    true,
                                    false,
                                    true,
                                    "Folder saved.",
                                    true
                            ),
                            new FolderSettingsViewState.SectionState(
                                    "Current project override",
                                    "",
                                    "Enable the override to save a project-specific folder for this Burp project file.",
                                    "Save",
                                    "Browse…",
                                    "Override folder for this project only",
                                    true,
                                    true,
                                    false,
                                    false,
                                    "",
                                    false
                            )
                    )
            );
        }
    }

    private static final class ToggleController extends FolderSettingsController {
        private boolean lastToggleValue = true;

        private ToggleController() {
            super(new InMemoryFolderSettingsStore(), new EffectiveFolderResolver(new InMemoryFolderSettingsStore(), new OutputDirectoryResolver()), Optional::<Path>empty);
        }

        @Override
        public FolderSettingsViewState loadViewState() {
            return new FolderSettingsViewState(
                    "Export folders",
                    "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.",
                    "Current export folder",
                    "C:/project/exports",
                    "From project override",
                    FolderSettingsViewState.ActiveMode.PROJECT_SETTING,
                    FolderSettingsFixtures.enabledState().globalSection(),
                    FolderSettingsFixtures.enabledState().projectSection()
            );
        }

        @Override
        public FolderSettingsViewState setProjectOverrideEnabled(boolean enabled) {
            lastToggleValue = enabled;
            return FolderSettingsFixtures.disabledProjectState("Project override removed.");
        }
    }

    private static final class SequencedFolderChooser implements java.util.function.Function<String, Optional<Path>> {
        private final ArrayDeque<Optional<Path>> results;

        private SequencedFolderChooser(Optional<Path>... results) {
            this.results = new ArrayDeque<>(List.of(results));
        }

        @Override
        public Optional<Path> apply(String currentValue) {
            return results.isEmpty() ? Optional.empty() : results.removeFirst();
        }
    }

    private static final class InMemoryFolderSettingsStore implements FolderSettingsStore {
        private Optional<Path> globalDefault = Optional.empty();
        private Optional<Path> currentProjectOverride = Optional.empty();
        private boolean currentProjectOverrideEnabled = true;

        @Override
        public Optional<Path> findGlobalDefault() {
            return globalDefault;
        }

        @Override
        public void saveGlobalDefault(Path folderPath) {
            globalDefault = Optional.of(folderPath.normalize());
        }

        @Override
        public Optional<Path> findCurrentProjectOverride() {
            return currentProjectOverride;
        }

        @Override
        public boolean isCurrentProjectOverrideEnabled() {
            return currentProjectOverride.isPresent() && currentProjectOverrideEnabled;
        }

        @Override
        public void saveCurrentProjectOverride(Path folderPath) {
            currentProjectOverride = Optional.of(folderPath.normalize());
            currentProjectOverrideEnabled = true;
        }

        @Override
        public void setCurrentProjectOverrideEnabled(boolean enabled) {
            currentProjectOverrideEnabled = enabled;
        }

        @Override
        public void clearCurrentProjectOverride() {
            currentProjectOverride = Optional.empty();
            currentProjectOverrideEnabled = false;
        }
    }
}
