package cn.lunadeer.dominion.uis.chest;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.providers.PlayerProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.chestui.ChestUiManager;
import cn.lunadeer.dominion.utils.chestui.MenuNavigator;
import cn.lunadeer.dominion.utils.chestui.MenuRoute;
import cn.lunadeer.dominion.utils.chestui.MenuSession;
import cn.lunadeer.dominion.utils.chestui.MenuView;
import cn.lunadeer.dominion.utils.chestui.MenuViewBuilder;
import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

import static cn.lunadeer.dominion.Dominion.adminPermission;

/** Renders the root, title selection, and confirmation menus. */
final class BuiltinRootMenu extends AbstractBuiltinMenu {
    BuiltinRootMenu(ChestUiManager ui, ChestUiConfig config, MenuNavigator nav) {
        super(ui, config, nav);
    }

    MenuView main(Player player, MenuSession session) {
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "main", Map.of());
        view.item("info", Map.of(), null, null);
        view.item("dominions", Map.of(), null, click -> nav.push(player, MenuRoute.of(MenuId.DOMINION_LIST)));
        view.item("create", Map.of(), null, click -> createDominionInput(player, null));
        view.item("templates", Map.of(), null, click -> nav.push(player, MenuRoute.of(MenuId.TEMPLATE_LIST)));
        view.item("titles", Map.of(), null, click -> nav.push(player, MenuRoute.of(MenuId.TITLE_LIST)));
        if (player.hasPermission(adminPermission)) {
            view.item("all", Map.of(), null, click -> nav.push(player, MenuRoute.of(MenuId.ALL_DOMINIONS)));
        }
        view.item("help", Map.of(), null, click -> {
            Notification.info(player, Configuration.externalLinks.documentation);
            ui.close(player);
        });
        view.item("close", Map.of(), null, click -> ui.close(player));
        return view.build();
    }

    MenuView titleList(Player player, MenuSession session) {
        MenuRoute route = session.current();
        List<GroupDTO> titles = PlayerProvider.getInstance().getAvailableGroupTitles(player.getUniqueId());
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "title-list", Map.of());
        renderPage(view, route, titles, (slot, group) -> {
            DominionDTO dominion = api.getDominion(group.getDomID());
            view.itemAt(slot, "content", "content", Map.of(
                    "group", group.getNamePlain(),
                    "dominion", dominion == null ? config.text("labels.unknown") : dominion.getName()),
                    null, click -> ui.submit(player,
                            PlayerProvider.getInstance().setGroupTitle(player, group), ignored -> {}));
        });
        view.item("disable", Map.of(), null, click -> ui.submit(player,
                PlayerProvider.getInstance().setGroupTitle(player, null), ignored -> {}));
        listNavigation(player, view, route, titles.size());
        return view.build();
    }

    MenuView confirm(Player player, MenuSession session) {
        String summary = session.current().string("summary");
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "confirm", Map.of("summary", summary));
        view.item("info", Map.of("summary", summary), null, null);
        view.item("confirm", Map.of("summary", summary), null, click -> {
            var action = session.takeConfirmation();
            session.back();
            if (action != null) action.accept(player);
        });
        view.item("cancel", Map.of(), null, click -> {
            session.takeConfirmation();
            nav.back(player);
        });
        return view.build();
    }
}
