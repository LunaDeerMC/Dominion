package cn.lunadeer.dominion.utils.dialogui;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Common validation independent from any Minecraft protocol implementation.
 */
public final class DialogModelValidator {
    private DialogModelValidator() {
    }

    public static void validate(DialogSpec dialog) {
        validate(dialog, java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void validate(DialogSpec dialog, Set<DialogSpec> visiting) {
        if (!visiting.add(dialog)) throw new IllegalArgumentException("Dialog references cannot form a cycle");

        if (dialog.base().pause() && dialog.base().afterAction() == DialogSpec.AfterAction.NONE) {
            throw new IllegalArgumentException("A pausing dialog must use an after-action that unpauses");
        }

        Set<String> inputKeys = new HashSet<>();
        for (DialogSpec.Input input : dialog.base().inputs()) {
            validateInput(input);
            if (!inputKeys.add(input.key())) throw new IllegalArgumentException("Duplicate dialog input key: " + input.key());
            if (input.key().startsWith(DialogSpec.RESERVED_INPUT_PREFIX)) {
                throw new IllegalArgumentException("Dialog input key uses reserved prefix: " + input.key());
            }
        }

        for (DialogSpec.Body body : dialog.base().body()) validateBody(body);
        validateType(dialog.type(), visiting);
        visiting.remove(dialog);
    }

    private static void validateBody(DialogSpec.Body body) {
        if (body instanceof DialogSpec.PlainMessageBody message) {
            width(message.width(), "plain message width");
        } else if (body instanceof DialogSpec.ItemBody item) {
            range(item.width(), 1, 256, "item width");
            range(item.height(), 1, 256, "item height");
            if (item.description() != null) validateBody(item.description());
        } else {
            rejectReservedKind(body);
        }
    }

    private static void validateInput(DialogSpec.Input input) {
        if (input.key() == null || input.key().isBlank()) {
            throw new IllegalArgumentException("Dialog input key cannot be blank");
        }
        if (!input.key().matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid dialog input key: " + input.key());
        }
        if (input instanceof DialogSpec.TextInput text) {
            width(text.width(), "text input width");
            if (text.maxLength() < 1) throw new IllegalArgumentException("Text max length must be positive");
            if (text.initial().length() > text.maxLength()) {
                throw new IllegalArgumentException("Initial text exceeds max length for " + text.key());
            }
            if (text.multiline() != null) {
                if (text.multiline().maxLines() != null && text.multiline().maxLines() < 1) {
                    throw new IllegalArgumentException("Multiline max lines must be positive");
                }
                if (text.multiline().height() != null) {
                    range(text.multiline().height(), 1, 512, "multiline height");
                }
            }
        } else if (input instanceof DialogSpec.BooleanInput) {
            // Boolean inputs have no universal numeric or size constraints.
        } else if (input instanceof DialogSpec.NumberRangeInput range) {
            width(range.width(), "number range width");
            if (!Float.isFinite(range.start()) || !Float.isFinite(range.end())) {
                throw new IllegalArgumentException("Number range bounds must be finite");
            }
            float min = Math.min(range.start(), range.end());
            float max = Math.max(range.start(), range.end());
            if (range.initial() != null && (range.initial() < min || range.initial() > max)) {
                throw new IllegalArgumentException("Initial number is outside range for " + range.key());
            }
            if (range.step() != null && (!Float.isFinite(range.step()) || range.step() <= 0)) {
                throw new IllegalArgumentException("Number range step must be positive");
            }
        } else if (input instanceof DialogSpec.SingleOptionInput option) {
            width(option.width(), "single option width");
            if (option.options().isEmpty()) throw new IllegalArgumentException("Single option input cannot be empty");
            Set<String> ids = new HashSet<>();
            long initial = 0;
            for (DialogSpec.Option entry : option.options()) {
                if (!ids.add(entry.id())) throw new IllegalArgumentException("Duplicate option id: " + entry.id());
                if (entry.initial()) initial++;
            }
            if (initial > 1) throw new IllegalArgumentException("Single option input has multiple initial values");
        } else {
            rejectReservedKind(input);
        }
    }

    private static void validateType(DialogSpec.Type type, Set<DialogSpec> visiting) {
        if (type instanceof DialogSpec.Notice notice) {
            if (notice.action() != null) validateButton(notice.action(), visiting);
        } else if (type instanceof DialogSpec.Confirmation confirmation) {
            validateButton(confirmation.yes(), visiting);
            validateButton(confirmation.no(), visiting);
        } else if (type instanceof DialogSpec.MultiAction multi) {
            if (multi.actions().isEmpty()) throw new IllegalArgumentException("Multi-action dialog cannot be empty");
            if (multi.columns() < 1) throw new IllegalArgumentException("Dialog columns must be positive");
            multi.actions().forEach(button -> validateButton(button, visiting));
            if (multi.exitAction() != null) validateButton(multi.exitAction(), visiting);
        } else if (type instanceof DialogSpec.DialogList list) {
            if (list.dialogs().isEmpty()) throw new IllegalArgumentException("Dialog list cannot be empty");
            if (list.columns() < 1) throw new IllegalArgumentException("Dialog columns must be positive");
            width(list.buttonWidth(), "dialog-list button width");
            for (DialogSpec.Reference reference : list.dialogs()) validateReference(reference, visiting);
            if (list.exitAction() != null) validateButton(list.exitAction(), visiting);
        } else if (type instanceof DialogSpec.ServerLinks links) {
            if (links.columns() < 1) throw new IllegalArgumentException("Dialog columns must be positive");
            width(links.buttonWidth(), "server-links button width");
            if (links.exitAction() != null) validateButton(links.exitAction(), visiting);
        } else {
            rejectReservedKind(type);
        }
    }

    private static void validateButton(DialogSpec.ActionButton button, Set<DialogSpec> visiting) {
        width(button.width(), "button width");
        if (button.icon() != null && !button.icon().isBlank()) {
            DialogSpritePath.parse(button.icon());
        }
        if (button.action() != null) {
            DialogSpec.Action action = button.action();
            if (!(action instanceof DialogSpec.CallbackAction)
                    && !(action instanceof DialogSpec.CustomClickAction)
                    && !(action instanceof DialogSpec.CommandTemplateAction)
                    && !(action instanceof DialogSpec.StaticAction)) {
                rejectReservedKind(action);
            }
            if (action instanceof DialogSpec.CustomClickAction custom) {
                validateCustomClick(custom.id(), custom.additions());
            }
            if (action instanceof DialogSpec.StaticAction staticAction
                    && staticAction.value() instanceof DialogSpec.ShowDialog show) {
                validateReference(show.dialog(), visiting);
            }
            if (action instanceof DialogSpec.StaticAction staticAction
                    && staticAction.value() instanceof DialogSpec.StaticCustomClick custom) {
                validateCustomClick(custom.id(), custom.payload());
            }
            if (action instanceof DialogSpec.StaticAction staticAction
                    && staticAction.value() instanceof DialogSpec.ChangePage page
                    && page.page() < 1) {
                throw new IllegalArgumentException("Book page must be positive");
            }
        }
    }

    private static void validateReference(DialogSpec.Reference reference, Set<DialogSpec> visiting) {
        if (reference instanceof DialogSpec.InlineReference inline) validate(inline.dialog(), visiting);
    }

    private static void width(int value, String name) {
        range(value, 1, 1024, name);
    }

    private static void range(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be in [" + min + ", " + max + "]");
        }
    }

    private static void rejectReservedKind(DialogSpec.Element element) {
        String namespace = element.kind().namespace();
        if ("minecraft".equals(namespace) || "dominion".equals(namespace)) {
            throw new IllegalArgumentException("Extension element cannot use reserved kind " + element.kind());
        }
    }

    private static void validateCustomClick(DialogKey id, DialogPayload additions) {
        if (DialogCallbackRegistry.CALLBACK_ACTION_ID.equals(id.toString())) {
            throw new IllegalArgumentException("Custom click uses Dominion's reserved callback id");
        }
        if (containsReservedField(additions)) {
            throw new IllegalArgumentException("Custom click payload uses a reserved Dominion field");
        }
    }

    private static boolean containsReservedField(DialogPayload payload) {
        for (var entry : payload.values().entrySet()) {
            if (entry.getKey().startsWith(DialogSpec.RESERVED_INPUT_PREFIX)) return true;
            if (entry.getValue() instanceof DialogPayload.CompoundValue compound
                    && containsReservedField(compound.value())) return true;
            if (entry.getValue() instanceof DialogPayload.ListValue list) {
                for (DialogPayload.Value value : list.values()) {
                    if (value instanceof DialogPayload.CompoundValue compound
                            && containsReservedField(compound.value())) return true;
                }
            }
        }
        return false;
    }
}
