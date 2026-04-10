package com.example.b2auco.settings;

import burp.api.montoya.persistence.PersistedObject;
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
    void exposesFolderPersistenceContractForGlobalDefaultsAndCurrentProjectOverrides() {
        Set<String> methodNames = Arrays.stream(FolderSettingsStore.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "findGlobalDefault",
                "saveGlobalDefault",
                "findCurrentProjectOverride",
                "saveCurrentProjectOverride",
                "clearCurrentProjectOverride"
        ), methodNames);
    }

    @Test
    void saveGlobalDefaultRejectsBlankFolderInput() {
        InMemoryPreferences preferences = new InMemoryPreferences();
        FolderSettingsStore store = new PreferencesFolderSettingsStore(preferences, persistedObject());

        assertThrows(IllegalArgumentException.class, () -> store.saveGlobalDefault(blankPath()));
        assertTrue(preferences.stringKeys().isEmpty());
    }

    @Test
    void saveGlobalDefaultRoundTripsNormalizedFolderPathFromPreferencesStorage() {
        FolderSettingsStore store = new PreferencesFolderSettingsStore(new InMemoryPreferences(), persistedObject());
        Path rawFolderPath = Path.of("C:/work/exports/../exports");

        store.saveGlobalDefault(rawFolderPath);

        assertEquals(Path.of("C:/work/exports"), store.findGlobalDefault().orElseThrow());
    }

    @Test
    void saveCurrentProjectOverrideRejectsBlankFolderInput() {
        PersistedObject extensionData = persistedObject();
        FolderSettingsStore store = new PreferencesFolderSettingsStore(new InMemoryPreferences(), extensionData);

        assertThrows(IllegalArgumentException.class, () -> store.saveCurrentProjectOverride(blankPath()));
        assertTrue(extensionData.stringKeys().isEmpty());
    }

    @Test
    void saveCurrentProjectOverrideRoundTripsNormalizedFolderPathFromProjectStorage() {
        FolderSettingsStore store = new PreferencesFolderSettingsStore(new InMemoryPreferences(), persistedObject());
        Path rawFolderPath = Path.of("C:/work/project-exports/./requests");

        store.saveCurrentProjectOverride(rawFolderPath);

        assertEquals(Path.of("C:/work/project-exports/requests"), store.findCurrentProjectOverride().orElseThrow());
    }

    @Test
    void clearCurrentProjectOverrideRemovesStoredProjectOverride() {
        FolderSettingsStore store = new PreferencesFolderSettingsStore(new InMemoryPreferences(), persistedObject());
        store.saveCurrentProjectOverride(Path.of("C:/work/project-exports"));

        store.clearCurrentProjectOverride();

        assertTrue(store.findCurrentProjectOverride().isEmpty());
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

    private static PersistedObject persistedObject() {
        Map<String, String> strings = new HashMap<>();
        return (PersistedObject) Proxy.newProxyInstance(
                PersistedObject.class.getClassLoader(),
                new Class<?>[]{PersistedObject.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getString" -> strings.get((String) args[0]);
                    case "setString" -> {
                        strings.put((String) args[0], (String) args[1]);
                        yield null;
                    }
                    case "deleteString" -> {
                        strings.remove((String) args[0]);
                        yield null;
                    }
                    case "stringKeys" -> new HashSet<>(strings.keySet());
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
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
