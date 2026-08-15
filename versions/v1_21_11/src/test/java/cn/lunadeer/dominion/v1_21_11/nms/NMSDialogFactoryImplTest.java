package cn.lunadeer.dominion.v1_21_11.nms;

import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.bukkit.craftbukkit.CraftRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NMSDialogFactoryImplTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CraftRegistry.setMinecraftRegistry(net.minecraft.core.RegistryAccess.EMPTY);
    }

    @Test
    void encodesPlayerHeadObjectWithStoredSkinTexture() throws Exception {
        UUID playerId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        String skinUrl = "https://textures.minecraft.net/texture/example";
        DialogSpec.PlayerHeadIcon playerHead = new DialogSpec.PlayerHeadIcon(
                playerId, "LunaDeer", skinUrl, false);
        Method method = NMSDialogFactoryImpl.class.getDeclaredMethod(
                "component", Component.class, String.class, DialogSpec.PlayerHeadIcon.class);
        method.setAccessible(true);

        net.minecraft.network.chat.Component parsed =
                (net.minecraft.network.chat.Component) method.invoke(
                        null, Component.text("LunaDeer"),
                        "minecraft:items/item/armor_stand", playerHead);

        net.minecraft.network.chat.contents.ObjectContents contents = findObjectContents(parsed);
        net.minecraft.network.chat.contents.objects.PlayerSprite sprite =
                (net.minecraft.network.chat.contents.objects.PlayerSprite) contents.contents();
        com.mojang.authlib.GameProfile profile = sprite.player().partialProfile();
        assertEquals(playerId, profile.id());
        assertEquals("LunaDeer", profile.name());
        assertFalse(sprite.hat());
        String property = profile.properties().get("textures").iterator().next().value();
        String decoded = new String(Base64.getDecoder().decode(property), StandardCharsets.UTF_8);
        assertEquals(skinUrl, JsonParser.parseString(decoded).getAsJsonObject()
                .getAsJsonObject("textures").getAsJsonObject("SKIN")
                .get("url").getAsString());
    }

    @Test
    void invalidPlayerNameFallsBackToBuiltInSteveSkin() throws Exception {
        UUID playerId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        DialogSpec.PlayerHeadIcon playerHead = new DialogSpec.PlayerHeadIcon(
                playerId,
                "Clincded_Xsa_79a4df70",
                "https://textures.minecraft.net/texture/example");
        Method method = NMSDialogFactoryImpl.class.getDeclaredMethod(
                "component", Component.class, String.class, DialogSpec.PlayerHeadIcon.class);
        method.setAccessible(true);

        net.minecraft.network.chat.Component parsed =
                (net.minecraft.network.chat.Component) method.invoke(
                        null, Component.text("Clincded_Xsa_79a4df70"),
                        "minecraft:items/item/armor_stand", playerHead);

        net.minecraft.network.chat.contents.ObjectContents contents = findObjectContents(parsed);
        net.minecraft.network.chat.contents.objects.PlayerSprite sprite =
                (net.minecraft.network.chat.contents.objects.PlayerSprite) contents.contents();
        com.mojang.authlib.GameProfile profile = sprite.player().partialProfile();
        assertTrue(playerHead.usesDefaultSkin());
        assertEquals(playerId, profile.id());
        assertEquals(DialogSpec.PlayerHeadIcon.DEFAULT_PLAYER_NAME, profile.name());
        assertTrue(profile.properties().isEmpty());
        assertEquals(DialogSpec.PlayerHeadIcon.DEFAULT_SKIN_TEXTURE,
                sprite.player().skinPatch().body().orElseThrow().id().toString());
    }

    private static net.minecraft.network.chat.contents.ObjectContents findObjectContents(
            net.minecraft.network.chat.Component component) {
        if (component.getContents() instanceof net.minecraft.network.chat.contents.ObjectContents object) {
            return object;
        }
        for (net.minecraft.network.chat.Component sibling : component.getSiblings()) {
            try {
                return findObjectContents(sibling);
            } catch (AssertionError ignored) {
                // Continue searching the component tree.
            }
        }
        throw new AssertionError("No player object component found");
    }
}
