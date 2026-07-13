package cn.lunadeer.dominion.utils.chestui;

import cn.lunadeer.dominion.utils.chestui.config.ItemAppearance;
import cn.lunadeer.dominion.utils.LegacyToMiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ItemFactory {
    private ItemFactory() {}

    static ItemStack create(Player viewer, ItemAppearance appearance, String name, List<String> lore,
                            Map<String, ?> placeholders, UUID headOwner) {
        ItemStack item = new ItemStack(appearance.material(), appearance.amount());
        ItemMeta meta = item.getItemMeta();
        Component displayName = LegacyToMiniMessage.parse(TextRenderer.render(viewer, name, placeholders))
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(displayName);
        List<Component> renderedLore = new ArrayList<>();
        for (String line : lore) {
            renderedLore.add(LegacyToMiniMessage.parse(TextRenderer.render(viewer, line, placeholders))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(renderedLore);
        if (appearance.customModelData() != null) meta.setCustomModelData(appearance.customModelData());
        if (appearance.glow()) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (!appearance.itemFlags().isEmpty()) {
            meta.addItemFlags(appearance.itemFlags().toArray(ItemFlag[]::new));
        }
        UUID resolvedHead = resolveHead(viewer, appearance.headSource(), headOwner);
        if (meta instanceof SkullMeta skull && resolvedHead != null) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(resolvedHead));
        }
        item.setItemMeta(meta);
        return item;
    }

    private static UUID resolveHead(Player viewer, String source, UUID dynamic) {
        if (source == null || source.equalsIgnoreCase("dynamic")) return dynamic;
        if (source.equalsIgnoreCase("viewer")) return viewer.getUniqueId();
        if (source.equalsIgnoreCase("none")) return null;
        try {
            return UUID.fromString(source);
        } catch (IllegalArgumentException ignored) {
            return dynamic;
        }
    }
}
