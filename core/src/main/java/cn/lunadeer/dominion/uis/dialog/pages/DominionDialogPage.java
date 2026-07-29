package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.utils.LegacyToMiniMessage;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dominion-only visual composition helper. It deliberately has no slots,
 * inventory clicks, or configurable layout: every page declares native
 * dialog bodies, inputs, and actions explicitly.
 */
final class DominionDialogPage {
    static final int CONTENT_WIDTH = 336;
    static final int THREE_COLUMN_WIDTH = 108;
    static final int TWO_COLUMN_WIDTH = 164;
    static final int WIDE_BUTTON_WIDTH = 336;
    static final int LIST_PAGE_SIZE = 8;
    static final int FLAG_PAGE_SIZE = 8;

    private final DialogUiText text;
    private final String menuId;
    private final Component title;
    private final List<DialogSpec.Body> bodies = new ArrayList<>();
    private final List<DialogSpec.Input> inputs = new ArrayList<>();
    private final List<DialogSpec.ActionButton> actions = new ArrayList<>();
    private int listActionCount;
    private DialogSpec.AfterAction afterAction = DialogSpec.AfterAction.CLOSE;
    private DialogSpec.ActionButton exitAction;

    DominionDialogPage(DialogUiText text, String menuId, Map<String, ?> titleValues) {
        this.text = text;
        this.menuId = menuId;
        this.title = component(render(text.text("menus." + menuId + ".title"), titleValues));
    }

    DominionDialogPage message(String value) {
        bodies.add(new DialogSpec.PlainMessageBody(component(value), CONTENT_WIDTH));
        return this;
    }

    DominionDialogPage messageKey(String key, Map<String, ?> values) {
        return message(render(text.text(key), values));
    }

    DominionDialogPage summary(String element, Map<String, ?> values, Material material) {
        String path = itemPath(element);
        Component label = component(render(text.text(path + ".name"), values));
        List<Component> lore = lore(path, values);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(label);
        if (!lore.isEmpty()) meta.lore(lore);
        item.setItemMeta(meta);
        bodies.add(new DialogSpec.ItemBody(
                item,
                lore.isEmpty() ? null : new DialogSpec.PlainMessageBody(join(lore), CONTENT_WIDTH - 56),
                true,
                true,
                48,
                48
        ));
        return this;
    }

    DominionDialogPage textInput(String key, String labelKey, String initial, int maxLength) {
        inputs.add(new DialogSpec.TextInput(
                key,
                CONTENT_WIDTH,
                component(text.text(labelKey)),
                true,
                initial == null ? "" : initial,
                maxLength,
                null
        ));
        return this;
    }

    DominionDialogPage multilineInput(String key, String labelKey, String initial, int maxLength) {
        inputs.add(new DialogSpec.TextInput(
                key,
                CONTENT_WIDTH,
                component(text.text(labelKey)),
                true,
                initial == null ? "" : initial,
                maxLength,
                new DialogSpec.Multiline(3, 54)
        ));
        return this;
    }

    DominionDialogPage booleanInput(String key, Component label, boolean initial) {
        inputs.add(new DialogSpec.BooleanInput(key, label, initial, "true", "false"));
        return this;
    }

    DominionDialogPage optionInput(String key, String labelKey, List<DialogSpec.Option> options) {
        inputs.add(new DialogSpec.SingleOptionInput(
                key,
                CONTENT_WIDTH,
                options,
                component(text.text(labelKey)),
                true
        ));
        return this;
    }

    DominionDialogPage action(String element, Map<String, ?> values, int width, DialogSpec.Callback callback) {
        actions.add(button(element, values, width, callback));
        return this;
    }

    DominionDialogPage action(Component label, Component tooltip, int width, DialogSpec.Callback callback) {
        actions.add(new DialogSpec.ActionButton(
                label,
                tooltip,
                width,
                new DialogSpec.CallbackAction(callback)
        ));
        return this;
    }

    DominionDialogPage listAction(Component name, Component tooltip, DialogSpec.Callback callback) {
        Component label = name.colorIfAbsent(NamedTextColor.WHITE)
                .append(Component.text("  ›", NamedTextColor.DARK_GRAY));
        action(label, tooltip, TWO_COLUMN_WIDTH, callback);
        listActionCount++;
        return this;
    }

    DominionDialogPage completeListRow() {
        if (listActionCount % 2 != 0) {
            actions.add(new DialogSpec.ActionButton(
                    Component.text(" "), null, TWO_COLUMN_WIDTH, null));
            listActionCount++;
        }
        return this;
    }

    DominionDialogPage keepOpenAfterAction() {
        afterAction = DialogSpec.AfterAction.NONE;
        return this;
    }

    DominionDialogPage backExit(DialogSpec.Callback back) {
        exitAction = button("back", Map.of(), WIDE_BUTTON_WIDTH, back);
        return this;
    }

    DialogSpec.ActionButton button(String element, Map<String, ?> values, int width, DialogSpec.Callback callback) {
        String path = itemPath(element);
        List<Component> lore = lore(path, values);
        return new DialogSpec.ActionButton(
                component(render(text.text(path + ".name"), values)),
                lore.isEmpty() ? null : join(lore),
                width,
                new DialogSpec.CallbackAction(callback)
        );
    }

    DialogSpec build(int columns, DialogSpec.ActionButton exit) {
        DialogSpec.ActionButton resolvedExit = exit == null ? exitAction : exit;
        DialogSpec.Type type = actions.isEmpty()
                ? new DialogSpec.Notice(resolvedExit)
                : new DialogSpec.MultiAction(actions, resolvedExit, columns);
        DialogSpec.Builder builder = DialogSpec.builder(title, type)
                .afterAction(afterAction);
        bodies.forEach(builder::body);
        inputs.forEach(builder::input);
        return builder.build();
    }

    DialogSpec buildList() {
        return build(2, null);
    }

    DialogSpec buildConfirmation(DialogSpec.ActionButton yes, DialogSpec.ActionButton no) {
        DialogSpec.Builder builder = DialogSpec.builder(title, new DialogSpec.Confirmation(yes, no))
                .afterAction(DialogSpec.AfterAction.CLOSE);
        bodies.forEach(builder::body);
        inputs.forEach(builder::input);
        return builder.build();
    }

    private String itemPath(String element) {
        String menuPath = "menus." + menuId + ".items." + element;
        return text.contains(menuPath + ".name") ? menuPath : "buttons." + element;
    }

    private List<Component> lore(String path, Map<String, ?> values) {
        return text.textList(path + ".lore").stream()
                .map(line -> component(render(line, values)))
                .toList();
    }

    static Component component(String value) {
        return LegacyToMiniMessage.parse(value == null ? "" : value);
    }

    static String render(String template, Map<String, ?> values) {
        return DialogTextRenderer.replaceNamed(template, values);
    }

    private static Component join(List<Component> lines) {
        Component result = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) result = result.append(Component.newline());
            result = result.append(lines.get(index));
        }
        return result;
    }
}
