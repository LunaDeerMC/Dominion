package cn.lunadeer.dominion.utils;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LegacyToMiniMessage {

    private static MiniMessage miniMessage = null;

    private static final Map<String, String> legacyToMiniMessageMap = Map.ofEntries(
            // Standard color codes
            Map.entry("§0", "<black>"),
            Map.entry("§1", "<dark_blue>"),
            Map.entry("§2", "<dark_green>"),
            Map.entry("§3", "<dark_aqua>"),
            Map.entry("§4", "<dark_red>"),
            Map.entry("§5", "<dark_purple>"),
            Map.entry("§6", "<gold>"),
            Map.entry("§7", "<gray>"),
            Map.entry("§8", "<dark_gray>"),
            Map.entry("§9", "<blue>"),
            Map.entry("§a", "<green>"),
            Map.entry("§b", "<aqua>"),
            Map.entry("§c", "<red>"),
            Map.entry("§d", "<light_purple>"),
            Map.entry("§e", "<yellow>"),
            Map.entry("§f", "<white>"),
            // Legacy formatting codes
            Map.entry("§l", "<bold>"),
            Map.entry("§m", "<strikethrough>"),
            Map.entry("§n", "<underline>"),
            Map.entry("§o", "<italic>"),
            Map.entry("§r", "<reset>"),
            Map.entry("§k", "<obfuscated>")
    );

    public static final char COLOR_CHAR = '§';

    public static Component parse(String legacyText) {
        if (miniMessage == null) {
            miniMessage = MiniMessage.miniMessage();
        }
        String miniMessageText = translateAlternateColorCodes('&', legacyText);
        for (Map.Entry<String, String> entry : legacyToMiniMessageMap.entrySet()) {
            miniMessageText = miniMessageText.replace(entry.getKey(), entry.getValue());
        }
        return miniMessage.deserialize(miniMessageText);
    }

    @NotNull
    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        Preconditions.checkArgument(textToTranslate != null, "无法翻译空文本");
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }
}
