package cn.lunadeer.dominion.uis.dialog.pages.root;

import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.uis.DominionUi;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.UiMode;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Map;

import static cn.lunadeer.dominion.Dominion.adminPermission;

/** The root page of the native dialog UI. */
public final class MainPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 3, 108, 336);

    public MainPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DominionDialogPage page = new DominionDialogPage(config, "main", Map.of(), LAYOUT)
                .summary("info", Map.of())
                .action("dominions", Map.of(), LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                DialogRoute.of(DialogMenuId.DOMINION_LIST)))
                .action("templates", Map.of(), LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                DialogRoute.of(DialogMenuId.TEMPLATE_LIST)))
                .action("titles", Map.of(), LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                DialogRoute.of(DialogMenuId.TITLE_LIST)));
        if (player.hasPermission(adminPermission)) {
            page.action("all", Map.of(), LAYOUT.buttonWidth(),
                            (viewer, response) -> nav.push(viewer,
                                    DialogRoute.of(DialogMenuId.ALL_DOMINIONS)))
                    .action("player-dominions", Map.of(), LAYOUT.buttonWidth(),
                            (viewer, response) -> nav.push(viewer,
                                    DialogRoute.of(DialogMenuId.ADMIN_PLAYER_DOMINIONS)));
        }
        page.action("help", Map.of(), LAYOUT.buttonWidth(), (viewer, response) -> {
                    Notification.info(viewer, Configuration.externalLinks.documentation);
                    ui.close(viewer);
                })
                .action("ui", Map.of(), LAYOUT.buttonWidth(),
                        (viewer, response) -> DominionUi.setPreferenceAndOpen(viewer, UiMode.CHEST));
        return page.build(3, closeButton(page));
    }
}
