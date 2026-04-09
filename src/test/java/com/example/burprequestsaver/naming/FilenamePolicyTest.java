package com.example.burprequestsaver.naming;

import com.example.burprequestsaver.model.ExportFileName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilenamePolicyTest {
    private final FilenamePolicy policy = new FilenamePolicy();

    // D-01 + D-02
    @Test
    void hostAndFlattenedPathProduceTxtFilename() {
        ExportFileName fileName = policy.deriveFileName("example.com", "/api/users");

        assertEquals("example.com-api-users", fileName.baseStem());
        assertEquals("example.com-api-users.txt", fileName.finalFileName());
        assertTrue(fileName.finalFileName().endsWith(".txt"));
    }

    // D-03
    @Test
    void rootPathUsesExplicitRootMarker() {
        ExportFileName fileName = policy.deriveFileName("example.com", "/");

        assertEquals("example.com-root", fileName.baseStem());
        assertEquals("example.com-root.txt", fileName.finalFileName());
        assertFalse(fileName.finalFileName().equals("example.com.txt"));
    }

    // D-04
    @Test
    void queryFreePathStaysStableAndReadable() {
        ExportFileName fileName = policy.deriveFileName("example.com", "/search");

        assertEquals("example.com-search", fileName.baseStem());
        assertEquals("example.com-search.txt", fileName.finalFileName());
        assertFalse(fileName.finalFileName().contains("?"));
    }

    @Test
    void illegalFilenameCharactersNormalizeToSingleHyphenAndStillEndInTxt() {
        ExportFileName fileName = policy.deriveFileName("exa<mple>.com", "/api/\tusers/:\"bad\"/report|name?ignored*value");

        assertEquals("exa-mple-.com-api-users-bad-report-name", fileName.baseStem());
        assertEquals("exa-mple-.com-api-users-bad-report-name.txt", fileName.finalFileName());
        assertTrue(fileName.finalFileName().endsWith(".txt"));
        assertFalse(fileName.finalFileName().contains("<"));
        assertFalse(fileName.finalFileName().contains(">"));
        assertFalse(fileName.finalFileName().contains(":"));
        assertFalse(fileName.finalFileName().contains("\""));
        assertFalse(fileName.finalFileName().contains("\\"));
        assertFalse(fileName.finalFileName().contains("|"));
        assertFalse(fileName.finalFileName().contains("*"));
    }

    @Test
    void blankNullOrSlashOnlyPathsNormalizeToRootSoHostOnlyNamesNeverAppear() {
        assertEquals("example.com-root.txt", policy.deriveFileName("example.com", null).finalFileName());
        assertEquals("example.com-root.txt", policy.deriveFileName("example.com", "").finalFileName());
        assertEquals("example.com-root.txt", policy.deriveFileName("example.com", "   ").finalFileName());
        assertEquals("example.com-root.txt", policy.deriveFileName("example.com", "///").finalFileName());
    }
}
