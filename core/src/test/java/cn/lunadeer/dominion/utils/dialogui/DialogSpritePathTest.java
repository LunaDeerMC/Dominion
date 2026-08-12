package cn.lunadeer.dominion.utils.dialogui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogSpritePathTest {
    @Test
    void parsesCompactAtlasSpritePath() {
        DialogSpritePath path = DialogSpritePath.parse("minecraft:items/item/emerald");

        assertEquals("minecraft:items", path.atlas());
        assertEquals("minecraft:item/emerald", path.sprite());
        assertTrue(DialogSpritePath.isValid("minecraft:gui/button"));
        assertTrue(DialogSpritePath.isValid("minecraft:items/item/paper"));
    }

    @Test
    void rejectsMalformedPath() {
        assertFalse(DialogSpritePath.isValid("minecraft:blocks"));
        assertFalse(DialogSpritePath.isValid("minecraft:blocks/"));
        assertFalse(DialogSpritePath.isValid("Minecraft:items/item/paper"));
        assertThrows(IllegalArgumentException.class,
                () -> DialogSpritePath.parse("minecraft:items/item/emerald:extra"));
    }

}
