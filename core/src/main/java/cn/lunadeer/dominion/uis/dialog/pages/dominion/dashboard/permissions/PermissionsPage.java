package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Map;

/** Entry page for environment, guest, member, and group permissions. */
public final class PermissionsPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 2, 164, 336);

    public PermissionsPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "permissions", values, LAYOUT)
                .summary("info", values)
                .action("environment", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.ENV_FLAGS, dominion)))
                .action("guest", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.GUEST_FLAGS, dominion)))
                .action("members", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.MEMBER_LIST, dominion)))
                .action("groups", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.GROUP_LIST, dominion)));
        commonFooter(page);
        return page.build();
    }
}
