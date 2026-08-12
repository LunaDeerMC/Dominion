package cn.lunadeer.dominion.utils.dialogui;

import java.util.HashMap;
import java.util.Map;

public record DialogRoute(String id, Map<String, String> parameters, int page, String filter) {
    public DialogRoute {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("route id cannot be blank");
        parameters = Map.copyOf(parameters);
        page = Math.max(1, page);
        filter = filter == null ? "" : filter;
    }

    public static DialogRoute of(String id) {
        return new DialogRoute(id, Map.of(), 1, "");
    }

    public static DialogRoute of(Enum<?> id) {
        return of(id.name());
    }

    public DialogRoute with(String key, Object value) {
        Map<String, String> copy = new HashMap<>(parameters);
        copy.put(key, String.valueOf(value));
        return new DialogRoute(id, copy, page, filter);
    }

    public DialogRoute without(String key) {
        if (!parameters.containsKey(key)) return this;
        Map<String, String> copy = new HashMap<>(parameters);
        copy.remove(key);
        return new DialogRoute(id, copy, page, filter);
    }

    public DialogRoute page(int value) {
        return new DialogRoute(id, parameters, value, filter);
    }

    public DialogRoute filter(String value) {
        return new DialogRoute(id, parameters, 1, value);
    }

    public int integer(String key) {
        return Integer.parseInt(parameters.getOrDefault(key, "-1"));
    }

    public String string(String key) {
        return parameters.getOrDefault(key, "");
    }
}
