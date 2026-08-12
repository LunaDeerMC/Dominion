package cn.lunadeer.dominion.uis.dialog.pages.root;

import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Map;

/** Confirmation page used by destructive and ownership-changing actions. */
public final class ConfirmPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 2, 164, 336);

    public ConfirmPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        String summary = session.current().string("summary");
        Map<String, Object> values = Map.of("summary", summary);
        DominionDialogPage page = new DominionDialogPage(config, "confirm", values, LAYOUT)
                .summary("info", values);
        DialogSpec.ActionButton yes = page.button("confirm", values, LAYOUT.buttonWidth(),
                (viewer, response) -> {
                    var action = session.takeConfirmation();
                    session.back();
                    if (action != null) action.accept(viewer);
                });
        DialogSpec.ActionButton no = page.button("cancel", Map.of(), LAYOUT.buttonWidth(),
                (viewer, response) -> {
                    session.takeConfirmation();
                    nav.back(viewer);
                });
        return page.buildConfirmation(yes, no);
    }
}
