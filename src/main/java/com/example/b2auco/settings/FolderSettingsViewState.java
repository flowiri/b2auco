package com.example.b2auco.settings;

import java.util.Objects;

public record FolderSettingsViewState(
        String title,
        String introText,
        String summaryLabel,
        String summaryFolderPath,
        String summarySourceLabel,
        SectionState globalSection,
        SectionState projectSection
) {
    public FolderSettingsViewState {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(introText, "introText");
        Objects.requireNonNull(summaryLabel, "summaryLabel");
        Objects.requireNonNull(summaryFolderPath, "summaryFolderPath");
        Objects.requireNonNull(summarySourceLabel, "summarySourceLabel");
        Objects.requireNonNull(globalSection, "globalSection");
        Objects.requireNonNull(projectSection, "projectSection");
    }

    public record SectionState(
            String heading,
            String fieldValue,
            String helperText,
            String actionLabel,
            String browseLabel,
            boolean enabled,
            String feedbackMessage
    ) {
        public SectionState {
            Objects.requireNonNull(heading, "heading");
            Objects.requireNonNull(fieldValue, "fieldValue");
            Objects.requireNonNull(helperText, "helperText");
            Objects.requireNonNull(actionLabel, "actionLabel");
            Objects.requireNonNull(browseLabel, "browseLabel");
            Objects.requireNonNull(feedbackMessage, "feedbackMessage");
        }
    }
}
