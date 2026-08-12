package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.providers.DominionProvider;
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

/** Legacy ownership page for transfer and deletion actions. */
public final class OwnershipPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 2, 164, 336);

    public OwnershipPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "ownership", values, LAYOUT)
                .summary("info", values)
                .action("transfer", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.TRANSFER_PICKER, dominion)))
                .action("delete", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> ui.confirm(viewer,
                                configured("confirm.delete-dominion",
                                        Map.of("dominion", dominion.getName())),
                                confirmed -> ui.submit(viewer,
                                        DominionProvider.getInstance().deleteDominion(
                                                viewer, dominion, false, true),
                                        (ignored, sess) -> sess.home())));
        commonFooter(page);
        return page.build();
    }
}
