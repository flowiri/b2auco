package com.example.b2auco.settings;

import java.util.Objects;

// Settings state now carries both legacy two-section UI data and the new three-folder controller data.
public record FolderSettingsViewState(
        String title,
        String introText,
        String summaryLabel,
        String summaryFolderPath,
        String summarySourceLabel,
        ActiveMode activeMode,
        SectionState globalSection,
        SectionState projectSection,
        SectionState authSection,
        SectionState backlogSection,
        SectionState resultsSection,
        String resultsStatusMessage,
        String resultsContent
) {
    // Older tests and view helpers still construct the two-section state; default new sections keep those callers source-compatible.
    public FolderSettingsViewState(
            String title,
            String introText,
            String summaryLabel,
            String summaryFolderPath,
            String summarySourceLabel,
            ActiveMode activeMode,
            SectionState globalSection,
            SectionState projectSection
    ) {
        this(
                title,
                introText,
                summaryLabel,
                summaryFolderPath,
                summarySourceLabel,
                activeMode,
                globalSection,
                projectSection,
                emptySection("Auth folder"),
                emptySection("Backlog folder"),
                emptySection("Results folder"),
                "",
                ""
        );
    }

    // Canonical validation keeps new Auth, Backlog, Results, and existing legacy sections non-null.
    // Canonical constructor validates all strings and sections before Swing consumes the view model.
    public FolderSettingsViewState {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(introText, "introText");
        Objects.requireNonNull(summaryLabel, "summaryLabel");
        Objects.requireNonNull(summaryFolderPath, "summaryFolderPath");
        Objects.requireNonNull(summarySourceLabel, "summarySourceLabel");
        Objects.requireNonNull(activeMode, "activeMode");
        Objects.requireNonNull(globalSection, "globalSection");
        Objects.requireNonNull(projectSection, "projectSection");
        Objects.requireNonNull(authSection, "authSection");
        Objects.requireNonNull(backlogSection, "backlogSection");
        Objects.requireNonNull(resultsSection, "resultsSection");
        Objects.requireNonNull(resultsStatusMessage, "resultsStatusMessage");
        Objects.requireNonNull(resultsContent, "resultsContent");
    }

    // Compatibility constructor above needs inert sections that do not alter old UI tests.
    private static SectionState emptySection(String heading) {
        return new SectionState(heading, "", "", "Save", "Browse…", "", false, false, false, true, "", false);
    }

    // Active modes include legacy tabs plus direct Auth, Backlog, and Results save contexts.
    public enum ActiveMode {
        USER_SETTING,
        PROJECT_SETTING,
        AUTH_FOLDER,
        BACKLOG_FOLDER,
        RESULTS_FOLDER
    }

    // Section state stays generic so the parent UI can render any of the three folder controls.
    public record SectionState(
            String heading,
            String fieldValue,
            String helperText,
            String actionLabel,
            String browseLabel,
            String toggleLabel,
            boolean toggleVisible,
            boolean toggleEnabled,
            boolean toggleSelected,
            boolean controlsEnabled,
            String feedbackMessage,
            boolean mirrorsProjectOverrideToggle
    ) {
        // Section validation preserves the existing non-null rendering contract.
        public SectionState {
            Objects.requireNonNull(heading, "heading");
            Objects.requireNonNull(fieldValue, "fieldValue");
            Objects.requireNonNull(helperText, "helperText");
            Objects.requireNonNull(actionLabel, "actionLabel");
            Objects.requireNonNull(browseLabel, "browseLabel");
            Objects.requireNonNull(toggleLabel, "toggleLabel");
            Objects.requireNonNull(feedbackMessage, "feedbackMessage");
        }
    }
}
