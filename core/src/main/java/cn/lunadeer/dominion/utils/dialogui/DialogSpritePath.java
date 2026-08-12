package cn.lunadeer.dominion.utils.dialogui;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Parsed path for Minecraft's {@code atlas} text component.
 *
 * <p>Dominion exposes one compact path to UI code and configuration:
 * {@code namespace:atlas/sprite}.  Minecraft's text component protocol needs
 * the atlas and sprite as two separate namespaced identifiers.</p>
 */
public record DialogSpritePath(String atlas, String sprite) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9._-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public DialogSpritePath {
        Objects.requireNonNull(atlas, "atlas");
        Objects.requireNonNull(sprite, "sprite");
        validateIdentifier(atlas, "atlas");
        validateIdentifier(sprite, "sprite");
    }

    /**
     * Parses {@code namespace:atlas/sprite}.  The namespace is optional for
     * convenience and defaults to {@code minecraft}.
     */
    public static DialogSpritePath parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Dialog sprite path cannot be blank");
        }

        String raw = value.trim();
        int colon = raw.indexOf(':');
        if (colon != raw.lastIndexOf(':')) {
            throw new IllegalArgumentException("Invalid dialog sprite path: " + value);
        }
        String namespace = colon < 0 ? "minecraft" : raw.substring(0, colon);
        String path = colon < 0 ? raw : raw.substring(colon + 1);
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid dialog sprite namespace: " + namespace);
        }

        int slash = path.indexOf('/');
        if (slash <= 0 || slash == path.length() - 1 || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Dialog sprite path must be namespace:atlas/sprite: " + value);
        }
        String atlasPath = path.substring(0, slash);
        String spritePath = path.substring(slash + 1);
        if (!PATH.matcher(atlasPath).matches() || atlasPath.indexOf('/') >= 0
                || !PATH.matcher(spritePath).matches()
                || spritePath.startsWith("/") || spritePath.endsWith("/")) {
            throw new IllegalArgumentException("Invalid dialog sprite path: " + value);
        }

        return new DialogSpritePath(namespace + ":" + atlasPath, namespace + ":" + spritePath);
    }

    public static boolean isValid(String value) {
        try {
            parse(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public DialogSpritePath withAtlas(String value) {
        return new DialogSpritePath(value, sprite);
    }

    private static void validateIdentifier(String value, String name) {
        int colon = value.indexOf(':');
        if (colon <= 0 || colon == value.length() - 1 || colon != value.lastIndexOf(':')) {
            throw new IllegalArgumentException("Invalid " + name + " identifier: " + value);
        }
        String namespace = value.substring(0, colon);
        String path = value.substring(colon + 1);
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid " + name + " identifier: " + value);
        }
    }

}
