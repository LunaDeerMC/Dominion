package cn.lunadeer.dominion.v1_21_8.nms;

import cn.lunadeer.dominion.nms.NMSDialogFactory;
import cn.lunadeer.dominion.utils.dialogui.DialogCallbackRegistry;
import cn.lunadeer.dominion.utils.dialogui.DialogEncodingResult;
import cn.lunadeer.dominion.utils.dialogui.DialogKey;
import cn.lunadeer.dominion.utils.dialogui.DialogModelValidator;
import cn.lunadeer.dominion.utils.dialogui.DialogPayload;
import cn.lunadeer.dominion.utils.dialogui.DialogSessionContext;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.ConfirmationDialog;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.CommandTemplate;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.action.ParsedTemplate;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.NumberRangeInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minecraft 1.21.8 implementation of the version-independent dialog model.
 */
public final class NMSDialogFactoryImpl implements NMSDialogFactory {
    private final EncoderRegistry encoders = new EncoderRegistry();

    @Override
    public boolean isSupported() {
        return Bukkit.getMinecraftVersion().startsWith("1.21.8");
    }

    @Override
    public DialogEncodingResult validate(DialogSpec dialog) {
        if (!isSupported()) return DialogEncodingResult.unsupported("Dialog backend requires Minecraft 1.21.8");
        return validateModel(dialog);
    }

    DialogEncodingResult validateModel(DialogSpec dialog) {
        try {
            DialogModelValidator.validate(dialog);
            encoders.validate(dialog);
            return DialogEncodingResult.success();
        } catch (Exception exception) {
            return DialogEncodingResult.unsupported(exception.getMessage());
        }
    }

    @Override
    public DialogEncodingResult show(Player player, DialogSpec dialog, DialogSessionContext context) {
        DialogEncodingResult validation = validate(dialog);
        if (!validation.successful()) return validation;

        DialogCallbackRegistry.INSTANCE.beginRender(player.getUniqueId(), context);
        try {
            EncodeContext encodeContext = new EncodeContext(player, context);
            Dialog encoded = encoders.dialog(dialog, encodeContext);
            getServerPlayer(player).connection.send(new ClientboundShowDialogPacket(Holder.direct(encoded)));
            return DialogEncodingResult.success();
        } catch (Exception exception) {
            DialogCallbackRegistry.INSTANCE.invalidate(player.getUniqueId());
            return DialogEncodingResult.unsupported(exception.getMessage());
        }
    }

    @Override
    public void close(Player player) {
        DialogCallbackRegistry.INSTANCE.invalidate(player.getUniqueId());
        getServerPlayer(player).connection.send(ClientboundClearDialogPacket.INSTANCE);
    }

    private static ServerPlayer getServerPlayer(Player player) {
        try {
            return (ServerPlayer) player.getClass().getMethod("getHandle").invoke(player);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to obtain ServerPlayer", exception);
        }
    }

    private record EncodeContext(Player player, DialogSessionContext session) {
    }

    private final class EncoderRegistry {
        private final Map<DialogKey, BodyEncoder> bodyEncoders = new LinkedHashMap<>();
        private final Map<DialogKey, InputEncoder> inputEncoders = new LinkedHashMap<>();
        private final Map<DialogKey, TypeEncoder> typeEncoders = new LinkedHashMap<>();
        private final Map<DialogKey, ActionEncoder> actionEncoders = new LinkedHashMap<>();

        private EncoderRegistry() {
            bodyEncoders.put(DialogSpec.PlainMessageBody.KIND,
                    (body, context) -> plainMessage((DialogSpec.PlainMessageBody) body));
            bodyEncoders.put(DialogSpec.ItemBody.KIND, (body, context) -> itemBody((DialogSpec.ItemBody) body));

            inputEncoders.put(DialogSpec.TextInput.KIND, (input, context) -> textInput((DialogSpec.TextInput) input));
            inputEncoders.put(DialogSpec.BooleanInput.KIND, (input, context) -> booleanInput((DialogSpec.BooleanInput) input));
            inputEncoders.put(DialogSpec.NumberRangeInput.KIND,
                    (input, context) -> numberRangeInput((DialogSpec.NumberRangeInput) input));
            inputEncoders.put(DialogSpec.SingleOptionInput.KIND,
                    (input, context) -> singleOptionInput((DialogSpec.SingleOptionInput) input));

            typeEncoders.put(DialogSpec.Notice.KIND, (spec, type, context) -> notice(spec, (DialogSpec.Notice) type, context));
            typeEncoders.put(DialogSpec.Confirmation.KIND,
                    (spec, type, context) -> confirmation(spec, (DialogSpec.Confirmation) type, context));
            typeEncoders.put(DialogSpec.MultiAction.KIND,
                    (spec, type, context) -> multiAction(spec, (DialogSpec.MultiAction) type, context));
            typeEncoders.put(DialogSpec.DialogList.KIND,
                    (spec, type, context) -> dialogList(spec, (DialogSpec.DialogList) type, context));
            typeEncoders.put(DialogSpec.ServerLinks.KIND,
                    (spec, type, context) -> serverLinks(spec, (DialogSpec.ServerLinks) type, context));

            actionEncoders.put(DialogSpec.CallbackAction.KIND,
                    (action, context) -> callback((DialogSpec.CallbackAction) action, context));
            actionEncoders.put(DialogSpec.CustomClickAction.KIND,
                    (action, context) -> customClick((DialogSpec.CustomClickAction) action));
            actionEncoders.put(DialogSpec.CommandTemplateAction.KIND,
                    (action, context) -> commandTemplate((DialogSpec.CommandTemplateAction) action));
            actionEncoders.put(DialogSpec.StaticAction.KIND,
                    (action, context) -> staticAction((DialogSpec.StaticAction) action, context));
        }

