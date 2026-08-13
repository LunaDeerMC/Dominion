package cn.lunadeer.dominion.utils;

import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

/** Creates local item profiles from Dominion's cached skin URL. */
public final class PlayerSkinProfileFactory {
    private PlayerSkinProfileFactory() {
    }

    public static PlayerProfile create(UUID playerUuid) {
        PlayerDTO player = CacheManager.instance == null ? null : CacheManager.instance.getPlayer(playerUuid);
        URL skinUrl = PlayerSkin.defaultSkinUrl();
        if (player != null) {
            try {
                skinUrl = player.getSkinUrl();
            } catch (MalformedURLException ignored) {
                // Keep the built-in texture when an old or malformed value is found.
            }
        }

        try {
            String name = player == null ? null : player.getLastKnownName();
            PlayerProfile profile = Bukkit.createPlayerProfile(playerUuid, name);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(skinUrl);
            profile.setTextures(textures);
            return profile;
        } catch (IllegalArgumentException | IllegalStateException | NoSuchMethodError exception) {
            return null;
        }
    }
}
