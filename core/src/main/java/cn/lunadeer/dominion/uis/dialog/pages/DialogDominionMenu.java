package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.events.dominion.modify.DominionReSizeEvent;
import cn.lunadeer.dominion.events.dominion.modify.DominionSetMessageEvent;
import cn.lunadeer.dominion.providers.CopyProvider;
import cn.lunadeer.dominion.providers.CopyType;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.TeleportProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Native territory discovery, dashboard, area, appearance, ownership, and copy dialogs. */
final class DialogDominionMenu extends AbstractDialogMenu {
    DialogDominionMenu(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    DialogSpec dominionList(Player player, DialogMenuSession session) {
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
            dominions = Stream.concat(
                            api.getPlayerOwnDominionDTOs(targetUuid).stream(),
                            api.getPlayerAdminDominionDTOs(targetUuid).stream())
                    .collect(Collectors.toMap(DominionDTO::getId, Function.identity(), (a, b) -> a))
                    .values().stream().toList();
            title = configured("titles.player-dominions", Map.of("player", route.string("playerName")));
        } else {
            dominions = managedDominions(player);
            title = config.text("titles.my-dominions");
        }
        dominions = dominions.stream()
                .filter(dominion -> matches(route.filter(), dominion.getName()))
                .sorted(Comparator.comparing(DominionDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        DominionDialogPage page = new DominionDialogPage(config, "dominion-list", Map.of("title", title))
                .textInput("search", "input.search", route.filter(), 128)
                .keepOpenAfterAction();
        if (id(route) == DialogMenuId.DOMINION_LIST) {
            page.action("primary", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                            (viewer, response) -> createDominionInput(viewer, null))
                    .action("search", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                            (viewer, response) -> applySearch(viewer, route, response));
        } else {
            listSearchAction(page, route);
        }
        DialogPagination pagination = pagination(route, dominions.size(), DominionDialogPage.LIST_PAGE_SIZE);
        for (DominionDTO dominion : dominions.subList(pagination.from(), pagination.to())) {
            boolean remote = isRemoteDominion(dominion);
            Map<String, Object> values = dominionValues(dominion, remote);
            Component tooltip = DominionDialogPage.component(
                    configured(remote
                            ? "descriptions.remote-dominion-entry"
                            : "descriptions.local-dominion-entry", values));
            if (id(route) == DialogMenuId.COPY_SOURCE) {
                page.listAction(
                        DominionDialogPage.component(dominion.getName()),
                        tooltip,
                        (viewer, response) -> nav.push(viewer, DialogRoute.of(DialogMenuId.COPY_TYPE)
                                .with("target", route.integer("dom")).with("source", dominion.getId())));
                continue;
            }
            if (remote) {
                page.listAction(
                        DominionDialogPage.component(dominion.getName()),
                        tooltip,
                        (viewer, response) -> teleport(viewer, dominion));
            } else {
                page.listAction(
                        DominionDialogPage.component(dominion.getName()),
                        tooltip,
                        (viewer, response) -> nav.push(viewer,
                                DialogRoute.of(DialogMenuId.DASHBOARD)
                                        .with("dom", dominion.getId())));
            }
        }
        listNavigation(player, page, route, pagination, dominions.size());
        commonFooter(page);
        return page.buildList();
    }

    DialogSpec dashboard(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "dashboard", values)
                .summary("info", values, Material.GRASS_BLOCK)
                .action("teleport", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> teleport(viewer, dominion))
                .action("area", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.AREA, dominion)))
                .action("permissions", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.PERMISSIONS, dominion)))
                .action("people", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.PEOPLE, dominion)))
                .action("appearance", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.APPEARANCE, dominion)))
                .action("copy", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.COPY_SOURCE, dominion)))
                .action("ownership", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.OWNERSHIP, dominion)));
        commonFooter(page);
        return page.build(3, null);
    }

    DialogSpec area(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "area", values)
                .summary("info", values, Material.RECOVERY_COMPASS)
                .action("teleport", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> teleport(viewer, dominion))
                .action("set-teleport", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> ui.submit(viewer,
                                DominionProvider.getInstance().setDominionTpLocation(
                                        viewer, dominion, viewer.getLocation()), ignored -> {}))
                .action("resize", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.RESIZE, dominion)))
                .action("create-child", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> createDominionInput(viewer, dominion))
                .action("children", values, DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.CHILD_LIST, dominion)));
        commonFooter(page);
        return page.build(3, null);
    }

    DialogSpec appearance(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "appearance", values)
                .summary("info", values, Material.NAME_TAG)
                .textInput("name", "input.rename-dominion", dominion.getName(), 128)
                .multilineInput("enter_message", "input.enter-message", dominion.getJoinMessage(), 512)
                .multilineInput("leave_message", "input.leave-message", dominion.getLeaveMessage(), 512)
                .textInput("map_color", "input.map-color", dominion.getColor(), 9)
                .action("save", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> saveAppearance(viewer, dominion, response));
        formFooter(page);
        return page.build(3, null);
    }

    DialogSpec ownership(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "ownership", values)
                .summary("info", values, Material.GOLDEN_HELMET)
                .action("transfer", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.TRANSFER_PICKER, dominion)))
                .action("delete", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> ui.confirm(viewer,
                                configured("confirm.delete-dominion",
                                        Map.of("dominion", dominion.getName())),
                                confirmed -> ui.submit(viewer,
                                        DominionProvider.getInstance().deleteDominion(
                                                viewer, dominion, false, true),
                                        ignored -> nav.home(viewer))));
        commonFooter(page);
        return page.build(2, null);
    }

    DialogSpec resize(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        List<DialogSpec.Option> directions = List.of(
                option("north", "labels.directions.north", true),
                option("south", "labels.directions.south", false),
                option("east", "labels.directions.east", false),
                option("west", "labels.directions.west", false),
                option("up", "labels.directions.up", false),
                option("down", "labels.directions.down", false)
        );
        List<DialogSpec.Option> modes = List.of(
                option("expand", "labels.resize-modes.expand", true),
                option("contract", "labels.resize-modes.contract", false)
        );
        DominionDialogPage page = new DominionDialogPage(config, "resize", values)
                .summary("info", values, Material.STRUCTURE_VOID)
                .optionInput("direction", "input.resize-direction", directions)
                .optionInput("mode", "input.resize-mode", modes)
                .textInput("amount", "input.resize-amount", "1", 10)
                .action("apply", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH,
                        (viewer, response) -> resize(viewer, dominion,
                                response.getOption("direction"),
                                response.getOption("mode"),
                                response.getText("amount")));
        formFooter(page);
        return page.build(3, null);
    }

    DialogSpec copyType(Player player, DialogMenuSession session) {
        DominionDTO source = requireDominion(player, session.current().integer("source"));
        DominionDTO target = requireDominion(player, session.current().integer("target"));
        Map<String, Object> values = Map.of("source", source.getName(), "target", target.getName());
        List<DialogSpec.Option> types = List.of(
                option("environment", "labels.copy-types.environment", true),
                option("guest", "labels.copy-types.guest", false),
                option("member", "labels.copy-types.member", false),
                option("group", "labels.copy-types.group", false)
        );
        DominionDialogPage page = new DominionDialogPage(config, "copy-type", values)
                .summary("info", values, Material.WRITABLE_BOOK)
                .optionInput("copy_type", "input.copy-type", types)
                .action("apply", Map.of(), DominionDialogPage.THREE_COLUMN_WIDTH, (viewer, response) -> {
                    String selected = response.getOption("copy_type");
                    CopyType type = switch (selected == null ? "" : selected) {
                        case "environment" -> CopyType.ENVIRONMENT;
                        case "guest" -> CopyType.GUEST;
                        case "member" -> CopyType.MEMBER;
                        case "group" -> CopyType.GROUP;
                        default -> null;
                    };
                    if (type == null) return;
                    ui.confirm(viewer, configured("confirm.copy", Map.of(
                            "type", config.text("labels.copy-types." + selected),
                            "source", source.getName(),
                            "target", target.getName())), confirmed ->
                            ui.submit(viewer,
                                    CopyProvider.getInstance().copy(viewer, source, target, type),
                                    ignored -> {}));
                });
        formFooter(page);
        return page.build(3, null);
    }

    private DominionDTO currentDominion(Player player, DialogMenuSession session) {
        return requireDominion(player, session.current().integer("dom"));
    }

    private void applySearch(Player player, DialogRoute route,
                             cn.lunadeer.dominion.utils.dialogui.DialogResponse response) {
        String filter = response.getText("search");
        nav.replace(player, route.filter(filter == null ? "" : filter.trim()));
    }

    private DialogSpec.Option option(String id, String labelKey, boolean initial) {
        return new DialogSpec.Option(id, DominionDialogPage.component(config.text(labelKey)), initial);
    }

    private void saveAppearance(Player player, DominionDTO dominion,
                                cn.lunadeer.dominion.utils.dialogui.DialogResponse response) {
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

    private void teleport(Player player, DominionDTO dominion) {
        ui.submit(player, TeleportProvider.getInstance().teleport(player, dominion), accepted -> {
            if (accepted) ui.close(player);
        });
    }

    private static boolean isRemoteDominion(DominionDTO dominion) {
        return Configuration.multiServer.enable
                && dominion.getServerId() != Configuration.multiServer.serverId;
    }

    private void resize(Player player, DominionDTO dominion, String directionValue,
                        String modeValue, String amountValue) {
        try {
            int amount = Integer.parseInt(amountValue == null ? "" : amountValue.trim());
            if (amount <= 0) throw new NumberFormatException();
            DominionReSizeEvent.DIRECTION direction = switch (directionValue == null ? "" : directionValue) {
                case "north" -> DominionReSizeEvent.DIRECTION.NORTH;
                case "south" -> DominionReSizeEvent.DIRECTION.SOUTH;
                case "east" -> DominionReSizeEvent.DIRECTION.EAST;
                case "west" -> DominionReSizeEvent.DIRECTION.WEST;
                case "up" -> DominionReSizeEvent.DIRECTION.UP;
                case "down" -> DominionReSizeEvent.DIRECTION.DOWN;
                default -> throw new IllegalArgumentException();
            };
            DominionReSizeEvent.TYPE type = switch (modeValue == null ? "" : modeValue) {
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
}
