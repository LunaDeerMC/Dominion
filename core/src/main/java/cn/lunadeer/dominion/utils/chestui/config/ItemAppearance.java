package cn.lunadeer.dominion.utils.chestui.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public record ItemAppearance(Material material, int amount, Integer customModelData, boolean glow,
                             Set<ItemFlag> itemFlags, String headSource) {
    public ItemAppearance {
        itemFlags = Set.copyOf(itemFlags);
        headSource = headSource == null ? "dynamic" : headSource;
    }

    /** Convenience constructor used for dynamic flag icons. */
    public ItemAppearance(Material material, int amount, Integer customModelData, boolean glow,
                          boolean playerHead) {
        this(material, amount, customModelData, glow, Set.of(), playerHead ? "dynamic" : "none");
    }

    static ItemAppearance read(ConfigurationSection section) {
        String materialName = section == null ? "STONE" : section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName == null ? "STONE" : materialName);
        if (material == null) material = Material.BARRIER;
        int amount = section == null ? 1 : Math.max(1, Math.min(64, section.getInt("amount", 1)));
        Integer model = section != null && section.contains("custom-model-data")
                ? section.getInt("custom-model-data") : null;
        boolean glow = section != null && section.getBoolean("glow", false);
        Set<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);
        if (section != null) {
            for (String value : section.getStringList("item-flags")) {
                try {
                    flags.add(ItemFlag.valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        String headSource = section == null ? "dynamic" : section.getString("head-source", "dynamic");
        return new ItemAppearance(material, amount, model, glow, flags, headSource);
    }
}
