package cn.lunadeer.dominion.uis.dialog.pages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

final class DialogTextRenderer {
    private DialogTextRenderer() {
    }

    static String replaceNamed(String template, Map<String, ?> values) {
        String result = template == null ? "" : template;
        for (var entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", formatted(entry.getValue()));
        }
        return result;
    }

    static String formatted(Object value) {
        if (value instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }
        return String.valueOf(value);
    }
}
