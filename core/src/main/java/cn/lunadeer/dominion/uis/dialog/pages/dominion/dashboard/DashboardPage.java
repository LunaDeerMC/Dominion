package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Map;

/** The dashboard page for one dominion. */
public final class DashboardPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 3, 108, 336);

    public DashboardPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "dashboard", values, LAYOUT)
                .summary("info", values)
                .action("teleport", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> teleport(viewer, dominion))
                .action("permissions", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.PERMISSIONS, dominion)))
                .action("children", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.CHILD_LIST, dominion)))
                .action("other", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.OTHER, dominion)));
        commonFooter(page);
        return page.build();
    }
}
