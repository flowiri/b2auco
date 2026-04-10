package com.example.b2auco.settings;

import com.example.b2auco.location.OutputDirectoryResolver;
import org.junit.jupiter.api.Test;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertEquals(Path.of("C:/work/project-exports").toString(), state.projectSection().fieldValue());
        assertEquals("From project override", state.summarySourceLabel());
    }

    @Test
    void enablingProjectOverrideImmediatelySelectsToggleAndEnablesControlsBeforeSave() {
        InMemoryFolderSettingsStore store = new InMemoryFolderSettingsStore();
        Path projectIdentity = Path.of("C:/work/project.burp");
        store.saveGlobalDefault(Path.of("C:/work/global-exports"));
        FolderSettingsController controller = new FolderSettingsController(
                store,
                new EffectiveFolderResolver(store, new OutputDirectoryResolver()),
                () -> Optional.of(projectIdentity)
        );

        FolderSettingsViewState state = controller.setProjectOverrideEnabled(true);

        assertTrue(state.projectSection().toggleSelected());
        assertTrue(state.projectSection().controlsEnabled());
        assertEquals("", state.projectSection().fieldValue());
        assertEquals(Path.of("C:/work/global-exports").toString(), state.summaryFolderPath());
        assertEquals("From global default", state.summarySourceLabel());
        assertTrue(store.findCurrentProjectOverride().isEmpty());
    }

    @Test
    void turningProjectOverrideOffRemovesStoredOverrideAndRefreshesSummary() {
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

        assertTrue(store.findCurrentProjectOverride().isEmpty());
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

        assertTrue(projectResult.success());
        assertEquals("Folder saved.", projectResult.message());
        assertEquals(Path.of("C:/work/project"), Path.of(projectResult.viewState().summaryFolderPath()));
        assertEquals("From project override", projectResult.viewState().summarySourceLabel());
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
    void bothFolderSectionsExposeCompactFieldRowsAndProjectToggle() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertCompactSectionStructure(tab.globalSectionPanel(), false, "Used for all exports unless the current Burp project has its own override.");
        assertCompactSectionStructure(tab.projectSectionPanel(), true, "Used only for this Burp project file and overrides the global folder.");
        assertEquals("Override folder for this project only", tab.projectOverrideToggle().getText());
        assertTrue(tab.projectOverrideToggle().isSelected());
    }

    @Test
    void disabledProjectSectionKeepsToggleVisibleAndDisablesControls() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.disabledProjectState()));

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
    void toggleOnImmediatelyEnablesProjectControlsInTab() {
        FolderSettingsController controller = new FolderSettingsController(
                new InMemoryFolderSettingsStore(),
                new EffectiveFolderResolver(new InMemoryFolderSettingsStore(), new OutputDirectoryResolver()),
                () -> Optional.of(Path.of("C:/work/project.burp"))
        );
        FolderSettingsTab tab = new FolderSettingsTab(controller);

        tab.projectOverrideToggle().doClick();

        assertTrue(tab.projectOverrideToggle().isSelected());
        assertTrue(tab.projectField().isEnabled());
        assertTrue(tab.projectBrowseButton().isEnabled());
        assertTrue(tab.projectSaveButton().isEnabled());
    }

    @Test
    void toggleOffImmediatelyRemovesOverrideAndRefreshesSummaryInTab() {
        ToggleController controller = new ToggleController();
        FolderSettingsTab tab = new FolderSettingsTab(controller);

        tab.projectOverrideToggle().doClick();

        assertFalse(controller.lastToggleValue);
        assertFalse(tab.projectOverrideToggle().isSelected());
        assertFalse(tab.projectField().isEnabled());
        assertEquals("C:/global/exports", tab.summaryPathField().getText());
        assertEquals("From global default", tab.summarySourceLabel().getText());
        assertEquals("Project override removed.", tab.projectFeedbackLabel().getText());
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
    void projectBrowseImmediatelyPersistsOverrideAndRefreshesSummary() {
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
        assertTrue(originalPanel == tab.panel());
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
                    new FolderSettingsViewState.SectionState(
                            "Global default folder",
                            "C:/global/exports",
                            "Used for all exports unless the current Burp project has its own override.",
                            "Save",
                            "Browse…",
                            "",
                            false,
                            false,
                            false,
                            true,
                            ""
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
                            ""
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
                    new FolderSettingsViewState.SectionState(
                            "Global default folder",
                            "C:/global/exports",
                            "Used for all exports unless the current Burp project has its own override.",
                            "Save",
                            "Browse…",
                            "",
                            false,
                            false,
                            false,
                            true,
                            ""
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
                            feedbackMessage
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
                            new FolderSettingsViewState.SectionState(
                                    "Global default folder",
                                    folderInput,
                                    "Used for all exports unless the current Burp project has its own override.",
                                    "Save",
                                    "Browse…",
                                    "",
                                    false,
                                    false,
                                    false,
                                    true,
                                    "Folder saved."
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
                                    ""
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
            return FolderSettingsFixtures.enabledState();
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
        public void saveCurrentProjectOverride(Path folderPath) {
            currentProjectOverride = Optional.of(folderPath.normalize());
        }

        @Override
        public void clearCurrentProjectOverride() {
            currentProjectOverride = Optional.empty();
        }
    }
}
