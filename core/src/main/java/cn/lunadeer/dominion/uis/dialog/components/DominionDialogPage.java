package cn.lunadeer.dominion.uis.dialog.components;

import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.utils.LegacyToMiniMessage;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dominion-only visual composition helper. It deliberately has no slots,
 * inventory clicks, or slot-based layout: every page declares native dialog
 * bodies, inputs, actions, and its own protocol layout explicitly. Button
 * icons are resolved from the shared DialogUI layout configuration.
 */
public final class DominionDialogPage {
    private final DialogUiText text;
    private final String menuId;
    private final Component title;
    private final DialogPageLayout layout;
    private final DialogListStyle listStyle;
    private final List<DialogSpec.Body> bodies = new ArrayList<>();
    private final List<DialogSpec.Input> inputs = new ArrayList<>();
    private final List<DialogSpec.ActionButton> actions = new ArrayList<>();
    private int listActionCount;
    private DialogSpec.AfterAction afterAction = DialogSpec.AfterAction.CLOSE;
    private DialogSpec.ActionButton exitAction;

    public DominionDialogPage(DialogUiText text, String menuId,
                              Map<String, ?> titleValues, DialogPageLayout layout) {
        this(text, menuId, titleValues, layout, null);
    }

    public DominionDialogPage(DialogUiText text, String menuId,
                              Map<String, ?> titleValues, DialogListStyle listStyle) {
        this(text, menuId, titleValues, listStyle.pageLayout(), listStyle);
    }

    /** Convenience constructor for callers that do not need a custom layout. */
    public DominionDialogPage(DialogUiText text, String menuId, Map<String, ?> titleValues) {
        this(text, menuId, titleValues, DialogPageLayout.of(3));
    }

    private DominionDialogPage(DialogUiText text, String menuId,
                               Map<String, ?> titleValues, DialogPageLayout layout,
                               DialogListStyle listStyle) {
        this.text = text;
        this.menuId = menuId;
        this.layout = layout;
        this.listStyle = listStyle;
        this.title = component(render(text.text("menus." + menuId + ".title"), titleValues));
    }

    public DominionDialogPage message(String value) {
        bodies.add(new DialogSpec.PlainMessageBody(component(value), layout.contentWidth()));
        return this;
    }

    public DominionDialogPage messageKey(String key, Map<String, ?> values) {
        return message(render(text.text(key), values));
    }

    public DominionDialogPage summary(String element, Map<String, ?> values) {
        String path = itemPath(element);
        Component label = component(render(text.text(path + ".name"), values));
        List<Component> lore = lore(path, values);
        List<Component> lines = new ArrayList<>();
        lines.add(label);
        lines.addAll(lore);
        bodies.add(new DialogSpec.PlainMessageBody(join(lines), layout.contentWidth()));
        return this;
    }

    public DominionDialogPage textInput(String key, String labelKey, String initial, int maxLength) {
        inputs.add(new DialogSpec.TextInput(
                key, layout.contentWidth(), component(text.text(labelKey)), true,
                initial == null ? "" : initial, maxLength, null));
        return this;
    }

    public DominionDialogPage multilineInput(String key, String labelKey,
                                              String initial, int maxLength) {
        inputs.add(new DialogSpec.TextInput(
                key, layout.contentWidth(), component(text.text(labelKey)), true,
                initial == null ? "" : initial, maxLength, new DialogSpec.Multiline(3, 54)));
        return this;
    }

    public DominionDialogPage booleanInput(String key, Component label, boolean initial) {
        inputs.add(new DialogSpec.BooleanInput(key, label, initial, "true", "false"));
        return this;
    }

    public DominionDialogPage optionInput(String key, String labelKey,
                                          List<DialogSpec.Option> options) {
        return optionInput(key, component(text.text(labelKey)), options);
    }

    public DominionDialogPage optionInput(String key, Component label,
                                          List<DialogSpec.Option> options) {
        inputs.add(new DialogSpec.SingleOptionInput(
                key, layout.contentWidth(), options, label, true));
        return this;
    }

    public DominionDialogPage action(String element, Map<String, ?> values,
                                     int width, DialogSpec.Callback callback) {
        actions.add(button(element, values, width, callback));
        return this;
    }

    public DominionDialogPage action(Component label, Component tooltip,
                                     int width, DialogSpec.Callback callback) {
        return action(label, tooltip, width, icon("default"), callback);
    }

