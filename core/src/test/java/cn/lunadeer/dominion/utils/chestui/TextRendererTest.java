package cn.lunadeer.dominion.utils.chestui;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextRendererTest {
    @Test
    void replacesNamedPlaceholdersAndEscapesDynamicFormatting() {
        String result = TextRenderer.replaceNamed("<green>{name} {count}",
                Map.of("name", "<red>&c§l", "count", 3));
        assertEquals("<green>\\<red\\>＆c＃l 3", result);
    }

    @Test
    void preservesFormattingForTrustedConfigurationText() {
        String result = TextRenderer.replaceNamed("<gray>Status: {state}",
                Map.of("state", TextRenderer.formatted("<green>Enabled")));
        assertEquals("<gray>Status: <green>Enabled", result);
    }
}
