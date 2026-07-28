package cn.lunadeer.dominion.v26.nms;

import cn.lunadeer.dominion.utils.dialogui.DialogEncodingResult;
import cn.lunadeer.dominion.utils.dialogui.DialogKey;
import cn.lunadeer.dominion.utils.dialogui.DialogPayload;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NMSDialogFactoryImplTest {
    private static final Component TEXT = Component.text("test");
    private final NMSDialogFactoryImpl factory = new NMSDialogFactoryImpl();

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

    private record ExtensionType() implements DialogSpec.Type {
        @Override
        public DialogKey kind() {
            return DialogKey.parse("example:future");
        }
    }
}
