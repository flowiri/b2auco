package com.example.b2auco.settings;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectiveFolderResolverTest {
    @Test
    void definesSourceEnumValuesForProjectOverrideGlobalDefaultAndFallbackDefault() {
        assertEquals(
                EnumSet.of(
                        EffectiveFolderSource.PROJECT_OVERRIDE,
                        EffectiveFolderSource.GLOBAL_DEFAULT,
                        EffectiveFolderSource.FALLBACK_DEFAULT
                ),
                EnumSet.allOf(EffectiveFolderSource.class)
        );
    }

    @Test
    void resolvesProjectOverrideBeforeGlobalDefaultBeforeFallbackDefault() {
        assertEquals(EffectiveFolderSource.PROJECT_OVERRIDE, EffectiveFolderSource.valueOf("PROJECT_OVERRIDE"));
        assertEquals(EffectiveFolderSource.GLOBAL_DEFAULT, EffectiveFolderSource.valueOf("GLOBAL_DEFAULT"));
        assertEquals(EffectiveFolderSource.FALLBACK_DEFAULT, EffectiveFolderSource.valueOf("FALLBACK_DEFAULT"));
    }
}
