package cn.lunadeer.dominion.utils;

import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.URL;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerSkinProfileFactoryTest {
    @Test
    void createsAProfileFromTheCachedTextureWithoutLookingUpAnOfflinePlayer() throws Exception {
        CacheManager cacheManager = mock(CacheManager.class);
        PlayerDTO player = mock(PlayerDTO.class);
        PlayerProfile profile = mock(PlayerProfile.class);
        PlayerTextures textures = mock(PlayerTextures.class);
        UUID uuid = UUID.randomUUID();
        URL skin = new URL("http://textures.minecraft.net/texture/cached");
        CacheManager.instance = cacheManager;

        when(cacheManager.getPlayer(uuid)).thenReturn(player);
        when(player.getLastKnownName()).thenReturn("CachedPlayer");
        when(player.getSkinUrl()).thenReturn(skin);
        when(profile.getTextures()).thenReturn(textures);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createPlayerProfile(uuid, "CachedPlayer")).thenReturn(profile);
            PlayerSkinProfileFactory.create(uuid);
        } finally {
            CacheManager.instance = null;
        }

        verify(textures).setSkin(skin);
        verify(profile).setTextures(textures);
    }
}
