package cn.lunadeer.dominion.utils.dialogui;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A version-independent namespaced identifier used by the dialog model.
 */
public record DialogKey(String namespace, String value) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern VALUE = Pattern.compile("[a-z0-9/._-]+");

    public DialogKey {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(value, "value");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }
        if (!VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid namespaced value: " + value);
        }
    }

    public static DialogKey of(String namespace, String value) {
        return new DialogKey(namespace, value);
    }

    public static DialogKey parse(String input) {
        Objects.requireNonNull(input, "input");
        int separator = input.indexOf(':');
        if (separator < 1 || separator == input.length() - 1) {
            throw new IllegalArgumentException("Expected namespace:value, got: " + input);
        }
        return new DialogKey(input.substring(0, separator), input.substring(separator + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + value;
    }
}
