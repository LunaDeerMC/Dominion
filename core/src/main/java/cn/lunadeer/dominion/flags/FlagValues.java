package cn.lunadeer.dominion.flags;

import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Owns the configured values for one permission scope (guests, member, group or world).
 * Only active keys are stored. The public API view supplies legacy aliases without
 * duplicating their values; persistence receives the active values only.
 */
public final class FlagValues<F extends Flag> {
    private final Map<F, Boolean> stored = new HashMap<>();
    private final Map<F, Boolean> activeView = Collections.unmodifiableMap(stored);
    private final Map<F, Boolean> apiView = LegacyFlagMap.view(stored);

    public Boolean get(F flag) {
        Flag target = Flags.resolveReadFlag(flag);
        return stored.getOrDefault(target, target.getDefaultValue());
    }

    public Boolean getOrDefault(F flag, boolean defaultValue) {
        return stored.getOrDefault(Flags.resolveReadFlag(flag), defaultValue);
    }

    public void set(F flag, Boolean value) {
        if (Flags.isLegacyFlag(flag)) {
            throw new IllegalArgumentException("Legacy flag " + flag.getFlagName()
                    + " is read-only; use " + Flags.resolveReadFlag(flag).getFlagName());
        }
        stored.put(flag, value);
    }

    /** Imports active values, including from a public map containing aliases. */
    public void copyFrom(Map<F, Boolean> source) {
        stored.putAll(activeCopy(source));
    }

    /** The original mutable API map, with additional read-only alias entries. */
    public Map<F, Boolean> asMap() {
        return apiView;
    }

    /** The template API's immutable snapshot, including aliases. */
    public Map<F, Boolean> snapshot() {
        return Map.copyOf(apiView);
    }

    /** Read-only storage view; compatibility aliases are never persisted. */
    public Map<F, Boolean> activeValues() {
        return activeView;
    }

    /** Copies only configurable keys from an external DTO's public map. */
    public static <F extends Flag> Map<F, Boolean> activeCopy(Map<F, Boolean> source) {
        Map<F, Boolean> result = new HashMap<>();
        source.forEach((flag, value) -> {
            if (!Flags.isLegacyFlag(flag)) result.put(flag, value);
        });
        return result;
    }
}
