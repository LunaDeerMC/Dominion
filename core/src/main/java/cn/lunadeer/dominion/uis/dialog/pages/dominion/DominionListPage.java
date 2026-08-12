package cn.lunadeer.dominion.uis.dialog.pages.dominion;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogListStyle;
import cn.lunadeer.dominion.uis.dialog.components.DialogListTemplate;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The shared visual list used for owned, child, admin, and copy-source views. */
public final class DominionListPage extends AbstractDialogPage {
    private static final DialogListStyle STYLE = DialogListStyle.DEFAULT
            .withCompactItemWidth(104)
            .withCreateItemWidth(94);
    private static final DialogListStyle CREATE_STYLE = STYLE.withCreateButton();

    public DominionListPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        List<DominionDTO> dominions;
        String title;
        if (id(route) == DialogMenuId.ALL_DOMINIONS) {
            dominions = api.getAllDominions();
            title = config.text("titles.all-dominions");
        } else if (id(route) == DialogMenuId.CHILD_LIST) {
            DominionDTO parent = requireDominion(player, route.integer("dom"));
            dominions = api.getChildrenDominionOf(parent);
            title = configured("titles.children", Map.of("dominion", parent.getName()));
        } else if (id(route) == DialogMenuId.COPY_SOURCE) {
            int target = route.integer("dom");
            dominions = managedDominions(player).stream()
                    .filter(dominion -> dominion.getId() != target)
                    .toList();
            title = config.text("titles.copy-source");
        } else if (id(route) == DialogMenuId.PLAYER_DOMINIONS) {
            UUID targetUuid = UUID.fromString(route.string("player"));
            dominions = Stream.concat(api.getPlayerOwnDominionDTOs(targetUuid).stream(),
                            api.getPlayerAdminDominionDTOs(targetUuid).stream())
                    .collect(Collectors.toMap(DominionDTO::getId, Function.identity(), (a, b) -> a))
                    .values().stream().toList();
            title = configured("titles.player-dominions", Map.of(
                    "player", route.string("playerName")));
        } else {
            dominions = managedDominions(player);
            title = config.text("titles.my-dominions");
        }
        boolean hasCreateButton = id(route) == DialogMenuId.DOMINION_LIST
                || id(route) == DialogMenuId.CHILD_LIST;
        DialogListStyle style = hasCreateButton ? CREATE_STYLE : STYLE;
        Map<String, Object> titleValues = Map.of("title", title);
        if (DialogListTemplate.isSearchPage(route)) {
            return listSearchPage("dominion-list", titleValues, route, style);
        }
        dominions = dominions.stream()
                .filter(dominion -> matches(route.filter(), dominion.getName()))
                .sorted(Comparator.comparing(DominionDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        DominionDialogPage page = new DominionDialogPage(config, "dominion-list", titleValues, style)
                .keepOpenAfterAction();
        DialogPagination pagination = pagination(route, dominions.size(), style.pageSize());
        listNavigation(page, route, pagination, dominions.size(), style);
        if (hasCreateButton) {
            DialogListTemplate.item(page,
                    DominionDialogPage.component(config.text("menus.dominion-list.items.primary.name")),
                    DominionDialogPage.component(config.text("menus.dominion-list.items.primary.lore.0")),
                    style.createItemWidth(), page.icon("primary"),
                    (viewer, response) -> createDominionInput(viewer,
                            id(route) == DialogMenuId.CHILD_LIST
                                    ? requireDominion(viewer, route.integer("dom")) : null));
        }
        for (DominionDTO dominion : dominions.subList(pagination.from(), pagination.to())) {
            boolean remote = isRemoteDominion(dominion);
            Map<String, Object> values = dominionValues(dominion, remote);
            var tooltip = DominionDialogPage.component(configured(
                    remote ? "descriptions.remote-dominion-entry"
                            : "descriptions.local-dominion-entry", values));
            if (id(route) == DialogMenuId.COPY_SOURCE) {
                DialogListTemplate.item(page, DominionDialogPage.component(dominion.getName()), tooltip,
                        style.compactItemWidth(), page.icon("copy"),
                        (viewer, response) -> nav.push(viewer, DialogRoute.of(DialogMenuId.COPY_TYPE)
                                .with("target", route.integer("dom")).with("source", dominion.getId())));
                continue;
            }
            if (remote) {
                DialogListTemplate.item(page, DominionDialogPage.component(dominion.getName()), tooltip,
                        style.compactItemWidth(), page.icon("remote-content"),
                        (viewer, response) -> teleport(viewer, dominion));
            } else {
                DialogListTemplate.item(page, DominionDialogPage.component(dominion.getName()), tooltip,
                        style.compactItemWidth(), page.icon("content"),
                        (viewer, response) -> nav.push(viewer,
                                DialogRoute.of(DialogMenuId.DASHBOARD).with("dom", dominion.getId())));
            }
        }
        commonFooter(page);
        return page.buildList();
    }

    private static boolean isRemoteDominion(DominionDTO dominion) {
        return Configuration.multiServer.enable
                && dominion.getServerId() != Configuration.multiServer.serverId;
    }
}
