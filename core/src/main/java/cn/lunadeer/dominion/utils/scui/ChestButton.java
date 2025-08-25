package cn.lunadeer.dominion.utils.scui;

import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.utils.ColorParser;
import cn.lunadeer.dominion.utils.LegacyToMiniMessage;
import cn.lunadeer.dominion.utils.Misc;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static cn.lunadeer.dominion.utils.Misc.*;

public abstract class ChestButton {

    private final char symbol;
    private final ItemStack item;
    private String displayName;
    private List<String> lore;

    public ChestButton(ButtonConfiguration config) {
        if (config.material.contains(";")) {
            String[] parts = config.material.split(";");
            if (parts.length == 3 && parts[0].equalsIgnoreCase("PLAYER_HEAD")) {
                // Handle PLAYER_HEAD with B64 or URL
                this.item = new ItemStack(Material.PLAYER_HEAD);
                Scheduler.runTaskAsync(() -> {
                    SkullMeta meta = (SkullMeta) this.item.getItemMeta();
                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), null);
                    PlayerTextures textures = profile.getTextures();
                    try {
                        if (parts[1].equalsIgnoreCase("B64")) {
                            String textureDecoded = new String(Base64.getDecoder().decode(parts[2]));
                            // decode: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/bb772b2b9bf108ef15de1556c41eec0cc781a9e6cc6507669e0d2c3b56b740cc"}}}
                            JSONObject json = (JSONObject) new JSONParser().parse(textureDecoded);
                            String skinUrlString = (String) ((JSONObject) ((JSONObject) json.get("textures")).get("SKIN")).get("url");
                            textures.setSkin(new URL(skinUrlString));
                        } else if (parts[1].equalsIgnoreCase("URL")) {
                            // Handle PLAYER_HEAD with URL
                            String skinUrlString = parts[2];
                            textures.setSkin(new URL(skinUrlString));
                        } else if (parts[1].equalsIgnoreCase("NAME")) {
                            // Handle PLAYER_HEAD with NAME
                            PlayerDTO cachedPlayer = CacheManager.instance.getPlayer(parts[2]);
                            if (cachedPlayer == null) {
                                URL url = new URL("http://textures.minecraft.net/texture/613ba1403f98221fab6f4ae0f9e5298068262258966e8f9e53cdedd97aa45ef1");
                                textures.setSkin(url);
                            } else {
                                textures.setSkin(cachedPlayer.getSkinUrl());
                            }
                        } else {
                            throw new IllegalArgumentException("Invalid PLAYER_HEAD texture type: " + parts[1] +
                                    ". Expected 'B64' or 'URL' or 'NAME'.");
                        }
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Failed to parse texture data: " + parts[2], e);
                    }
                    profile.setTextures(textures);
                    meta.setOwnerProfile(profile);
                    this.item.setItemMeta(meta);
                });
            } else {
                throw new IllegalArgumentException("Invalid material type: " + config.material);
            }
        } else {
            this.item = new ItemStack(Material.valueOf(config.material));
        }
        this.symbol = config.getSymbol();
        this.displayName = config.name;
        this.lore = new ArrayList<>(config.lore);
    }

    /**
     * Sets the display name of the button with formatted arguments.
     * The arguments are applied to placeholders in the display name.
     *
     * @param args The arguments to format the display name.
     * @return The current instance of `ChestButton` for method chaining.
     */
    public ChestButton setDisplayNameArgs(Object... args) {
        this.displayName = formatString(displayName, args);
        return this;
    }

    /**
     * Sets the lore of the button with formatted arguments.
     * Each argument replaces corresponding placeholders in the lore strings.
     *
     * @param args The arguments to format the lore.
     * @return The current instance of `ChestButton` for method chaining.
     */
    public ChestButton setLoreArgs(Object... args) {
        this.lore = formatStringList(lore, args);
        return this;
    }

    public abstract void onClick(ClickType type);

    public ItemStack build(Player viewOwner) {
        ItemMeta meta = item.getItemMeta();
        if (displayName != null) {
            displayName = setPlaceholder(viewOwner, displayName);
            displayName = ColorParser.getBukkitType(displayName);
            if (Misc.isPaper()) {
                meta.displayName(LegacyToMiniMessage.parse(displayName).decoration(TextDecoration.ITALIC, false));
            } else {
                meta.setDisplayName(displayName);
            }
        }
        if (lore != null) {
            List<Component> componentsLore = new ArrayList<>();
            for (int i = 0; i < lore.size(); i++) {
                lore.set(i, setPlaceholder(viewOwner, lore.get(i)));
                lore.set(i, ColorParser.getBukkitType(lore.get(i)));
                componentsLore.add(LegacyToMiniMessage.parse(lore.get(i)).decoration(TextDecoration.ITALIC, false));
            }
            if (Misc.isPaper()) {
                meta.lore(componentsLore);
            } else {
                meta.setLore(lore);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    public char getSymbol() {
        return symbol;
    }
}
