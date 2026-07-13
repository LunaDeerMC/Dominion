package cn.lunadeer.dominion.uis.chest;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.events.dominion.modify.DominionReSizeEvent;
import cn.lunadeer.dominion.events.dominion.modify.DominionSetMessageEvent;
import cn.lunadeer.dominion.providers.CopyProvider;
import cn.lunadeer.dominion.providers.CopyType;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.TeleportProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.chestui.ChestUiManager;
import cn.lunadeer.dominion.utils.chestui.MenuNavigator;
import cn.lunadeer.dominion.utils.chestui.MenuRoute;
import cn.lunadeer.dominion.utils.chestui.MenuSession;
import cn.lunadeer.dominion.utils.chestui.MenuView;
import cn.lunadeer.dominion.utils.chestui.MenuViewBuilder;
import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import cn.lunadeer.dominion.utils.chestui.config.ItemAppearance;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders territory discovery, dashboard, area, appearance, ownership, and copy menus. */
final class BuiltinDominionMenu extends AbstractBuiltinMenu {
    BuiltinDominionMenu(ChestUiManager ui, ChestUiConfig config, MenuNavigator nav) {
        super(ui, config, nav);
    }

    MenuView dominionList(Player player, MenuSession session) {
        MenuRoute route = session.current();
        List<DominionDTO> dominions;
        String title;
        if (id(route) == MenuId.ALL_DOMINIONS) {
            dominions = api.getAllDominions();
            title = config.text("titles.all-dominions");
        } else if (id(route) == MenuId.CHILD_LIST) {
            DominionDTO parent = requireDominion(player, route.integer("dom"));
            dominions = api.getChildrenDominionOf(parent);
            title = configured("titles.children", Map.of("dominion", parent.getName()));
        } else if (id(route) == MenuId.COPY_SOURCE) {
            int target = route.integer("dom");
            dominions = managedDominions(player).stream().filter(dominion -> dominion.getId() != target).toList();
            title = config.text("titles.copy-source");
        } else {
            dominions = managedDominions(player);
            title = config.text("titles.my-dominions");
        }
        dominions = dominions.stream()
                .filter(dominion -> matches(route.filter(), dominion.getName()))
                .sorted(Comparator.comparing(DominionDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "dominion-list", Map.of("title", title));
        renderPage(view, route, dominions, (slot, dominion) -> {
            boolean remote = isRemoteDominion(dominion);
            String textElement = remote ? "remote-content" : "local-content";
            ItemAppearance appearance = remote ? view.appearance("remote-content") : null;
            view.itemAt(slot, "content", textElement, dominionValues(dominion), null, appearance, click -> {
                if (click.isRightClick()) {
                    teleport(player, dominion);
                } else if (isLeftClick(click) && !remote) {
                    if (id(route) == MenuId.COPY_SOURCE) {
                        nav.push(player, MenuRoute.of(MenuId.COPY_TYPE)
                                .with("target", route.integer("dom")).with("source", dominion.getId()));
                    } else {
                        nav.push(player, MenuRoute.of(MenuId.DASHBOARD).with("dom", dominion.getId()));
                    }
                }
            });
        });
        if (id(route) == MenuId.DOMINION_LIST) {
            view.item("primary", Map.of(), null, click -> createDominionInput(player, null));
        }
        view.item("search", Map.of(), null, click -> search(player));
        listNavigation(player, view, route, dominions.size());
        return view.build();
    }

    MenuView dashboard(Player player, MenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion);
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "dashboard", values);
        view.item("info", values, null, null);
        view.item("area", values, null, click -> nav.push(player, route(MenuId.AREA, dominion)));
        view.item("permissions", values, null, click -> nav.push(player, route(MenuId.PERMISSIONS, dominion)));
        view.item("people", values, null, click -> nav.push(player, route(MenuId.PEOPLE, dominion)));
        view.item("appearance", values, null, click -> nav.push(player, route(MenuId.APPEARANCE, dominion)));
        view.item("copy", values, null, click -> nav.push(player, route(MenuId.COPY_SOURCE, dominion)));
        view.item("ownership", values, null, click -> nav.push(player, route(MenuId.OWNERSHIP, dominion)));
        commonFooter(player, view);
        return view.build();
    }

    MenuView area(Player player, MenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion);
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "area", values);
        view.item("info", values, null, null);
        view.item("set-teleport", values, null, click -> ui.submit(player,
                DominionProvider.getInstance().setDominionTpLocation(player, dominion, player.getLocation()),
                ignored -> {}));
        view.item("resize", values, null, click -> nav.push(player, route(MenuId.RESIZE, dominion)));
        view.item("create-child", values, null, click -> createDominionInput(player, dominion));
        view.item("children", values, null, click -> nav.push(player, route(MenuId.CHILD_LIST, dominion)));
        commonFooter(player, view);
        return view.build();
    }

    MenuView appearance(Player player, MenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion);
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "appearance", values);
        view.item("info", values, null, null);
        view.item("rename", values, null, click -> ui.requestInput(player, config.text("input.rename-dominion"),
                value -> ui.submit(player, DominionProvider.getInstance().renameDominion(player, dominion, value),
                        result -> {})));
        view.item("enter-message", values, null,
                click -> messageInput(player, dominion, DominionSetMessageEvent.TYPE.ENTER));
        view.item("leave-message", values, null,
                click -> messageInput(player, dominion, DominionSetMessageEvent.TYPE.LEAVE));
        view.item("map-color", values, null, click -> ui.requestInput(player, config.text("input.map-color"), value -> {
            try {
                String normalized = value.toLowerCase(Locale.ROOT).startsWith("0x") ? value.substring(2) : value;
                Color color = Color.fromRGB(Integer.parseInt(normalized, 16));
                ui.submit(player, DominionProvider.getInstance().setDominionMapColor(player, dominion, color),
                        ignored -> {});
            } catch (Exception exception) {
                Notification.error(player, config.text("errors.invalid-color"));
                nav.refresh(player);
            }
        }));
        commonFooter(player, view);
        return view.build();
    }

    MenuView ownership(Player player, MenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion);
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "ownership", values);
        view.item("info", values, null, null);
        view.item("transfer", values, null, click -> nav.push(player, route(MenuId.TRANSFER_PICKER, dominion)));
        view.item("delete", values, null, click -> ui.confirm(player,
                configured("confirm.delete-dominion", Map.of("dominion", dominion.getName())), confirmed ->
                        ui.submit(player, DominionProvider.getInstance().deleteDominion(player, dominion, false, true),
                                ignored -> nav.home(player))));
        commonFooter(player, view);
        return view.build();
    }

    MenuView resize(Player player, MenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion);
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "resize", values);
        view.item("info", values, null, null);
        Map<String, DominionReSizeEvent.DIRECTION> directions = Map.of(
                "north", DominionReSizeEvent.DIRECTION.NORTH,
                "south", DominionReSizeEvent.DIRECTION.SOUTH,
                "east", DominionReSizeEvent.DIRECTION.EAST,
                "west", DominionReSizeEvent.DIRECTION.WEST,
                "up", DominionReSizeEvent.DIRECTION.UP,
                "down", DominionReSizeEvent.DIRECTION.DOWN);
        directions.forEach((element, direction) -> view.item(element, values, null, click ->
                ui.requestInput(player, config.text("input.resize"), input -> resize(player, dominion, direction, input))));
        commonFooter(player, view);
        return view.build();
    }

    MenuView copyType(Player player, MenuSession session) {
        DominionDTO source = requireDominion(player, session.current().integer("source"));
        DominionDTO target = requireDominion(player, session.current().integer("target"));
        Map<String, Object> values = Map.of("source", source.getName(), "target", target.getName());
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "copy-type", values);
        view.item("info", values, null, null);
        Map<String, CopyType> types = Map.of(
                "environment", CopyType.ENVIRONMENT,
                "guest", CopyType.GUEST,
                "member", CopyType.MEMBER,
                "group", CopyType.GROUP);
        types.forEach((element, type) -> view.item(element, values, null, click -> ui.confirm(player,
                configured("confirm.copy", Map.of(
                        "type", config.text("labels.copy-types." + element),
                        "source", source.getName(),
                        "target", target.getName())), confirmed ->
                        ui.submit(player, CopyProvider.getInstance().copy(player, source, target, type), ignored -> {}))));
        commonFooter(player, view);
        return view.build();
    }

    private DominionDTO currentDominion(Player player, MenuSession session) {
        return requireDominion(player, session.current().integer("dom"));
    }

    private void messageInput(Player player, DominionDTO dominion, DominionSetMessageEvent.TYPE type) {
        String hint = config.text(type == DominionSetMessageEvent.TYPE.ENTER
                ? "input.enter-message" : "input.leave-message");
        ui.requestInput(player, hint, message -> ui.submit(player,
                DominionProvider.getInstance().setDominionMessage(player, dominion, type, message), ignored -> {}));
    }

    private void teleport(Player player, DominionDTO dominion) {
        ui.submit(player, TeleportProvider.getInstance().teleport(player, dominion), accepted -> {
            if (accepted) ui.close(player);
        });
    }

    private static boolean isRemoteDominion(DominionDTO dominion) {
        return Configuration.multiServer.enable
                && dominion.getServerId() != Configuration.multiServer.serverId;
    }

    private static boolean isLeftClick(ClickType click) {
        return click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
    }

    private void resize(Player player, DominionDTO dominion, DominionReSizeEvent.DIRECTION direction, String input) {
        try {
            int signed = Integer.parseInt(input);
            if (signed == 0) throw new NumberFormatException();
            DominionReSizeEvent.TYPE type = signed > 0
                    ? DominionReSizeEvent.TYPE.EXPAND : DominionReSizeEvent.TYPE.CONTRACT;
            ui.submit(player, DominionProvider.getInstance().resizeDominion(
                    player, dominion, type, direction, Math.abs(signed)), ignored -> {});
        } catch (NumberFormatException exception) {
            Notification.error(player, config.text("errors.non-zero-integer"));
            nav.refresh(player);
        }
    }
}
