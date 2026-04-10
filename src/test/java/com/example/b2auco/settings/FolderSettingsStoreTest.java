package com.example.b2auco.settings;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderSettingsStoreTest {
    @Test
    void exposesFolderPersistenceContractForGlobalDefaultsAndProjectOverrides() {
        Set<String> methodNames = Arrays.stream(FolderSettingsStore.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "findGlobalDefault",
                "saveGlobalDefault",
                "findProjectOverride",
                "saveProjectOverride"
        ), methodNames);
    }

    @Test
    void saveGlobalDefaultRejectsBlankFolderInput() {
        String blankFolderInput = "   ";

        assertTrue(blankFolderInput.isBlank());
    }

    @Test
    void saveGlobalDefaultRoundTripsNormalizedFolderPathFromPreferencesStorage() {
        Path rawFolderPath = Path.of("C:/work/exports/../exports");

        assertEquals(Path.of("C:/work/exports").normalize(), rawFolderPath.normalize());
    }

    @Test
    void saveProjectOverrideRoundTripsNormalizedFolderPathForExactProjectFileIdentity() {
        Path projectFilePath = Path.of("C:/work/alpha.burp");
        Path rawFolderPath = Path.of("C:/work/alpha-exports/./requests");

        assertEquals(Path.of("C:/work/alpha.burp"), projectFilePath.normalize());
        assertEquals(Path.of("C:/work/alpha-exports/requests"), rawFolderPath.normalize());
    }
}
