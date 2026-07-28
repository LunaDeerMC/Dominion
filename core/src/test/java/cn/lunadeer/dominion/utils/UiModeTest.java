package cn.lunadeer.dominion.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiModeTest {
    @Test
    void parsesStoredAndConfiguredValuesSafely() {
        assertEquals(UiMode.CHEST, UiMode.parse("chest", UiMode.DEFAULT));
        assertEquals(UiMode.DIALOG, UiMode.parse(" DIALOG ", UiMode.DEFAULT));
        assertEquals(UiMode.DEFAULT, UiMode.parse("unknown", UiMode.DEFAULT));
        assertEquals(UiMode.CHEST, UiMode.parse(null, UiMode.CHEST));
    }
}
