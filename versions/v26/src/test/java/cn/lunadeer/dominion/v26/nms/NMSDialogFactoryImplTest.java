package cn.lunadeer.dominion.v26.nms;

import cn.lunadeer.dominion.utils.dialogui.DialogEncodingResult;
import cn.lunadeer.dominion.utils.dialogui.DialogKey;
import cn.lunadeer.dominion.utils.dialogui.DialogPayload;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.Bootstrap;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NMSDialogFactoryImplTest {
    private static final Component TEXT = Component.text("test");
    private final NMSDialogFactoryImpl factory = new NMSDialogFactoryImpl();

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CraftRegistry.setMinecraftRegistry(net.minecraft.core.RegistryAccess.EMPTY);
    }

    @Test
    void registryRecognizesEveryCoreBodyInputTypeAndActionKind() {
        DialogSpec.ActionButton callback = DialogSpec.ActionButton.of(
                TEXT, new DialogSpec.CallbackAction((player, response) -> {
                }));
        DialogSpec inline = DialogSpec.builder(TEXT, new DialogSpec.Notice(callback)).build();
        List<DialogSpec.ActionButton> actions = List.of(
                callback,
                DialogSpec.ActionButton.of(TEXT, new DialogSpec.CustomClickAction(
                        DialogKey.parse("example:custom"), DialogPayload.EMPTY)),
                DialogSpec.ActionButton.of(TEXT, new DialogSpec.CommandTemplateAction("say hello")),
                DialogSpec.ActionButton.of(TEXT, new DialogSpec.StaticAction(
                        new DialogSpec.ShowDialog(new DialogSpec.InlineReference(inline))))
        );
        ItemStack item = mock(ItemStack.class);
        when(item.clone()).thenReturn(item);

        DialogSpec complete = DialogSpec.builder(TEXT, new DialogSpec.MultiAction(actions, callback, 2))
                .body(new DialogSpec.PlainMessageBody(TEXT, 200))
                .body(new DialogSpec.ItemBody(item, null,
                        true, true, 32, 32))
                .input(new DialogSpec.TextInput("text", 200, TEXT, true, "", 32, null))
                .input(new DialogSpec.BooleanInput("boolean", TEXT, false, "true", "false"))
                .input(new DialogSpec.NumberRangeInput("number", 200, TEXT, "%s",
                        0, 10, 5F, 1F))
                .input(new DialogSpec.SingleOptionInput("option", 200,
                        List.of(new DialogSpec.Option("one", TEXT, true)), TEXT, true))
                .build();
        assertTrue(factory.validateModel(complete).successful());

        assertTrue(factory.validateModel(DialogSpec.builder(TEXT,
                new DialogSpec.Confirmation(callback, callback)).build()).successful());
        assertTrue(factory.validateModel(DialogSpec.builder(TEXT,
                new DialogSpec.DialogList(List.of(new DialogSpec.InlineReference(inline)),
                        callback, 1, 200)).build()).successful());
        assertTrue(factory.validateModel(DialogSpec.builder(TEXT,
                new DialogSpec.ServerLinks(callback, 1, 200)).build()).successful());
    }

    @Test
    void unregisteredExtensionKindReturnsDiagnosticResult() {
        DialogSpec spec = DialogSpec.builder(TEXT, new ExtensionType()).build();
        DialogEncodingResult result = factory.validateModel(spec);
        assertFalse(result.successful());
        assertTrue(result.message().contains("example:future"));
    }

    @Test
    void mapsEveryCoreAfterActionWithoutChangingCloseSemantics() {
        assertEquals(net.minecraft.server.dialog.DialogAction.CLOSE,
                NMSDialogFactoryImpl.afterAction(DialogSpec.AfterAction.CLOSE));
        assertEquals(net.minecraft.server.dialog.DialogAction.NONE,
                NMSDialogFactoryImpl.afterAction(DialogSpec.AfterAction.NONE));
        assertEquals(net.minecraft.server.dialog.DialogAction.WAIT_FOR_RESPONSE,
                NMSDialogFactoryImpl.afterAction(DialogSpec.AfterAction.WAIT_FOR_RESPONSE));
    }

    @Test
    void preservesAtlasObjectComponentWhenUsingTheServerJsonCodec() {
        String json = "{\"text\":\"\",\"extra\":["
                + "{\"object\":\"atlas\",\"atlas\":\"minecraft:blocks\","
                + "\"sprite\":\"minecraft:block/grass_block_side\"},\" \","
                + "{\"text\":\"Grass\"}]}";
        net.minecraft.network.chat.Component parsed = ComponentSerialization.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertTrue(parsed.getSiblings().getFirst().getContents()
                instanceof net.minecraft.network.chat.contents.ObjectContents);
    }

    @Test
    void encodesAtlasObjectWithTheExpectedIdentifiersThroughCraftChatMessage() throws Exception {
        Method method = NMSDialogFactoryImpl.class.getDeclaredMethod(
                "component", Component.class, String.class);
        method.setAccessible(true);
        net.minecraft.network.chat.Component parsed = (net.minecraft.network.chat.Component) method.invoke(
                null, Component.text("Grass"), "minecraft:blocks/block/grass_block_side");

        net.minecraft.network.chat.contents.ObjectContents contents = findObjectContents(parsed);
        net.minecraft.network.chat.contents.objects.AtlasSprite sprite =
                (net.minecraft.network.chat.contents.objects.AtlasSprite) contents.contents();
        assertEquals("minecraft:blocks", sprite.atlas().toString());
        assertEquals("minecraft:block/grass_block_side", sprite.sprite().toString());
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
        throw new AssertionError("No object component found");
    }

    private record ExtensionType() implements DialogSpec.Type {
        @Override
        public DialogKey kind() {
            return DialogKey.parse("example:future");
        }
    }
}
