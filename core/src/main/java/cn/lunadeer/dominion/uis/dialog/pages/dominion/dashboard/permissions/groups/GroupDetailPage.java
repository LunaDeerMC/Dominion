package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.groups;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogResponse;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Map;

/** Edits one permission group and manages its members and flags. */
public final class GroupDetailPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 2, 164, 336);

    public GroupDetailPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) throws Exception {
        var route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        GroupDTO group = requireGroup(dominion, route.integer("group"));
        Map<String, Object> values = Map.of(
                "dominion", dominion.getName(),
                "group", group.getNamePlain(),
                "members", group.getMembers().size());
        DominionDialogPage page = new DominionDialogPage(config, "group-detail", values, LAYOUT)
                .summary("info", values)
                .textInput("group_name", "input.rename-group", group.getNamePlain(), 128)
                .action("save", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> save(viewer, response, dominion, group))
                .action("flags", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.GROUP_FLAGS, dominion).with("group", group.getId())))
                .action("add-member", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.GROUP_MEMBER_PICKER, dominion)
                                        .with("group", group.getId()).with("mode", "add")))
                .action("members", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.GROUP_MEMBER_PICKER, dominion)
                                        .with("group", group.getId()).with("mode", "remove")))
                .action("delete", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> ui.confirm(viewer,
                                configured("confirm.delete-group",
                                        Map.of("group", group.getNamePlain())),
                                confirmed -> ui.submit(viewer,
                                        GroupProvider.getInstance().deleteGroup(viewer, dominion, group),
                                        ignored -> nav.back(viewer))));
        commonFooter(page);
        return page.build();
    }

    private void save(Player player, DialogResponse response,
                      DominionDTO dominion, GroupDTO group) {
        String name = response.getText("group_name");
        if (name == null || name.isBlank() || name.equals(group.getNamePlain())) {
            nav.refresh(player);
            return;
        }
        ui.submit(player, GroupProvider.getInstance().renameGroup(
                player, dominion, group, name.trim()), ignored -> {});
    }
}
