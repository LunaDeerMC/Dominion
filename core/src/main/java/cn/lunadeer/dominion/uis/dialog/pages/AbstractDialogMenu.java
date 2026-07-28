package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.handler.UiDataHandler;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static cn.lunadeer.dominion.Dominion.adminPermission;

/** Shared rendering and validation helpers for the built-in menu families. */
abstract class AbstractDialogMenu {
    protected final DialogMenuUi ui;
    protected final DialogUiText config;
    protected final DialogNavigator nav;
    protected final DominionAPI api;

    AbstractDialogMenu(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        this.ui = ui;
        this.config = config;
        this.nav = nav;
        this.api = Objects.requireNonNull(DominionAPI.getInstance());
    }

    protected void createDominionInput(Player player, DominionDTO parent) {
        if (Configuration.autoCreateRadius < 0) {
            Notification.error(player, config.text("errors.auto-create-disabled"));
            return;
        }
        ui.requestInput(player, config.text("input.create-dominion"), name -> {
            Location center = player.getLocation();
            int radius = Configuration.autoCreateRadius;
            Location first = center.clone().add(-radius, -radius, -radius);
            Location second = center.clone().add(radius, radius, radius);
            if (Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).autoIncludeVertical) {
                first.setY(Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).noLowerThan);
                second.setY(Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).noHigherThan - 1);
            }
            CuboidDTO cuboid = new CuboidDTO(first, second);
            ui.submit(player, DominionProvider.getInstance().createDominion(player, name, player.getUniqueId(),
                    player.getWorld(), cuboid, parent, false), created -> nav.replace(player,
                    DialogRoute.of(DialogMenuId.DASHBOARD).with("dom", created.getId())));
        });
    }

    protected void commonFooter(DominionDialogPage page) {
        page.backExit((viewer, response) -> nav.back(viewer));
    }

    protected void formFooter(DominionDialogPage page) {
        commonFooter(page);
    }

    protected DialogPagination pagination(DialogRoute route, int total, int perPage) {
        return DialogPagination.of(route.page(), total, perPage);
    }

    protected void listSearchAction(DominionDialogPage page, DialogRoute route) {
        page.action("search", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> {
                            String filter = response.getText("search");
                            nav.replace(viewer, route.filter(filter == null ? "" : filter.trim()));
                        })
                .action("clear", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.replace(viewer, route.filter("")));
    }

    protected void listNavigation(Player player, DominionDialogPage page, DialogRoute route,
                                  DialogPagination pagination, int total) {
        page.completeListRow();
        int pages = pagination.pages();
        boolean hasPrevious = route.page() > 1;
        boolean hasNext = route.page() < pages;
        if (hasPrevious) {
            page.action("previous", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                    (viewer, response) -> nav.replace(viewer, route.page(route.page() - 1)));
        }
        if (hasNext) {
            page.action("next", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                    (viewer, response) -> nav.replace(viewer, route.page(route.page() + 1)));
        }
        page.message(configured("common.page", Map.of(
                "page", pagination.page(), "pages", pages, "total", total)));
    }

    protected DialogSpec.ActionButton closeButton(Player player, DominionDialogPage page) {
        return page.button("close", Map.of(), DominionDialogPage.WIDE_BUTTON_WIDTH,
                (viewer, response) -> ui.close(viewer));
    }

    protected List<DominionDTO> managedDominions(Player player) {
        return Stream.concat(api.getPlayerOwnDominionDTOs(player.getUniqueId()).stream(),
                        api.getPlayerAdminDominionDTOs(player.getUniqueId()).stream())
                .collect(java.util.stream.Collectors.toMap(DominionDTO::getId, Function.identity(), (a, b) -> a))
                .values().stream().toList();
    }

    protected Map<String, Object> dominionValues(DominionDTO dominion, boolean remote) {
        CuboidDTO cuboid = dominion.getCuboid();
        World world = dominion.getWorld();
        String worldDisplay = world == null ? dominion.getWorldUid().toString() : world.getName();
        String serverDisplay = remote
                ? UiDataHandler.serverName(dominion.getServerId())
                : String.valueOf(dominion.getServerId());
        return Map.ofEntries(Map.entry("dominion", dominion.getName()),
                Map.entry("owner", dominion.getOwnerDTO().getLastKnownName()),
                Map.entry("world", remote ? "" : worldDisplay),
                Map.entry("server", serverDisplay),
                Map.entry("size", cuboid.xLength() + " × " + cuboid.yLength() + " × " + cuboid.zLength()),
                Map.entry("bounds", cuboid.x1() + "," + cuboid.y1() + "," + cuboid.z1() + " → "
                        + cuboid.x2() + "," + cuboid.y2() + "," + cuboid.z2()),
                Map.entry("join-message", dominion.getJoinMessage()),
                Map.entry("leave-message", dominion.getLeaveMessage()),
                Map.entry("color", dominion.getColor()));
    }

    protected Map<String, Object> memberValues(DominionDTO dominion, MemberDTO member) {
        GroupDTO group = api.getGroup(member);
        boolean admin = group != null
                ? group.getFlagValue(cn.lunadeer.dominion.api.dtos.flag.Flags.ADMIN)
                : member.getFlagValue(cn.lunadeer.dominion.api.dtos.flag.Flags.ADMIN);
        return Map.of("dominion", dominion.getName(),
                "player", member.getPlayer().getLastKnownName(),
                "group", group == null ? config.text("labels.none") : group.getNamePlain(),
                "role", config.text(admin ? "labels.administrator" : "labels.member"));
    }

    protected DominionDTO requireDominion(Player player, int id) {
        DominionDTO value = api.getDominion(id);
        if (value == null) throw new IllegalStateException("Dominion no longer exists: " + id);
        if (!player.hasPermission(adminPermission)
                && managedDominions(player).stream().noneMatch(dominion -> dominion.getId().equals(id))) {
            throw new IllegalStateException("You no longer have permission to manage " + value.getName());
        }
        return value;
    }

    protected MemberDTO requireMember(DominionDTO dominion, int id) {
        return dominion.getMembers().stream().filter(member -> member.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalStateException("Member no longer exists: " + id));
    }

    protected GroupDTO requireGroup(DominionDTO dominion, int id) {
        return dominion.getGroups().stream().filter(group -> group.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalStateException("Group no longer exists: " + id));
    }

    protected TemplateDTO requireTemplate(Player player, int id) {
        TemplateDTO value = TemplateProvider.getInstance().getTemplate(player.getUniqueId(), id);
        if (value == null) throw new IllegalStateException("Template no longer exists: " + id);
        return value;
    }

    protected static boolean matches(String filter, String value) {
        return filter == null || filter.isBlank()
                || value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    protected static DialogRoute route(DialogMenuId id, DominionDTO dominion) {
        return DialogRoute.of(id).with("dom", dominion.getId());
    }

    protected static DialogMenuId id(DialogRoute route) {
        return DialogMenuId.valueOf(route.id());
    }

    protected String configured(String path, Map<String, ?> values) {
        return DialogTextRenderer.replaceNamed(config.text(path), values);
    }
}
