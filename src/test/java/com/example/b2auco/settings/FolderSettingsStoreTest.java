package com.example.b2auco.settings;

import burp.api.montoya.persistence.Preferences;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                "saveProjectOverride",
                "clearProjectOverride"
        ), methodNames);
    }

    @Test
    void saveGlobalDefaultRejectsBlankFolderInput() {
        InMemoryPreferences preferences = new InMemoryPreferences();
        FolderSettingsStore store = new PreferencesFolderSettingsStore(preferences);

        assertThrows(IllegalArgumentException.class, () -> store.saveGlobalDefault(blankPath()));
        assertTrue(preferences.stringKeys().isEmpty());
    }

    @Test
    void saveGlobalDefaultRoundTripsNormalizedFolderPathFromPreferencesStorage() {
        FolderSettingsStore store = new PreferencesFolderSettingsStore(new InMemoryPreferences());
        Path rawFolderPath = Path.of("C:/work/exports/../exports");

        store.saveGlobalDefault(rawFolderPath);

        assertEquals(Path.of("C:/work/exports"), store.findGlobalDefault().orElseThrow());
    }

    @Test
    void saveProjectOverrideRejectsBlankProjectIdentityKey() {
        InMemoryPreferences preferences = new InMemoryPreferences();
        FolderSettingsStore store = new PreferencesFolderSettingsStore(preferences);

        assertThrows(IllegalArgumentException.class, () -> store.saveProjectOverride(blankPath(), Path.of("C:/work/exports")));
        assertTrue(preferences.stringKeys().isEmpty());
    }

    @Test
    void saveProjectOverrideRoundTripsNormalizedFolderPathForExactProjectFileIdentity() {
        FolderSettingsStore store = new PreferencesFolderSettingsStore(new InMemoryPreferences());
        Path alphaProjectFile = Path.of("C:/work/alpha.burp");
        Path betaProjectFile = Path.of("C:/work/beta.burp");
        Path rawFolderPath = Path.of("C:/work/alpha-exports/./requests");

        store.saveProjectOverride(alphaProjectFile, rawFolderPath);

        assertEquals(Path.of("C:/work/alpha-exports/requests"), store.findProjectOverride(alphaProjectFile).orElseThrow());
        assertTrue(store.findProjectOverride(betaProjectFile).isEmpty());
    }

    @Test
    void clearProjectOverrideRemovesOnlyTheMatchingProjectIdentity() {
        FolderSettingsStore store = new PreferencesFolderSettingsStore(new InMemoryPreferences());
        Path alphaProjectFile = Path.of("C:/work/alpha.burp");
        Path betaProjectFile = Path.of("C:/work/beta.burp");
        store.saveProjectOverride(alphaProjectFile, Path.of("C:/work/alpha-exports"));
        store.saveProjectOverride(betaProjectFile, Path.of("C:/work/beta-exports"));

        store.clearProjectOverride(alphaProjectFile);

        assertTrue(store.findProjectOverride(alphaProjectFile).isEmpty());
        assertEquals(Path.of("C:/work/beta-exports"), store.findProjectOverride(betaProjectFile).orElseThrow());
    }

    private static Path blankPath() {
        return (Path) Proxy.newProxyInstance(
                Path.class.getClassLoader(),
                new Class<?>[]{Path.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "   ";
                    case "normalize" -> proxy;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    static final class InMemoryPreferences implements Preferences {
        private final Map<String, String> strings = new HashMap<>();

        @Override
        public String getString(String key) {
            return strings.get(key);
        }

        @Override
        public void setString(String key, String value) {
            strings.put(key, value);
        }

        @Override
        public void deleteString(String key) {
            strings.remove(key);
        }

        @Override
        public Set<String> stringKeys() {
            return new HashSet<>(strings.keySet());
        }

        @Override public Boolean getBoolean(String key) { return null; }
        @Override public void setBoolean(String key, boolean value) { throw new UnsupportedOperationException(); }
        @Override public void deleteBoolean(String key) { throw new UnsupportedOperationException(); }
        @Override public Set<String> booleanKeys() { return Set.of(); }
        @Override public Byte getByte(String key) { return null; }
        @Override public void setByte(String key, byte value) { throw new UnsupportedOperationException(); }
        @Override public void deleteByte(String key) { throw new UnsupportedOperationException(); }
        @Override public Set<String> byteKeys() { return Set.of(); }
        @Override public Short getShort(String key) { return null; }
        @Override public void setShort(String key, short value) { throw new UnsupportedOperationException(); }
        @Override public void deleteShort(String key) { throw new UnsupportedOperationException(); }
        @Override public Set<String> shortKeys() { return Set.of(); }
        @Override public Integer getInteger(String key) { return null; }
        @Override public void setInteger(String key, int value) { throw new UnsupportedOperationException(); }
        @Override public void deleteInteger(String key) { throw new UnsupportedOperationException(); }
        @Override public Set<String> integerKeys() { return Set.of(); }
        @Override public Long getLong(String key) { return null; }
        @Override public void setLong(String key, long value) { throw new UnsupportedOperationException(); }
        @Override public void deleteLong(String key) { throw new UnsupportedOperationException(); }
        @Override public Set<String> longKeys() { return Set.of(); }
    }
}
