package cn.lunadeer.dominion.uis.dialog.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public final class DialogTextRenderer {
    private DialogTextRenderer() {
    }

    public static String replaceNamed(String template, Map<String, ?> values) {
        String result = template == null ? "" : template;
        for (var entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", formatted(entry.getValue()));
        }
        return result;
    }

    public static String formatted(Object value) {
        if (value instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }
        return String.valueOf(value);
    }
}
