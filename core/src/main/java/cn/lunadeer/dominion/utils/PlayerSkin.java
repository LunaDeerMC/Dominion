package cn.lunadeer.dominion.utils;

import java.net.MalformedURLException;
import java.net.URL;

public final class PlayerSkin {
    public static final String DEFAULT_SKIN_URL = "http://textures.minecraft.net/texture/613ba1403f98221fab6f4ae0f9e5298068262258966e8f9e53cdedd97aa45ef1";

    private PlayerSkin() {
    }

    public static URL defaultSkinUrl() {
        try {
            return new URL(DEFAULT_SKIN_URL);
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Invalid built-in default skin URL", exception);
        }
    }
}