        private void validate(DialogSpec spec) {
            require(typeEncoders, spec.type().kind(), "dialog type");
            for (DialogSpec.Body body : spec.base().body()) require(bodyEncoders, body.kind(), "dialog body");
            for (DialogSpec.Input input : spec.base().inputs()) require(inputEncoders, input.kind(), "dialog input");
            validateTypeActions(spec.type());
        }

        private void validateTypeActions(DialogSpec.Type type) {
            if (type instanceof DialogSpec.Notice notice) {
                validateButton(notice.action());
            } else if (type instanceof DialogSpec.Confirmation confirmation) {
                validateButton(confirmation.yes());
                validateButton(confirmation.no());
            } else if (type instanceof DialogSpec.MultiAction multi) {
                multi.actions().forEach(this::validateButton);
                validateButton(multi.exitAction());
            } else if (type instanceof DialogSpec.DialogList list) {
                validateButton(list.exitAction());
                list.dialogs().forEach(this::validateReference);
            } else if (type instanceof DialogSpec.ServerLinks links) {
                validateButton(links.exitAction());
            }
        }

        private void validateButton(DialogSpec.ActionButton button) {
            if (button == null || button.action() == null) return;
            require(actionEncoders, button.action().kind(), "dialog action");
            if (button.action() instanceof DialogSpec.StaticAction action
                    && action.value() instanceof DialogSpec.ShowDialog show) {
                validateReference(show.dialog());
            }
        }

        private void validateReference(DialogSpec.Reference reference) {
            if (reference instanceof DialogSpec.InlineReference inline) validate(inline.dialog());
        }

        private <T> void require(Map<DialogKey, T> map, DialogKey key, String element) {
            if (!map.containsKey(key)) {
                throw new IllegalArgumentException("Minecraft 1.21.8 does not support " + element + " kind " + key);
            }
        }

        private Dialog dialog(DialogSpec spec, EncodeContext context) {
            TypeEncoder encoder = typeEncoders.get(spec.type().kind());
            if (encoder == null) throw new IllegalArgumentException("Unsupported dialog type: " + spec.type().kind());
            return encoder.encode(spec, spec.type(), context);
        }

        private CommonDialogData common(DialogSpec.Base base, EncodeContext context) {
            List<DialogBody> bodies = base.body().stream().map(body -> body(body, context)).toList();
            List<Input> inputs = base.inputs().stream()
                    .map(input -> new Input(input.key(), input(input, context)))
                    .toList();
            return new CommonDialogData(
                    component(base.title()),
                    Optional.ofNullable(base.externalTitle()).map(NMSDialogFactoryImpl::component),
                    base.canCloseWithEscape(),
                    base.pause(),
                    afterAction(base.afterAction()),
                    bodies,
                    inputs
            );
        }

        private DialogBody body(DialogSpec.Body body, EncodeContext context) {
            BodyEncoder encoder = bodyEncoders.get(body.kind());
            if (encoder == null) throw new IllegalArgumentException("Unsupported dialog body: " + body.kind());
            return encoder.encode(body, context);
        }

        private InputControl input(DialogSpec.Input input, EncodeContext context) {
            InputEncoder encoder = inputEncoders.get(input.kind());
            if (encoder == null) throw new IllegalArgumentException("Unsupported dialog input: " + input.kind());
            return encoder.encode(input, context);
        }

        private Action action(DialogSpec.Action action, EncodeContext context) {
            if (action == null) return null;
            ActionEncoder encoder = actionEncoders.get(action.kind());
            if (encoder == null) throw new IllegalArgumentException("Unsupported dialog action: " + action.kind());
            return encoder.encode(action, context);
        }

