package cn.lunadeer.dominion.uis.chest;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.utils.chestui.ChestUiManager;
import cn.lunadeer.dominion.utils.chestui.MenuNavigator;
import cn.lunadeer.dominion.utils.chestui.MenuRoute;
import cn.lunadeer.dominion.utils.chestui.MenuSession;
import cn.lunadeer.dominion.utils.chestui.MenuView;
import cn.lunadeer.dominion.utils.chestui.MenuViewBuilder;
import cn.lunadeer.dominion.utils.chestui.TextRenderer;
import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import cn.lunadeer.dominion.utils.chestui.config.ItemAppearance;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Renders permission flags and the member/group management menus. */
final class BuiltinPermissionMenu extends AbstractBuiltinMenu {
    BuiltinPermissionMenu(ChestUiManager ui, ChestUiConfig config, MenuNavigator nav) {
        super(ui, config, nav);
    }

    MenuView permissions(Player player, MenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion);
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "permissions", values);
        view.item("info", values, null, null);
        view.item("environment", values, null, click -> nav.push(player, route(MenuId.ENV_FLAGS, dominion)));
        view.item("guest", values, null, click -> nav.push(player, route(MenuId.GUEST_FLAGS, dominion)));
        commonFooter(player, view);
        return view.build();
    }

    MenuView people(Player player, MenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion);
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "people", values);
        view.item("info", values, null, null);
        view.item("members", values, null, click -> nav.push(player, route(MenuId.MEMBER_LIST, dominion)));
        view.item("groups", values, null, click -> nav.push(player, route(MenuId.GROUP_LIST, dominion)));
        commonFooter(player, view);
        return view.build();
    }

    MenuView flagList(Player player, MenuSession session) {
        MenuRoute route = session.current();
        DominionDTO dominion = id(route) == MenuId.TEMPLATE_FLAGS
                ? null : requireDominion(player, route.integer("dom"));
        TemplateDTO template = id(route) == MenuId.TEMPLATE_FLAGS
                ? requireTemplate(player, route.integer("template")) : null;
        List<? extends Flag> flags = id(route) == MenuId.ENV_FLAGS
                ? Flags.getAllEnvFlagsEnable() : Flags.getAllPriFlagsEnable();
        String title = switch (id(route)) {
            case ENV_FLAGS -> configured("titles.environment-flags", Map.of("dominion", dominion.getName()));
            case GUEST_FLAGS -> configured("titles.guest-flags", Map.of("dominion", dominion.getName()));
            case MEMBER_FLAGS -> config.text("titles.member-flags");
            case GROUP_FLAGS -> config.text("titles.group-flags");
            case TEMPLATE_FLAGS -> configured("titles.template-flags", Map.of("template", template.getName()));
            default -> config.text("titles.flags");
        };
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "flag-list", Map.of("title", title));
        renderPage(view, route, flags, (slot, flag) -> renderFlag(
                player, view, route, dominion, template, slot, flag));
        listNavigation(player, view, route, flags.size());
        return view.build();
    }

    MenuView memberList(Player player, MenuSession session) {
        MenuRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        List<MemberDTO> members = dominion.getMembers().stream()
                .filter(member -> matches(route.filter(), member.getPlayer().getLastKnownName()))
                .sorted(Comparator.comparing(
                        member -> member.getPlayer().getLastKnownName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "member-list", dominionValues(dominion));
        renderPage(view, route, members, (slot, member) -> view.itemAt(slot, "content", "content",
                memberValues(dominion, member), member.getPlayerUUID(), click -> nav.push(player,
                        route(MenuId.MEMBER_DETAIL, dominion).with("member", member.getId()))));
        view.item("primary", Map.of(), null, click -> nav.push(player, route(MenuId.PLAYER_PICKER, dominion)));
        view.item("search", Map.of(), null, click -> search(player));
        listNavigation(player, view, route, members.size());
        return view.build();
    }

    MenuView memberDetail(Player player, MenuSession session) {
        MenuRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        MemberDTO member = requireMember(dominion, route.integer("member"));
        Map<String, Object> values = memberValues(dominion, member);
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "member-detail", values);
        view.item("info", values, member.getPlayerUUID(), null);
        view.item("flags", values, null, click -> nav.push(player,
                route(MenuId.MEMBER_FLAGS, dominion).with("member", member.getId())));
        view.item("template", values, null, click -> nav.push(player,
                route(MenuId.TEMPLATE_PICKER, dominion).with("member", member.getId())));
        view.item("group", values, null, click -> updateMemberGroup(player, dominion, member));
        view.item("remove", values, null, click -> ui.confirm(player,
                configured("confirm.remove-member", Map.of("player", member.getPlayer().getLastKnownName())),
                confirmed -> ui.submit(player, MemberProvider.getInstance().removeMember(player, dominion, member),
                        ignored -> nav.back(player))));
        commonFooter(player, view);
        return view.build();
    }

    MenuView groupList(Player player, MenuSession session) {
        MenuRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        List<GroupDTO> groups = dominion.getGroups().stream()
                .sorted(Comparator.comparing(GroupDTO::getNamePlain, String.CASE_INSENSITIVE_ORDER))
                .toList();
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "group-list", dominionValues(dominion));
        renderPage(view, route, groups, (slot, group) -> renderGroup(player, view, dominion, slot, group));
        view.item("primary", Map.of(), null, click -> ui.requestInput(player, config.text("input.create-group"),
                name -> ui.submit(player, GroupProvider.getInstance().createGroup(player, dominion, name),
                        ignored -> {})));
        listNavigation(player, view, route, groups.size());
        return view.build();
    }

    MenuView groupDetail(Player player, MenuSession session) throws Exception {
        MenuRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        GroupDTO group = requireGroup(dominion, route.integer("group"));
        Map<String, Object> values = Map.of(
                "dominion", dominion.getName(),
                "group", group.getNamePlain(),
                "members", group.getMembers().size());
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "group-detail", values);
        view.item("info", values, null, null);
        view.item("rename", values, null, click -> ui.requestInput(player, config.text("input.rename-group"),
                name -> ui.submit(player, GroupProvider.getInstance().renameGroup(player, dominion, group, name),
                        ignored -> {})));
        view.item("flags", values, null, click -> nav.push(player,
                route(MenuId.GROUP_FLAGS, dominion).with("group", group.getId())));
        view.item("add-member", values, null, click -> nav.push(player,
                route(MenuId.GROUP_MEMBER_PICKER, dominion).with("group", group.getId()).with("mode", "add")));
        view.item("members", values, null, click -> nav.push(player,
                route(MenuId.GROUP_MEMBER_PICKER, dominion).with("group", group.getId()).with("mode", "remove")));
        view.item("delete", values, null, click -> ui.confirm(player,
                configured("confirm.delete-group", Map.of("group", group.getNamePlain())), confirmed ->
                        ui.submit(player, GroupProvider.getInstance().deleteGroup(player, dominion, group),
                                ignored -> nav.back(player))));
        commonFooter(player, view);
        return view.build();
    }

    private DominionDTO currentDominion(Player player, MenuSession session) {
        return requireDominion(player, session.current().integer("dom"));
    }

    private void renderFlag(Player player, MenuViewBuilder view, MenuRoute route, DominionDTO dominion,
                            TemplateDTO template, int slot, Flag flag) {
        boolean state = flagState(route, dominion, template, flag);
        Map<String, Object> values = Map.of(
                "flag", flag.getDisplayName(),
                "description", flag.getDescription(),
                "state", TextRenderer.formatted(
                        state ? config.text("common.enabled") : config.text("common.disabled")));
        view.itemAt(slot, "content", "content", values, null,
                new ItemAppearance(flag.getMaterial(), 1, null, state, false),
                click -> toggleFlag(player, route, dominion, template, flag, !state));
    }

    private void renderGroup(Player player, MenuViewBuilder view, DominionDTO dominion, int slot, GroupDTO group) {
        int count;
        try {
            count = group.getMembers().size();
        } catch (Exception exception) {
            count = 0;
        }
        Map<String, Object> values = Map.of("group", group.getNamePlain(), "members", count);
        view.itemAt(slot, "content", "content", values, null, click -> nav.push(player,
                route(MenuId.GROUP_DETAIL, dominion).with("group", group.getId())));
    }

    private void updateMemberGroup(Player player, DominionDTO dominion, MemberDTO member) {
        if (member.getGroupId() == -1) {
            nav.push(player, route(MenuId.GROUP_MEMBER_PICKER, dominion)
                    .with("member", member.getId()).with("mode", "assign"));
            return;
        }
        GroupDTO group = api.getGroup(member.getGroupId());
        if (group != null) {
            ui.submit(player, GroupProvider.getInstance().removeMember(player, dominion, group, member), ignored -> {});
        }
    }

    private void toggleFlag(Player player, MenuRoute route, DominionDTO dominion, TemplateDTO template,
                            Flag flag, boolean value) {
        switch (id(route)) {
            case ENV_FLAGS -> ui.submit(player, DominionProvider.getInstance()
                    .setDominionEnvFlag(player, dominion, (EnvFlag) flag, value), ignored -> {});
            case GUEST_FLAGS -> ui.submit(player, DominionProvider.getInstance()
                    .setDominionGuestFlag(player, dominion, (PriFlag) flag, value), ignored -> {});
            case MEMBER_FLAGS -> ui.submit(player, MemberProvider.getInstance().setMemberFlag(player, dominion,
                    requireMember(dominion, route.integer("member")), (PriFlag) flag, value), ignored -> {});
            case GROUP_FLAGS -> ui.submit(player, GroupProvider.getInstance().setGroupFlag(player, dominion,
                    requireGroup(dominion, route.integer("group")), (PriFlag) flag, value), ignored -> {});
            case TEMPLATE_FLAGS -> ui.submit(player, TemplateProvider.getInstance().setTemplateFlag(
                    player, template, (PriFlag) flag, value), ignored -> {});
            default -> { }
        }
    }

    private boolean flagState(MenuRoute route, DominionDTO dominion, TemplateDTO template, Flag flag) {
        return switch (id(route)) {
            case ENV_FLAGS -> dominion.getEnvFlagValue((EnvFlag) flag);
            case GUEST_FLAGS -> dominion.getGuestFlagValue((PriFlag) flag);
            case MEMBER_FLAGS -> requireMember(dominion, route.integer("member")).getFlagValue((PriFlag) flag);
            case GROUP_FLAGS -> requireGroup(dominion, route.integer("group")).getFlagValue((PriFlag) flag);
            case TEMPLATE_FLAGS -> template.getFlagValue((PriFlag) flag);
            default -> false;
        };
    }
}