    public DominionDialogPage action(Component label, Component tooltip, int width,
                                     String icon, DialogSpec.Callback callback) {
        return action(label, tooltip, width, icon, null, callback);
    }

    public DominionDialogPage action(Component label, Component tooltip, int width,
                                     String fallbackIcon, DialogSpec.PlayerHeadIcon playerHead,
                                     DialogSpec.Callback callback) {
        actions.add(new DialogSpec.ActionButton(
                label, tooltip, width, fallbackIcon, playerHead,
                callback == null ? null : new DialogSpec.CallbackAction(callback)));
        return this;
    }

    public DominionDialogPage listAction(Component name, Component tooltip,
                                          DialogSpec.Callback callback) {
        DialogListStyle style = listStyle == null ? DialogListStyle.DEFAULT : listStyle;
        return listAction(name, tooltip, style.itemWidth(), icon("default"), callback);
    }

    public DominionDialogPage listAction(Component name, Component tooltip, int width,
                                          String icon, DialogSpec.Callback callback) {
        return listAction(name, tooltip, width, icon, null, callback);
    }

    public DominionDialogPage listAction(Component name, Component tooltip, int width,
                                          String fallbackIcon, DialogSpec.PlayerHeadIcon playerHead,
                                          DialogSpec.Callback callback) {
        Component label = name.colorIfAbsent(NamedTextColor.WHITE)
                .append(Component.text("  ›", NamedTextColor.DARK_GRAY));
        action(label, tooltip, width, fallbackIcon, playerHead, callback);
        listActionCount++;
        return this;
    }

    public DominionDialogPage completeListRow() {
        int width = listStyle == null ? layout.buttonWidth() : listStyle.itemWidth();
        if (listActionCount % 2 != 0) {
            actions.add(new DialogSpec.ActionButton(
                    Component.text(" "), null, width, null));
            listActionCount++;
        }
        return this;
    }

    public DominionDialogPage keepOpenAfterAction() {
        afterAction = DialogSpec.AfterAction.NONE;
        return this;
    }

    public DominionDialogPage backExit(DialogSpec.Callback back) {
        exitAction = button("back", Map.of(), layout.wideButtonWidth(), back);
        return this;
    }

    public DialogSpec.ActionButton button(String element, Map<String, ?> values,
                                          int width, DialogSpec.Callback callback) {
        String path = itemPath(element);
        List<Component> lore = lore(path, values);
        return new DialogSpec.ActionButton(
                component(render(text.text(path + ".name"), values)),
                lore.isEmpty() ? null : join(lore), width, icon(element),
                callback == null ? null : new DialogSpec.CallbackAction(callback));
    }

    public DialogSpec build() {
        return build(layout.columns(), exitAction);
    }

    public DialogSpec build(int columns, DialogSpec.ActionButton exit) {
        DialogSpec.ActionButton resolvedExit = exit == null ? exitAction : exit;
        DialogSpec.Type type = actions.isEmpty()
                ? new DialogSpec.Notice(resolvedExit)
                : new DialogSpec.MultiAction(actions, resolvedExit, columns);
        DialogSpec.Builder builder = DialogSpec.builder(title, type).afterAction(afterAction);
        bodies.forEach(builder::body);
        inputs.forEach(builder::input);
        return builder.build();
    }

    public DialogSpec buildList() {
        return build(listStyle == null ? layout.columns() : listStyle.pageLayout().columns(), null);
    }

    public DialogSpec buildConfirmation(DialogSpec.ActionButton yes,
                                        DialogSpec.ActionButton no) {
        DialogSpec.Builder builder = DialogSpec.builder(
                        title, new DialogSpec.Confirmation(yes, no))
                .afterAction(DialogSpec.AfterAction.CLOSE);
        bodies.forEach(builder::body);
        inputs.forEach(builder::input);
        return builder.build();
    }

    public DialogPageLayout layout() {
        return layout;
    }

    public DialogListStyle listStyle() {
        return listStyle;
    }

    /** Resolves an icon key using this page's menu override and global layout. */
    public String icon(String element) {
        return text.icon(menuId, element);
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

    public static Component component(String value) {
        return LegacyToMiniMessage.parse(value == null ? "" : value);
    }

    public static String render(String template, Map<String, ?> values) {
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
