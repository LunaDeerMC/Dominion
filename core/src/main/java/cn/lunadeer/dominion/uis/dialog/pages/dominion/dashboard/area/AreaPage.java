package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.area;

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

/** Legacy area-management page kept as a direct dashboard child. */
public final class AreaPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 3, 108, 336);

    public AreaPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "area", values, LAYOUT)
                .summary("info", values)
                .action("teleport", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> teleport(viewer, dominion))
                .action("set-teleport", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> ui.submit(viewer,
                                cn.lunadeer.dominion.providers.DominionProvider.getInstance()
                                        .setDominionTpLocation(viewer, dominion, viewer.getLocation()),
                                ignored -> {}))
                .action("resize", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.RESIZE, dominion)))
                .action("create-child", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> createDominionInput(viewer, dominion))
                .action("children", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.CHILD_LIST, dominion)));
        commonFooter(page);
        return page.build();
    }
}
