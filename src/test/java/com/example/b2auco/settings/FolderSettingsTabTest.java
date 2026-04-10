package com.example.b2auco.settings;

import com.example.b2auco.location.OutputDirectoryResolver;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderSettingsTabTest {
    @Test
    void loadViewStateIncludesApprovedTitleSummaryAndSourceCopy() {
        FolderSettingsController controller = new FolderSettingsController(
                new InMemoryFolderSettingsStore(),
                new EffectiveFolderResolver(new InMemoryFolderSettingsStore(), new OutputDirectoryResolver()),
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
    void loadViewStateDisablesProjectSectionWhenNoProjectFileIdentityExists() {
        FolderSettingsController controller = new FolderSettingsController(
                new InMemoryFolderSettingsStore(),
                new EffectiveFolderResolver(new InMemoryFolderSettingsStore(), new OutputDirectoryResolver()),
                Optional::<Path>empty
        );

        FolderSettingsViewState state = controller.loadViewState();

        assertFalse(state.projectSection().enabled());
        assertEquals(
                "Open or save a Burp project file to set a project-specific override.",
                state.projectSection().helperText()
        );
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

        List<String> panelNames = new ArrayList<>();
        for (java.awt.Component component : tab.panel().getComponents()) {
            panelNames.add(assertInstanceOf(JComponent.class, component).getName());
        }

        assertEquals(List.of("titleBlock", "effectiveSummary", "globalSection", "projectSection"), panelNames);
    }

    @Test
    void bothFolderSectionsExposeFieldBrowseSaveHelperAndInlineFeedbackComponents() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.enabledState()));

        assertSectionStructure(tab.globalSectionPanel(), "Used for all exports unless the current Burp project has its own override.");
        assertSectionStructure(tab.projectSectionPanel(), "Used only for this Burp project file and overrides the global folder.");
    }

    @Test
    void disabledProjectSectionKeepsControlsVisibleButDisabled() {
        FolderSettingsTab tab = new FolderSettingsTab(new FakeController(FolderSettingsFixtures.disabledProjectState()));

        assertFalse(tab.projectField().isEnabled());
        assertFalse(tab.projectSaveButton().isEnabled());
        assertFalse(tab.projectBrowseButton().isEnabled());
        assertEquals(
                "Open or save a Burp project file to set a project-specific override.",
                tab.projectHelperLabel().getText()
        );
    }

    @Test
    void successfulSaveUpdatesSummaryWithoutRebuildingTab() {
        SequencedController controller = new SequencedController();
        FolderSettingsTab tab = new FolderSettingsTab(controller);
        JPanel originalPanel = tab.panel();

        tab.globalField().setText("C:/updated/global");
        tab.globalSaveButton().doClick();

        assertTrue(controller.globalSaveCalled);
        assertEquals("C:/updated/global", tab.summaryPathLabel().getText());
        assertEquals("From global default", tab.summarySourceLabel().getText());
        assertEquals("Folder saved.", tab.globalFeedbackLabel().getText());
        assertTrue(originalPanel == tab.panel());
    }

    private static void assertSectionStructure(JPanel sectionPanel, String helperText) {
        JTextField field = findSingle(sectionPanel, JTextField.class);
        List<JButton> buttons = findAll(sectionPanel, JButton.class);
        List<JLabel> labels = findAll(sectionPanel, JLabel.class);

        assertEquals(1, findAll(sectionPanel, JTextField.class).size());
        assertEquals(2, buttons.size());
        assertTrue(buttons.stream().anyMatch(button -> button.getText().equals("Browse…")));
        assertTrue(buttons.stream().anyMatch(button -> button.getText().equals("Save folder")));
        assertTrue(labels.stream().anyMatch(label -> label.getText().equals(helperText)));
        assertTrue(labels.stream().anyMatch(label -> label.getText().isEmpty()));
        assertTrue(field.isEditable());
    }

    private static <T> T findSingle(JPanel panel, Class<T> type) {
        return type.cast(findAll(panel, type).getFirst());
    }

    private static <T> List<T> findAll(JPanel panel, Class<T> type) {
        List<T> results = new ArrayList<>();
        for (java.awt.Component component : panel.getComponents()) {
            if (type.isInstance(component)) {
                results.add(type.cast(component));
            }
        }
        return results;
    }

    private static final class FolderSettingsFixtures {
        private static FolderSettingsViewState enabledState() {
            return new FolderSettingsViewState(
                    "Export folders",
                    "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.",
                    "Current export folder",
                    "C:/fallback/exports",
                    "From fallback default",
                    new FolderSettingsViewState.SectionState(
                            "Global default folder",
                            "",
                            "Used for all exports unless the current Burp project has its own override.",
                            "Save folder",
                            "Browse…",
                            true,
                            ""
                    ),
                    new FolderSettingsViewState.SectionState(
                            "Current project override",
                            "",
                            "Used only for this Burp project file and overrides the global folder.",
                            "Save folder",
                            "Browse…",
                            true,
                            ""
                    )
            );
        }

        private static FolderSettingsViewState disabledProjectState() {
            return new FolderSettingsViewState(
                    "Export folders",
                    "Choose where b2auco saves exported requests. Project overrides take precedence over the global folder.",
                    "Current export folder",
                    "C:/fallback/exports",
                    "From fallback default",
                    new FolderSettingsViewState.SectionState(
                            "Global default folder",
                            "",
                            "Used for all exports unless the current Burp project has its own override.",
                            "Save folder",
                            "Browse…",
                            true,
                            ""
                    ),
                    new FolderSettingsViewState.SectionState(
                            "Current project override",
                            "",
                            "Open or save a Burp project file to set a project-specific override.",
                            "Save folder",
                            "Browse…",
                            false,
                            ""
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
                                    "Save folder",
                                    "Browse…",
                                    true,
                                    "Folder saved."
                            ),
                            new FolderSettingsViewState.SectionState(
                                    "Current project override",
                                    "",
                                    "Used only for this Burp project file and overrides the global folder.",
                                    "Save folder",
                                    "Browse…",
                                    true,
                                    ""
                            )
                    )
            );
        }
    }

    private static final class InMemoryFolderSettingsStore implements FolderSettingsStore {
        private Optional<Path> globalDefault = Optional.empty();
        private final Map<Path, Path> projectOverrides = new HashMap<>();

        @Override
        public Optional<Path> findGlobalDefault() {
            return globalDefault;
        }

        @Override
        public void saveGlobalDefault(Path folderPath) {
            globalDefault = Optional.of(folderPath.normalize());
        }

        @Override
        public Optional<Path> findProjectOverride(Path projectFilePath) {
            return Optional.ofNullable(projectOverrides.get(projectFilePath.normalize()));
        }

        @Override
        public void saveProjectOverride(Path projectFilePath, Path folderPath) {
            projectOverrides.put(projectFilePath.normalize(), folderPath.normalize());
        }
    }
}
