package cn.lunadeer.dominion.uis.chest;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.chestui.ChestUiManager;
import cn.lunadeer.dominion.utils.chestui.MenuNavigator;
import cn.lunadeer.dominion.utils.chestui.MenuRoute;
import cn.lunadeer.dominion.utils.chestui.MenuViewBuilder;
import cn.lunadeer.dominion.utils.chestui.Pagination;
import cn.lunadeer.dominion.utils.chestui.TextRenderer;
import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static cn.lunadeer.dominion.Dominion.adminPermission;

/** Shared rendering and validation helpers for the built-in menu families. */
abstract class AbstractBuiltinMenu {
    protected final ChestUiManager ui;
    protected final ChestUiConfig config;
    protected final MenuNavigator nav;
    protected final DominionAPI api;

    AbstractBuiltinMenu(ChestUiManager ui, ChestUiConfig config, MenuNavigator nav) {
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
                    MenuRoute.of(MenuId.DASHBOARD).with("dom", created.getId())));
        });
    }

    protected void search(Player player) {
        ui.requestInput(player, config.text("input.search"), value ->
                nav.replace(player, ui.session(player).current().filter(value)));
    }

    protected void commonFooter(Player player, MenuViewBuilder view) {
        view.item("back", Map.of(), null, click -> nav.back(player));
        view.item("home", Map.of(), null, click -> nav.home(player));
        view.item("close", Map.of(), null, click -> ui.close(player));
    }

    protected void listNavigation(Player player, MenuViewBuilder view, MenuRoute route, int total) {
        int perPage = Math.max(1, view.slots("content").size());
        Pagination pagination = Pagination.of(route.page(), total, perPage);
        int pages = pagination.pages();
        if (route.page() > 1) {
            view.item("previous", Map.of(), null, click -> nav.replace(player, route.page(route.page() - 1)));
        }
        if (route.page() < pages) {
            view.item("next", Map.of(), null, click -> nav.replace(player, route.page(route.page() + 1)));
        }
        view.item("status", Map.of("page", pagination.page(), "pages", pages, "total", total), null, null);
        view.item("back", Map.of(), null, click -> nav.back(player));
        view.item("close", Map.of(), null, click -> ui.close(player));
    }

    protected <T> void renderPage(MenuViewBuilder view, MenuRoute route, List<T> values,
                                  BiConsumer<Integer, T> renderer) {
        List<Integer> slots = view.slots("content");
        if (slots.isEmpty()) return;
        Pagination pagination = Pagination.of(route.page(), values.size(), slots.size());
        for (int index = 0; pagination.from() + index < pagination.to(); index++) {
            renderer.accept(slots.get(index), values.get(pagination.from() + index));
        }
    }

    protected List<DominionDTO> managedDominions(Player player) {
        return Stream.concat(api.getPlayerOwnDominionDTOs(player.getUniqueId()).stream(),
                        api.getPlayerAdminDominionDTOs(player.getUniqueId()).stream())
                .collect(java.util.stream.Collectors.toMap(DominionDTO::getId, Function.identity(), (a, b) -> a))
                .values().stream().toList();
    }

    protected Map<String, Object> dominionValues(DominionDTO dominion) {
        CuboidDTO cuboid = dominion.getCuboid();
        World world = dominion.getWorld();
        return Map.ofEntries(Map.entry("dominion", dominion.getName()),
                Map.entry("owner", dominion.getOwnerDTO().getLastKnownName()),
                Map.entry("world", world == null ? dominion.getWorldUid().toString() : world.getName()),
                Map.entry("server", dominion.getServerId()),
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

    protected static MenuRoute route(MenuId id, DominionDTO dominion) {
        return MenuRoute.of(id).with("dom", dominion.getId());
    }

    protected static MenuId id(MenuRoute route) {
        return MenuId.valueOf(route.id());
    }

    protected String configured(String path, Map<String, ?> values) {
        return TextRenderer.replaceNamed(config.text(path), values);
    }
}
