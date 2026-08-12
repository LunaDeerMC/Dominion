package cn.lunadeer.dominion.utils.dialogui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DialogModelTest {
    private static final Component TEXT = Component.text("test");

    @Test
    void buildsEveryBodyInputTypeAndActionWithoutNmsTypes() {
        DialogSpec.ActionButton callback = new DialogSpec.ActionButton(
                TEXT, Component.text("tooltip"), 200,
                new DialogSpec.CallbackAction((player, response) -> {
                }, new DialogSpec.CallbackOptions(Duration.ofSeconds(30), 2))
        );
        DialogSpec nested = DialogSpec.builder(TEXT, new DialogSpec.Notice(callback)).build();

        DialogPayload additions = new DialogPayload(Map.of(
                "string", DialogPayload.string("value"),
                "boolean", DialogPayload.bool(true),
                "integer", DialogPayload.integer(42),
                "float", DialogPayload.floating(1.5),
                "list", DialogPayload.list(List.of(DialogPayload.string("a"), DialogPayload.string("b"))),
                "compound", DialogPayload.compound(new DialogPayload(Map.of("child", DialogPayload.integer(1))))
        ));

        List<DialogSpec.Action> actions = List.of(
                callback.action(),
                new DialogSpec.CustomClickAction(DialogKey.parse("example:click"), additions),
                new DialogSpec.CommandTemplateAction("say $(value)"),
                new DialogSpec.StaticAction(new DialogSpec.OpenUrl(URI.create("https://example.com"))),
                new DialogSpec.StaticAction(new DialogSpec.RunCommand("/dominion")),
                new DialogSpec.StaticAction(new DialogSpec.SuggestCommand("/dominion ")),
                new DialogSpec.StaticAction(new DialogSpec.ShowDialog(new DialogSpec.InlineReference(nested))),
                new DialogSpec.StaticAction(new DialogSpec.ChangePage(2)),
                new DialogSpec.StaticAction(new DialogSpec.CopyToClipboard("copy")),
                new DialogSpec.StaticAction(new DialogSpec.StaticCustomClick(
                        DialogKey.parse("example:static"), additions))
        );

        DialogSpec.Builder builder = DialogSpec.builder(TEXT,
                        new DialogSpec.MultiAction(
                                actions.stream().map(action -> DialogSpec.ActionButton.of(TEXT, action)).toList(),
                                callback, 3))
                .externalTitle(Component.text("external"))
                .canCloseWithEscape(false)
                .pause(true)
                .afterAction(DialogSpec.AfterAction.WAIT_FOR_RESPONSE)
                .body(new DialogSpec.PlainMessageBody(TEXT, 400))
                .body(new DialogSpec.ItemBody(
                        new ItemStack(Material.STONE),
                        new DialogSpec.PlainMessageBody(Component.text("stone"), 200),
                        true, false, 32, 32))
                .input(new DialogSpec.TextInput(
                        "text", 400, TEXT, true, "initial", 64,
                        new DialogSpec.Multiline(4, 80)))
                .input(new DialogSpec.BooleanInput("enabled", TEXT, true, "yes", "no"))
                .input(new DialogSpec.NumberRangeInput(
                        "amount", 300, TEXT, "value: %s", 0, 10, 5F, 0.5F))
                .input(new DialogSpec.SingleOptionInput(
                        "option", 300,
                        List.of(new DialogSpec.Option("one", Component.text("One"), true),
                                new DialogSpec.Option("two", null, false)),
                        TEXT, true));

        DialogSpec spec = builder.build();
        assertEquals(2, spec.base().body().size());
        assertEquals(4, spec.base().inputs().size());
        assertEquals(10, ((DialogSpec.MultiAction) spec.type()).actions().size());

        assertDoesNotThrow(() -> DialogSpec.builder(TEXT,
                new DialogSpec.Confirmation(callback, callback)).build());
        assertDoesNotThrow(() -> DialogSpec.builder(TEXT,
                new DialogSpec.DialogList(
                        List.of(new DialogSpec.InlineReference(nested),
                                new DialogSpec.KeyedReference(DialogKey.parse("minecraft:test"))),
                        callback, 2, 200)).build());
        assertDoesNotThrow(() -> DialogSpec.builder(TEXT,
                new DialogSpec.ServerLinks(callback, 2, 200)).build());
    }

    @Test
    void validatesUniversalRulesAndReservedKinds() {
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(TEXT, new DialogSpec.Notice(null))
                .input(new DialogSpec.TextInput("same", 100, TEXT, true, "", 10, null))
                .input(new DialogSpec.BooleanInput("same", TEXT, false, "true", "false"))
                .build());
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(TEXT, new DialogSpec.Notice(null))
                .input(new DialogSpec.TextInput("__dominion_token", 100, TEXT, true, "", 10, null))
                .build());
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(TEXT, new DialogSpec.Notice(null))
                .input(new DialogSpec.NumberRangeInput("range", 100, TEXT, "%s", 0, 1, 2F, 1F))
                .build());
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(TEXT, new DialogSpec.Notice(null))
                .input(new DialogSpec.SingleOptionInput("choice", 100,
                        List.of(new DialogSpec.Option("a", TEXT, true),
                                new DialogSpec.Option("b", TEXT, true)), TEXT, true))
                .build());
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(TEXT, new DialogSpec.Notice(
                DialogSpec.ActionButton.of(TEXT, new ReservedExtensionAction()))).build());
        assertDoesNotThrow(() -> DialogSpec.builder(TEXT,
                new ExtensionType(DialogKey.parse("example:future_type"))).build());
    }

    @Test
    void playerHeadButtonKeepsProfileAndAtlasFallbackData() {
        UUID playerId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        DialogSpec.PlayerHeadIcon playerHead = new DialogSpec.PlayerHeadIcon(
                playerId,
                "LunaDeer",
                "https://textures.minecraft.net/texture/example",
                false
        );
        DialogSpec.ActionButton button = DialogSpec.ActionButton.of(
                TEXT, 120, "minecraft:items/item/armor_stand", playerHead, null);

        DialogSpec spec = DialogSpec.builder(TEXT, new DialogSpec.Notice(button)).build();
        DialogSpec.ActionButton encoded = ((DialogSpec.Notice) spec.type()).action();
        assertEquals("minecraft:items/item/armor_stand", encoded.icon());
        assertEquals(playerId, encoded.playerHead().playerId());
        assertEquals("LunaDeer", encoded.playerHead().playerName());
        assertEquals("https://textures.minecraft.net/texture/example",
                encoded.playerHead().skinTextureUrl());
        assertFalse(encoded.playerHead().hat());

        DialogSpec.PlayerHeadIcon invalidSkin = new DialogSpec.PlayerHeadIcon(
                playerId, "LunaDeer", "file:///tmp/skin.png");
        assertEquals("LunaDeer", invalidSkin.playerName());
        assertNull(invalidSkin.skinTextureUrl());
        assertTrue(invalidSkin.usesDefaultSkin());

        DialogSpec.PlayerHeadIcon invalidName = new DialogSpec.PlayerHeadIcon(
                playerId,
                "Clincded_Xsa_79a4df70",
                "https://textures.minecraft.net/texture/example");
        assertEquals(DialogSpec.PlayerHeadIcon.DEFAULT_PLAYER_NAME, invalidName.playerName());
        assertNull(invalidName.skinTextureUrl());
        assertTrue(invalidName.usesDefaultSkin());

        DialogSpec.PlayerHeadIcon missingSkin = new DialogSpec.PlayerHeadIcon(
                playerId, "LunaDeer", null);
        assertEquals("LunaDeer", missingSkin.playerName());
        assertTrue(missingSkin.usesDefaultSkin());
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(
                TEXT, new DialogSpec.Notice(DialogSpec.ActionButton.of(
                        TEXT, 120, "not-an-atlas-path", playerHead, null))).build());
    }

    @Test
    void responseAccessorsAreTypedAndPayloadIsImmutable() {
        DialogPayload payload = new DialogPayload(Map.of(
                "text", DialogPayload.string("hello"),
                "option", DialogPayload.string("one"),
                "bool", DialogPayload.bool(true),
                "boolText", DialogPayload.string("false"),
                "number", DialogPayload.floating(2.25),
                "integer", DialogPayload.integer(3)
        ));
        DialogResponse response = new DialogResponse(payload);

        assertEquals("hello", response.getText("text"));
        assertEquals("one", response.getOption("option"));
        assertEquals(true, response.getBoolean("bool"));
        assertEquals(false, response.getBoolean("boolText"));
        assertEquals(2.25F, response.getFloat("number"));
        assertEquals(3F, response.getFloat("integer"));
        assertNull(response.getText("missing"));
        assertThrows(UnsupportedOperationException.class,
                () -> payload.values().put("bad", DialogPayload.string("bad")));
    }

    private record ExtensionType(DialogKey kind) implements DialogSpec.Type {
    }

    private record ReservedExtensionAction() implements DialogSpec.Action {
        @Override
        public DialogKey kind() {
            return DialogKey.parse("minecraft:static");
        }
    }
}