        private ActionButton button(DialogSpec.ActionButton button, EncodeContext context) {
            if (button == null) return null;
            CommonButtonData common = new CommonButtonData(
                    component(button.label()),
                    Optional.ofNullable(button.tooltip()).map(NMSDialogFactoryImpl::component),
                    button.width()
            );
            return new ActionButton(common, Optional.ofNullable(action(button.action(), context)));
        }

        private Dialog notice(DialogSpec spec, DialogSpec.Notice type, EncodeContext context) {
            return new NoticeDialog(common(spec.base(), context),
                    type.action() == null ? NoticeDialog.DEFAULT_ACTION : button(type.action(), context));
        }

        private Dialog confirmation(DialogSpec spec, DialogSpec.Confirmation type, EncodeContext context) {
            return new ConfirmationDialog(common(spec.base(), context),
                    button(type.yes(), context), button(type.no(), context));
        }

        private Dialog multiAction(DialogSpec spec, DialogSpec.MultiAction type, EncodeContext context) {
            return new MultiActionDialog(
                    common(spec.base(), context),
                    type.actions().stream().map(value -> button(value, context)).toList(),
                    Optional.ofNullable(type.exitAction()).map(value -> button(value, context)),
                    type.columns()
            );
        }

        private Dialog dialogList(DialogSpec spec, DialogSpec.DialogList type, EncodeContext context) {
            List<Holder<Dialog>> holders = type.dialogs().stream().map(value -> reference(value, context)).toList();
            return new DialogListDialog(
                    common(spec.base(), context),
                    HolderSet.direct(holders),
                    Optional.ofNullable(type.exitAction()).map(value -> button(value, context)),
                    type.columns(),
                    type.buttonWidth()
            );
        }

        private Dialog serverLinks(DialogSpec spec, DialogSpec.ServerLinks type, EncodeContext context) {
            return new ServerLinksDialog(
                    common(spec.base(), context),
                    Optional.ofNullable(type.exitAction()).map(value -> button(value, context)),
                    type.columns(),
                    type.buttonWidth()
            );
        }

        private Holder<Dialog> reference(DialogSpec.Reference reference, EncodeContext context) {
            if (reference instanceof DialogSpec.InlineReference inline) {
                return Holder.direct(dialog(inline.dialog(), context));
            }
            if (reference instanceof DialogSpec.KeyedReference keyed) {
                ServerPlayer serverPlayer = getServerPlayer(context.player());
                Registry<Dialog> registry = serverPlayer.level().getServer().registryAccess().lookupOrThrow(Registries.DIALOG);
                return registry.get(identifier(keyed.key()))
                        .map(holder -> (Holder<Dialog>) holder)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown dialog registry key " + keyed.key()));
            }
            throw new IllegalArgumentException("Unsupported dialog reference " + reference.getClass().getName());
        }

        private Action callback(DialogSpec.CallbackAction callback, EncodeContext context) {
            String token = DialogCallbackRegistry.INSTANCE.register(
                    context.player().getUniqueId(), context.session(), callback.callback(), callback.options());
            CompoundTag additions = new CompoundTag();
            additions.putString(DialogCallbackRegistry.TOKEN_KEY, token);
            return new CustomAll(ResourceLocation.parse(DialogCallbackRegistry.CALLBACK_ACTION_ID), Optional.of(additions));
        }

        private Action customClick(DialogSpec.CustomClickAction action) {
            CompoundTag additions = compound(action.additions());
            return new CustomAll(identifier(action.id()), additions.isEmpty() ? Optional.empty() : Optional.of(additions));
        }

        private Action commandTemplate(DialogSpec.CommandTemplateAction action) {
            ParsedTemplate parsed = ParsedTemplate.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(action.template()))
                    .result().orElseThrow(() -> new IllegalArgumentException("Invalid command template"));
            return new CommandTemplate(parsed);
        }

