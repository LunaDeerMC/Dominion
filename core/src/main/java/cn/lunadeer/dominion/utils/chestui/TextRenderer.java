package cn.lunadeer.dominion.utils.chestui;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public final class TextRenderer {
    private TextRenderer() {}

    /** Marks configuration-owned text whose MiniMessage formatting should be preserved. */
    public record Formatted(String value) {}

    public static Formatted formatted(String value) {
        return new Formatted(value);
    }

    public static String render(Player player, String template, Map<String, ?> values) {
        String result = template;
        if (Bukkit.getServer() != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = PlaceholderAPI.setPlaceholders(player, result);
            } catch (Throwable ignored) {
            }
        }
        return replaceNamed(result, values);
    }

    public static String replaceNamed(String template, Map<String, ?> values) {
        String result = template;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            String replacement = value instanceof Formatted formatted
                    ? formatted.value() : escape(String.valueOf(value));
            result = result.replace("{" + entry.getKey() + "}", replacement);
        }
        return result;
    }

    private static String escape(String value) {
        return value.replace("<", "\\<").replace(">", "\\>")
                .replace('&', '＆').replace('§', '＃');
    }
}
