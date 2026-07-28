package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.providers.PlayerProvider;
import cn.lunadeer.dominion.uis.DominionUi;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.UiMode;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static cn.lunadeer.dominion.Dominion.adminPermission;

/** Native root, title selection, and confirmation dialogs. */
final class DialogRootMenu extends AbstractDialogMenu {
    DialogRootMenu(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    DialogSpec main(Player player, DialogMenuSession session) {
        DominionDialogPage page = new DominionDialogPage(config, "main", Map.of())
                .summary("info", Map.of(), Material.GRASS_BLOCK)
                .action("dominions", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, DialogRoute.of(DialogMenuId.DOMINION_LIST)))
                .action("create", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> createDominionInput(viewer, null))
                .action("templates", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, DialogRoute.of(DialogMenuId.TEMPLATE_LIST)))
                .action("titles", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, DialogRoute.of(DialogMenuId.TITLE_LIST)));
        if (player.hasPermission(adminPermission)) {
            page.action("all", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                            (viewer, response) -> nav.push(viewer, DialogRoute.of(DialogMenuId.ALL_DOMINIONS)))
                    .action("player-dominions", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                            (viewer, response) ->
                                    nav.push(viewer, DialogRoute.of(DialogMenuId.ADMIN_PLAYER_DOMINIONS)));
        }
        page.action("help", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH, (viewer, response) -> {
                    Notification.info(viewer, Configuration.externalLinks.documentation);
                    ui.close(viewer);
                })
                .action("ui", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> DominionUi.setPreferenceAndOpen(viewer, UiMode.CHEST));
        return page.build(3, closeButton(player, page));
    }

    DialogSpec titleList(Player player, DialogMenuSession session) {
        List<GroupDTO> titles = PlayerProvider.getInstance().getAvailableGroupTitles(player.getUniqueId()).stream()
                .sorted(Comparator.comparing(GroupDTO::getNamePlain, String.CASE_INSENSITIVE_ORDER))
                .toList();
        PlayerDTO playerDto = api.getPlayer(player.getUniqueId());
        int selected = playerDto == null || playerDto.getUsingGroupTitleID() == null
                ? -1 : playerDto.getUsingGroupTitleID();
        boolean selectedAvailable = titles.stream().anyMatch(group -> group.getId() == selected);
        List<DialogSpec.Option> options = new ArrayList<>();
        options.add(new DialogSpec.Option("-1",
                DominionDialogPage.component(config.text("labels.none")),
                selected == -1 || !selectedAvailable));
        for (GroupDTO group : titles) {
            DominionDTO dominion = api.getDominion(group.getDomID());
            String label = configured("labels.title-option", Map.of(
                    "group", group.getNamePlain(),
                    "dominion", dominion == null ? config.text("labels.unknown") : dominion.getName()));
            options.add(new DialogSpec.Option(String.valueOf(group.getId()),
                    DominionDialogPage.component(label), selected == group.getId()));
        }

        DominionDialogPage page = new DominionDialogPage(config, "title-list", Map.of())
                .messageKey("descriptions.title-list", Map.of())
                .optionInput("title", "input.select-title", options)
                .action("save", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH, (viewer, response) -> {
                    String value = response.getOption("title");
                    if (value == null) return;
                    GroupDTO chosen = "-1".equals(value)
                            ? null
                            : titles.stream()
                                    .filter(group -> String.valueOf(group.getId()).equals(value))
                                    .findFirst()
                                    .orElse(null);
                    if (!"-1".equals(value) && chosen == null) return;
                    ui.submit(viewer, PlayerProvider.getInstance().setGroupTitle(viewer, chosen), ignored -> {});
                });
        formFooter(page);
        return page.build(3, null);
    }

    DialogSpec confirm(Player player, DialogMenuSession session) {
        String summary = session.current().string("summary");
        Map<String, Object> values = Map.of("summary", summary);
        DominionDialogPage page = new DominionDialogPage(config, "confirm", values)
                .summary("info", values, Material.BARRIER);
        DialogSpec.ActionButton yes = page.button(
                "confirm", values, DominionDialogPage.TWO_COLUMN_WIDTH, (viewer, response) -> {
                    var action = session.takeConfirmation();
                    session.back();
                    if (action != null) action.accept(viewer);
                });
        DialogSpec.ActionButton no = page.button(
                "cancel", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH, (viewer, response) -> {
                    session.takeConfirmation();
                    nav.back(viewer);
                });
        return page.buildConfirmation(yes, no);
    }
}
