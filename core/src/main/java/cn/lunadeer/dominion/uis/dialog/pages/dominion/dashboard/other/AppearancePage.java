package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.events.dominion.modify.DominionSetMessageEvent;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogResponse;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Edits a dominion's name, messages, and map color. */
public final class AppearancePage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 3, 108, 336);

    public AppearancePage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "appearance", values, LAYOUT)
                .summary("info", values)
                .textInput("name", "input.rename-dominion", dominion.getName(), 128)
                .multilineInput("enter_message", "input.enter-message", dominion.getJoinMessage(), 512)
                .multilineInput("leave_message", "input.leave-message", dominion.getLeaveMessage(), 512)
                .textInput("map_color", "input.map-color", dominion.getColor(), 9)
                .action("save", Map.of(), LAYOUT.buttonWidth(),
                        (viewer, response) -> save(viewer, dominion, response));
        formFooter(page);
        return page.build();
    }

    private void save(Player player, DominionDTO dominion, DialogResponse response) {
        String name = response.getText("name");
        String enter = response.getText("enter_message");
        String leave = response.getText("leave_message");
        String colorText = response.getText("map_color");
        if (name == null || enter == null || leave == null || colorText == null || name.isBlank()) return;

        Color color;
        try {
            String normalized = colorText.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("0x")) normalized = normalized.substring(2);
            if (normalized.startsWith("#")) normalized = normalized.substring(1);
            if (!normalized.matches("[0-9a-f]{6}")) throw new NumberFormatException();
            color = Color.fromRGB(Integer.parseInt(normalized, 16));
        } catch (Exception exception) {
            Notification.error(player, config.text("errors.invalid-color"));
            nav.refresh(player);
            return;
        }

        List<CompletableFuture<?>> updates = new ArrayList<>();
        if (!name.equals(dominion.getName())) {
            updates.add(DominionProvider.getInstance().renameDominion(player, dominion, name.trim()));
        }
        if (!enter.equals(dominion.getJoinMessage())) {
            updates.add(DominionProvider.getInstance().setDominionMessage(
                    player, dominion, DominionSetMessageEvent.TYPE.ENTER, enter));
        }
        if (!leave.equals(dominion.getLeaveMessage())) {
            updates.add(DominionProvider.getInstance().setDominionMessage(
                    player, dominion, DominionSetMessageEvent.TYPE.LEAVE, leave));
        }
        if (!("#" + String.format("%06X", color.asRGB())).equalsIgnoreCase(dominion.getColor())) {
            updates.add(DominionProvider.getInstance().setDominionMapColor(player, dominion, color));
        }
        if (updates.isEmpty()) {
            nav.refresh(player);
            return;
        }
        ui.submit(player, CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new)), ignored -> {});
    }
}
