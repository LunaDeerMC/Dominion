package cn.lunadeer.dominion.utils.chestui;

import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies the 1.21.4+ item model component without linking core to that API at compile time.
 */
public final class ItemModelSupport {
    private static final Method SET_ITEM_MODEL = findSetter();
    private static final AtomicBoolean UNSUPPORTED_WARNING_SHOWN = new AtomicBoolean();
    private static final AtomicBoolean INVOCATION_WARNING_SHOWN = new AtomicBoolean();
    private static final Set<String> INVALID_MODELS = ConcurrentHashMap.newKeySet();

    private ItemModelSupport() {
    }

    /** Normalizes and validates a configured resource key. Invalid values are ignored. */
    public static String normalizeConfiguredModel(String configured) {
        if (configured == null || configured.isBlank()) return null;
        String value = configured.trim();
        if (NamespacedKey.fromString(value) == null) {
            if (INVALID_MODELS.add(value)) {
                warn("Invalid chest UI item-model '{0}'; expected a valid namespace:key.", value);
            }
            return null;
        }
        return value;
    }

    public static boolean supportsItemModel() {
        return SET_ITEM_MODEL != null;
    }

    /** Warns once when configuration uses item models on an API that cannot apply them. */
    public static void warnIfUnsupported() {
        if (!supportsItemModel() && UNSUPPORTED_WARNING_SHOWN.compareAndSet(false, true)) {
            warn("Chest UI item-model is configured, but this server API does not support it; "
                    + "custom-model-data will be used as the legacy fallback.");
        }
    }

    /** Applies an item model when the running server exposes the supported API. */
    public static void apply(ItemMeta meta, String configured) {
        if (meta == null || configured == null) return;
        if (SET_ITEM_MODEL == null) {
            warnIfUnsupported();
            return;
        }

        NamespacedKey key = NamespacedKey.fromString(configured);
        if (key == null) {
            // ItemAppearance normally validates this during configuration parsing. Keep this
            // defensive check for programmatically-created appearances and reload races.
            normalizeConfiguredModel(configured);
            return;
        }

        try {
            SET_ITEM_MODEL.invoke(meta, key);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException | SecurityException e) {
            if (INVOCATION_WARNING_SHOWN.compareAndSet(false, true)) {
                warn("Unable to apply chest UI item-model '{0}': {1}", configured, e.getMessage());
            }
        }
    }

    private static Method findSetter() {
        try {
            return ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
        } catch (NoSuchMethodException | SecurityException ignored) {
            return null;
        }
    }

    private static void warn(String message, Object... args) {
        // Unit tests can construct configuration objects without bootstrapping the plugin logger.
        if (XLogger.instance != null) XLogger.warn(message, args);
    }
}
