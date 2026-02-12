package cn.lunadeer.dominion.utils;

import org.bukkit.plugin.java.JavaPlugin;

public class XVersionManager {

    public static ImplementationVersion VERSION;

    public static ImplementationVersion GetVersion(JavaPlugin plugin) {
        String version = plugin.getServer().getBukkitVersion();
        XLogger.debug("API version: {0}", version);
        if (version.contains("1.21")) {
            if (version.contains("1.21.9")
                    || version.contains("1.21.10")
                    || version.contains("1.21.11")) {
                return ImplementationVersion.v1_21_9;
            }
            if (version.contains("1.21.8")) {
                return ImplementationVersion.v1_21_8;
            }
            if (version.contains("1.21.4")
                    || version.contains("1.21.5")
                    || version.contains("1.21.6")
                    || version.contains("1.21.7")) {
                return ImplementationVersion.v1_21_4;
            }
            return ImplementationVersion.v1_21;
        } else if (version.contains("1.20.1")
                || version.contains("1.20.4")
                || version.contains("1.20.6")) {
            return ImplementationVersion.v1_20_1;
        }
        XLogger.error("Unsupported API version: {0}", version);
        plugin.getServer().getPluginManager().disablePlugin(plugin);
        return null;
    }

    public enum ImplementationVersion {
        v1_21_9,
        v1_21_8,
        v1_21_4,
        v1_21,
        v1_20_1;

        private final int[] value;

        ImplementationVersion() {
            // v1_21 -> [1, 21]
            // v1_20_1 -> [1, 20, 1]
            String[] parts = this.name().replace("v", "").split("_");
            value = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                value[i] = Integer.parseInt(parts[i]);
            }
        }

        /**
         * Compares this implementation version with another implementation version.
         * <p>
         * This method compares the current implementation version with another implementation version by comparing their
         * respective version number arrays. It returns a negative integer, zero, or a positive integer if this version
         * is less than, equal to, or greater than the specified version, respectively.
         *
         * @param other the other ImplementationVersion to compare with
         * @return negative integer => this version is less than the other version
         * <br>
         * zero => this version is equal to the other version
         * <br>
         * positive integer => this version is greater than the other version
         */
        public int compareWith(ImplementationVersion other) {
            for (int i = 0; i < Math.min(this.value.length, other.value.length); i++) {
                if (this.value[i] != other.value[i]) {
                    return this.value[i] - other.value[i];
                }
            }
            return this.value.length - other.value.length;
        }

        public ImplementationVersion getPrevious() {
            for (ImplementationVersion version : ImplementationVersion.values()) {
                if (version.compareWith(this) < 0) {
                    return version;
                }
            }
            return null;
        }
    }

}
