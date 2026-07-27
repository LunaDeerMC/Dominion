package cn.lunadeer.dominion.utils.chestui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemModelSupportTest {
    @Test
    void coreRemainsCompatibleWithThe1_20_1ItemMetaApi() {
        assertFalse(ItemModelSupport.supportsItemModel());
        ItemModelSupport.warnIfUnsupported();
        ItemModelSupport.apply(null, "dominion:button");
    }

    @Test
    void normalizesValidKeysAndRejectsInvalidKeys() {
        assertEquals("dominion:button", ItemModelSupport.normalizeConfiguredModel(" dominion:button "));
        assertNull(ItemModelSupport.normalizeConfiguredModel("invalid key"));
    }
}
