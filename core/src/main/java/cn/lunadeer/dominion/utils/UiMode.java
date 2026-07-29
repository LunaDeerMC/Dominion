package cn.lunadeer.dominion.utils;

public enum UiMode {
    DEFAULT,
    CHEST,
    DIALOG;

    public static UiMode parse(String value, UiMode fallback) {
        if (value == null) return fallback;
        try {
            return UiMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
