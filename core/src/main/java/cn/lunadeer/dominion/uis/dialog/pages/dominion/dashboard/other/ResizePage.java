package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.events.dominion.modify.DominionReSizeEvent;
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
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/** Adjusts one of a dominion's six boundaries. */
public final class ResizePage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 3, 108, 336);

    public ResizePage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        List<DialogSpec.Option> directions = List.of(
                option("north", "labels.directions.north", true),
                option("south", "labels.directions.south", false),
                option("east", "labels.directions.east", false),
                option("west", "labels.directions.west", false),
                option("up", "labels.directions.up", false),
                option("down", "labels.directions.down", false));
        List<DialogSpec.Option> modes = List.of(
                option("expand", "labels.resize-modes.expand", true),
                option("contract", "labels.resize-modes.contract", false));
        DominionDialogPage page = new DominionDialogPage(config, "resize", values, LAYOUT)
                .summary("info", values)
                .optionInput("direction", "input.resize-direction", directions)
                .optionInput("mode", "input.resize-mode", modes)
                .textInput("amount", "input.resize-amount", "1", 10)
                .action("apply", Map.of(), LAYOUT.buttonWidth(),
                        (viewer, response) -> resize(viewer, dominion, response));
        formFooter(page);
        return page.build();
    }

    private DialogSpec.Option option(String id, String labelKey, boolean initial) {
        return new DialogSpec.Option(id, DominionDialogPage.component(config.text(labelKey)), initial);
    }

    private void resize(Player player, DominionDTO dominion, DialogResponse response) {
        try {
            int amount = Integer.parseInt(value(response.getText("amount")));
            if (amount <= 0) throw new NumberFormatException();
            DominionReSizeEvent.DIRECTION direction = switch (value(response.getOption("direction"))) {
                case "north" -> DominionReSizeEvent.DIRECTION.NORTH;
                case "south" -> DominionReSizeEvent.DIRECTION.SOUTH;
                case "east" -> DominionReSizeEvent.DIRECTION.EAST;
                case "west" -> DominionReSizeEvent.DIRECTION.WEST;
                case "up" -> DominionReSizeEvent.DIRECTION.UP;
                case "down" -> DominionReSizeEvent.DIRECTION.DOWN;
                default -> throw new IllegalArgumentException();
            };
            DominionReSizeEvent.TYPE type = switch (value(response.getOption("mode"))) {
                case "expand" -> DominionReSizeEvent.TYPE.EXPAND;
                case "contract" -> DominionReSizeEvent.TYPE.CONTRACT;
                default -> throw new IllegalArgumentException();
            };
            ui.submit(player, DominionProvider.getInstance().resizeDominion(
                    player, dominion, type, direction, amount), ignored -> {});
        } catch (Exception exception) {
            Notification.error(player, config.text("errors.positive-integer"));
            nav.refresh(player);
        }
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