        private Action staticAction(DialogSpec.StaticAction action, EncodeContext context) {
            DialogSpec.StaticClick value = action.value();
            ClickEvent click = switch (value) {
                case DialogSpec.OpenUrl open -> new ClickEvent.OpenUrl(open.uri());
                case DialogSpec.RunCommand command -> new ClickEvent.RunCommand(command.command());
                case DialogSpec.SuggestCommand command -> new ClickEvent.SuggestCommand(command.command());
                case DialogSpec.ShowDialog show -> new ClickEvent.ShowDialog(reference(show.dialog(), context));
                case DialogSpec.ChangePage page -> new ClickEvent.ChangePage(page.page());
                case DialogSpec.CopyToClipboard copy -> new ClickEvent.CopyToClipboard(copy.value());
                case DialogSpec.StaticCustomClick custom -> new ClickEvent.Custom(
                        identifier(custom.id()), Optional.of(compound(custom.payload())));
                default -> throw new IllegalArgumentException("Unsupported static click " + value.getClass().getName());
            };
            return new StaticAction(click);
        }
    }

    static net.minecraft.server.dialog.DialogAction afterAction(DialogSpec.AfterAction value) {
        return switch (value) {
            case CLOSE -> net.minecraft.server.dialog.DialogAction.CLOSE;
            case NONE -> net.minecraft.server.dialog.DialogAction.NONE;
            case WAIT_FOR_RESPONSE -> net.minecraft.server.dialog.DialogAction.WAIT_FOR_RESPONSE;
        };
    }

    private static PlainMessage plainMessage(DialogSpec.PlainMessageBody body) {
        return new PlainMessage(component(body.contents()), body.width());
    }

    private static ItemBody itemBody(DialogSpec.ItemBody body) {
        return new ItemBody(
                CraftItemStack.asNMSCopy(body.item()),
                Optional.ofNullable(body.description()).map(NMSDialogFactoryImpl::plainMessage),
                body.showDecorations(), body.showTooltip(), body.width(), body.height());
    }

    private static TextInput textInput(DialogSpec.TextInput input) {
        Optional<TextInput.MultilineOptions> multiline = Optional.ofNullable(input.multiline())
                .map(value -> new TextInput.MultilineOptions(Optional.ofNullable(value.maxLines()), Optional.ofNullable(value.height())));
        return new TextInput(input.width(), component(input.label()), input.labelVisible(),
                input.initial(), input.maxLength(), multiline);
    }

    private static BooleanInput booleanInput(DialogSpec.BooleanInput input) {
        return new BooleanInput(component(input.label()), input.initial(), input.onTrue(), input.onFalse());
    }

    private static NumberRangeInput numberRangeInput(DialogSpec.NumberRangeInput input) {
        return new NumberRangeInput(input.width(), component(input.label()), input.labelFormat(),
                new NumberRangeInput.RangeInfo(input.start(), input.end(),
                        Optional.ofNullable(input.initial()), Optional.ofNullable(input.step())));
    }

    private static SingleOptionInput singleOptionInput(DialogSpec.SingleOptionInput input) {
        List<SingleOptionInput.Entry> entries = input.options().stream()
                .map(value -> new SingleOptionInput.Entry(value.id(),
                        Optional.ofNullable(value.display()).map(NMSDialogFactoryImpl::component), value.initial()))
                .toList();
        return new SingleOptionInput(input.width(), entries, component(input.label()), input.labelVisible());
    }

    private static net.minecraft.network.chat.Component component(Component component) {
        return CraftChatMessage.fromJSON(GsonComponentSerializer.gson().serialize(component));
    }

    private static ResourceLocation identifier(DialogKey key) {
        return ResourceLocation.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static CompoundTag compound(DialogPayload payload) {
        CompoundTag tag = new CompoundTag();
        payload.values().forEach((key, value) -> tag.put(key, value(value)));
        return tag;
    }

    private static Tag value(DialogPayload.Value value) {
        if (value instanceof DialogPayload.StringValue text) return net.minecraft.nbt.StringTag.valueOf(text.value());
        if (value instanceof DialogPayload.BooleanValue bool) return net.minecraft.nbt.ByteTag.valueOf(bool.value());
        if (value instanceof DialogPayload.IntegerValue integer) return net.minecraft.nbt.LongTag.valueOf(integer.value());
        if (value instanceof DialogPayload.FloatValue floating) return net.minecraft.nbt.DoubleTag.valueOf(floating.value());
        if (value instanceof DialogPayload.CompoundValue compound) return compound(compound.value());
        if (value instanceof DialogPayload.ListValue list) {
            ListTag result = new ListTag();
            for (DialogPayload.Value entry : list.values()) {
                if (!result.add(value(entry))) throw new IllegalArgumentException("NBT list payload values must have one common type");
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported dialog payload value " + value.getClass().getName());
    }

    @FunctionalInterface
    private interface BodyEncoder { DialogBody encode(DialogSpec.Body body, EncodeContext context); }
    @FunctionalInterface
    private interface InputEncoder { InputControl encode(DialogSpec.Input input, EncodeContext context); }
    @FunctionalInterface
    private interface TypeEncoder { Dialog encode(DialogSpec spec, DialogSpec.Type type, EncodeContext context); }
    @FunctionalInterface
    private interface ActionEncoder { Action encode(DialogSpec.Action action, EncodeContext context); }
}
