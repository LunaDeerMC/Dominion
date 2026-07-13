package cn.lunadeer.dominion.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LimitationTest {

    @Test
    void newLimitationFallsBackToDefaultWorldSettings() {
        Limitation limitation = new Limitation();

        Limitation.WorldLimitationSetting defaultSettings = limitation.getWorldSettings((String) null);

        assertNotNull(defaultSettings);
        assertSame(defaultSettings, limitation.getWorldSettings("unconfigured-world"));
    }

    @Test
    void missingDefaultWorldSettingsIsRepaired() {
        Limitation limitation = new Limitation();
        limitation.worldLimitations.clear();

        Limitation.WorldLimitationSetting settings = limitation.getWorldSettings("world");

        assertNotNull(settings);
        assertSame(settings, limitation.worldLimitations.get("default"));
    }
}
