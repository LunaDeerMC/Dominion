package cn.lunadeer.dominion.uis.dialog.pages.shared;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.handler.UiDataHandler;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogListStyle;
import cn.lunadeer.dominion.uis.dialog.components.DialogListTemplate;
import cn.lunadeer.dominion.uis.dialog.components.DialogTextRenderer;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static cn.lunadeer.dominion.Dominion.adminPermission;

/** Shared data, validation, navigation, and mutation helpers for pages. */
public abstract class AbstractDialogPage {
    protected final DialogMenuUi ui;
    protected final DialogUiText config;
    protected final DialogNavigator nav;
    protected final DominionAPI api;

    protected AbstractDialogPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
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
            ui.submit(player, DominionProvider.getInstance().createDominion(player, name,
                    player.getUniqueId(), player.getWorld(), cuboid, parent, false), created ->
                    nav.replace(player, DialogRoute.of(DialogMenuId.DASHBOARD).with("dom", created.getId())));
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

    protected void listNavigation(DominionDialogPage page, DialogRoute route,
                                  DialogPagination pagination, int total,
                                  DialogListStyle style) {
        DialogListTemplate.navigation(page, nav, route, pagination, style,
                DominionDialogPage.component(config.text("buttons.search.name")),
                (viewer, response) -> nav.push(viewer, DialogListTemplate.openSearch(route)),
                null, null);
        DialogListTemplate.summary(page, config, pagination, total);
    }

    protected DialogSpec listSearchPage(String menuId, Map<String, ?> titleValues,
                                        DialogRoute route, DialogListStyle style) {
        return DialogListTemplate.searchPage(config, menuId, titleValues, route, nav, style);
    }

    protected DialogSpec.ActionButton closeButton(DominionDialogPage page) {
        return page.button("close", Map.of(), page.layout().wideButtonWidth(),
                (viewer, response) -> ui.close(viewer));
    }

    protected List<DominionDTO> managedDominions(Player player) {
        return Stream.concat(api.getPlayerOwnDominionDTOs(player.getUniqueId()).stream(),
                        api.getPlayerAdminDominionDTOs(player.getUniqueId()).stream())
                .collect(java.util.stream.Collectors.toMap(DominionDTO::getId,
                        Function.identity(), (a, b) -> a))
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

    /** Builds a native player-head icon while tolerating stale stored skin URLs. */
    protected DialogSpec.PlayerHeadIcon playerHead(PlayerDTO player) {
        String skinTextureUrl = null;
        try {
            URL skin = player.getSkinUrl();
            if (skin != null) skinTextureUrl = skin.toExternalForm();
        } catch (MalformedURLException | RuntimeException ignored) {
            // PlayerHeadIcon replaces unavailable or malformed textures with built-in Steve.
        }
        return new DialogSpec.PlayerHeadIcon(
                player.getUuid(), player.getLastKnownName(), skinTextureUrl);
    }

    protected DominionDTO currentDominion(Player player, DialogMenuSession session) {
        return requireDominion(player, session.current().integer("dom"));
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

    protected void teleport(Player player, DominionDTO dominion) {
        ui.submit(player, cn.lunadeer.dominion.providers.TeleportProvider.getInstance()
                .teleport(player, dominion), accepted -> {
                    if (accepted) ui.close(player);
                });
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
