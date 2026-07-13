package cn.lunadeer.dominion.utils.chestui;

import java.util.HashMap;
import java.util.Map;

public record MenuRoute(String id, Map<String, String> parameters, int page, String filter) {
    public MenuRoute {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("route id cannot be blank");
        parameters = Map.copyOf(parameters);
        page = Math.max(1, page);
        filter = filter == null ? "" : filter;
    }

    public static MenuRoute of(String id) {
        return new MenuRoute(id, Map.of(), 1, "");
    }

    public static MenuRoute of(Enum<?> id) {
        return of(id.name());
    }

    public MenuRoute with(String key, Object value) {
        Map<String, String> copy = new HashMap<>(parameters);
        copy.put(key, String.valueOf(value));
        return new MenuRoute(id, copy, page, filter);
    }

    public MenuRoute page(int value) {
        return new MenuRoute(id, parameters, value, filter);
    }

    public MenuRoute filter(String value) {
        return new MenuRoute(id, parameters, 1, value);
    }

    public int integer(String key) {
        return Integer.parseInt(parameters.getOrDefault(key, "-1"));
    }

    public String string(String key) {
        return parameters.getOrDefault(key, "");
    }
}
