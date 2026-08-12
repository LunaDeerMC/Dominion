package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.providers.CopyProvider;
import cn.lunadeer.dominion.providers.CopyType;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogResponse;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/** Chooses which data to copy between two dominions. */
public final class CopyTypePage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 3, 108, 336);

    public CopyTypePage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DominionDTO source = requireDominion(player, route.integer("source"));
        DominionDTO target = requireDominion(player, route.integer("target"));
        Map<String, Object> values = Map.of("source", source.getName(), "target", target.getName());
        List<DialogSpec.Option> types = List.of(
                option("environment", "labels.copy-types.environment", true),
                option("guest", "labels.copy-types.guest", false),
                option("member", "labels.copy-types.member", false),
                option("group", "labels.copy-types.group", false));
        DominionDialogPage page = new DominionDialogPage(config, "copy-type", values, LAYOUT)
                .summary("info", values)
                .optionInput("copy_type", "input.copy-type", types)
                .action("apply", Map.of(), LAYOUT.buttonWidth(),
                        (viewer, response) -> apply(viewer, response, source, target));
        formFooter(page);
        return page.build();
    }

    private DialogSpec.Option option(String id, String labelKey, boolean initial) {
        return new DialogSpec.Option(id, DominionDialogPage.component(config.text(labelKey)), initial);
    }

    private void apply(Player player, DialogResponse response,
                       DominionDTO source, DominionDTO target) {
        String selected = response.getOption("copy_type");
        CopyType type = switch (selected == null ? "" : selected) {
            case "environment" -> CopyType.ENVIRONMENT;
            case "guest" -> CopyType.GUEST;
            case "member" -> CopyType.MEMBER;
            case "group" -> CopyType.GROUP;
            default -> null;
        };
        if (type == null) return;
        ui.confirm(player, configured("confirm.copy", Map.of(
                        "type", config.text("labels.copy-types." + selected),
                        "source", source.getName(), "target", target.getName())), confirmed ->
                ui.submit(player, CopyProvider.getInstance().copy(player, source, target, type),
                        ignored -> {}));
    }
}
