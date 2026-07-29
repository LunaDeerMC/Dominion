package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroup;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogResponse;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Native permission forms and member/group management dialogs. */
final class DialogPermissionMenu extends AbstractDialogMenu {
    DialogPermissionMenu(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    DialogSpec permissions(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "permissions", values)
                .summary("info", values, Material.WRITABLE_BOOK)
                .action("environment", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.ENV_FLAGS, dominion)))
                .action("guest", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.GUEST_FLAGS, dominion)));
        commonFooter(page);
        return page.build(2, null);
    }

    DialogSpec people(Player player, DialogMenuSession session) {
        DominionDTO dominion = currentDominion(player, session);
        Map<String, Object> values = dominionValues(dominion, false);
        DominionDialogPage page = new DominionDialogPage(config, "people", values)
                .summary("info", values, Material.PLAYER_HEAD)
                .action("members", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.MEMBER_LIST, dominion)))
                .action("groups", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer, route(DialogMenuId.GROUP_LIST, dominion)));
        commonFooter(page);
        return page.build(2, null);
    }

    DialogSpec flagGroupList(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DialogMenuId context = id(route);
        DominionDTO dominion = context == DialogMenuId.TEMPLATE_FLAGS
                ? null : requireDominion(player, route.integer("dom"));
        TemplateDTO template = context == DialogMenuId.TEMPLATE_FLAGS
                ? requireTemplate(player, route.integer("template")) : null;
        List<FlagGroup<?>> groups = new ArrayList<>(context == DialogMenuId.ENV_FLAGS
                ? FlagGroups.getEnvFlagGroups() : FlagGroups.getPriFlagGroups());
        FlagGroup<?> ungrouped = context == DialogMenuId.ENV_FLAGS
                ? FlagGroups.getUngroupedEnvFlags() : FlagGroups.getUngroupedPriFlags();
        if (ungrouped.getFlags().stream().anyMatch(Flag::getEnable)) groups.add(ungrouped);
        groups = groups.stream()
                .filter(group -> group.getFlags().stream().anyMatch(Flag::getEnable))
                .toList();
        String title = flagTitle(context, dominion, template);
        DominionDialogPage page = new DominionDialogPage(
                config, "flag-group-list", Map.of("title", title))
                .keepOpenAfterAction();
        DialogPagination pagination = pagination(
                route, groups.size(), DominionDialogPage.LIST_PAGE_SIZE);
        for (FlagGroup<?> group : groups.subList(pagination.from(), pagination.to())) {
            List<? extends Flag> flags = group.getFlags().stream().filter(Flag::getEnable).toList();
            long enabled = flags.stream()
                    .filter(flag -> flagState(context, route, dominion, template, flag))
                    .count();
            Map<String, Object> values = Map.of(
                    "group", group.getId().equals("ungrouped")
                            ? config.text("labels.ungrouped") : group.getDisplayName(),
                    "description", group.getId().equals("ungrouped")
                            ? config.text("labels.ungrouped-description") : group.getDescription(),
                    "enabled", enabled,
                    "total", flags.size());
            Component tooltip = DominionDialogPage.component(
                    configured("descriptions.flag-group", values));
            page.listAction(
                    DominionDialogPage.component(String.valueOf(values.get("group"))),
                    tooltip,
                    (viewer, response) -> nav.push(viewer,
                            groupRoute(route, context, group.getId())));
        }
        listNavigation(player, page, route, pagination, groups.size());
        commonFooter(page);
        return page.buildList();
    }

    DialogSpec flagList(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DialogMenuId context = flagContext(route);
        DominionDTO dominion = context == DialogMenuId.TEMPLATE_FLAGS
                ? null : requireDominion(player, route.integer("dom"));
        TemplateDTO template = context == DialogMenuId.TEMPLATE_FLAGS
                ? requireTemplate(player, route.integer("template")) : null;
        FlagGroup<?> group = requireFlagGroup(route, context);
        List<? extends Flag> flags = group.getFlags().stream().filter(Flag::getEnable).toList();
        String title = configured("titles.flag-group", Map.of(
                "title", flagTitle(context, dominion, template),
                "group", group.getId().equals("ungrouped")
                        ? config.text("labels.ungrouped") : group.getDisplayName()));
        String draftPrefix = flagDraftPrefix(route, context, group.getId());
        DialogPagination pagination = pagination(
                route, flags.size(), DominionDialogPage.FLAG_PAGE_SIZE);
        List<? extends Flag> visible = flags.subList(pagination.from(), pagination.to());
        DominionDialogPage page = new DominionDialogPage(config, "flag-list", Map.of("title", title))
                .message(configured("common.page", Map.of(
                        "page", pagination.page(),
                        "pages", pagination.pages(),
                        "total", flags.size())));
        for (Flag flag : visible) {
            Boolean draft = session.state(draftPrefix + flag.getFlagName(), Boolean.class);
            boolean state = draft == null
                    ? flagState(context, route, dominion, template, flag)
                    : draft;
            Component label = Component.text(flag.getDisplayName())
                    .append(Component.text(" — " + flag.getDescription(), NamedTextColor.GRAY));
            page.booleanInput(flagInputKey(flag), label, state);
        }
        boolean bulkTarget = FlagGroupActions.bulkTarget(
                flags, flag -> flagState(context, route, dominion, template, flag));
        page.action(
                DominionDialogPage.component(config.text(
                        bulkTarget ? "labels.enable-all" : "labels.disable-all")),
                DominionDialogPage.component(group.getDescription()),
                DominionDialogPage.TWO_COLUMN_WIDTH,
                (viewer, response) -> toggleFlagGroup(
                        viewer, route, context, dominion, template, flags));
        if (pagination.page() > 1) {
            page.action("previous", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                    (viewer, response) -> {
                        storeFlagDraft(session, draftPrefix, visible, response);
                        nav.replace(viewer, route.page(pagination.page() - 1));
                    });
        }
        if (pagination.page() < pagination.pages()) {
            page.action("next", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                    (viewer, response) -> {
                        storeFlagDraft(session, draftPrefix, visible, response);
                        nav.replace(viewer, route.page(pagination.page() + 1));
                    });
        }
        page.action("save", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                (viewer, response) -> {
                    storeFlagDraft(session, draftPrefix, visible, response);
                    saveFlagDraft(viewer, session, draftPrefix, route,
                            context, dominion, template, flags);
                });
        page.backExit((viewer, response) -> {
            session.clearState(draftPrefix);
            nav.back(viewer);
        });
        return page.build(2, null);
    }

    DialogSpec memberList(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        List<MemberDTO> members = dominion.getMembers().stream()
                .filter(member -> matches(route.filter(), member.getPlayer().getLastKnownName()))
                .sorted(Comparator.comparing(
                        member -> member.getPlayer().getLastKnownName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        DominionDialogPage page = new DominionDialogPage(
                config, "member-list", dominionValues(dominion, false))
                .textInput("search", "input.search", route.filter(), 128)
                .keepOpenAfterAction()
                .action("primary", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.PLAYER_PICKER, dominion)))
                .action("search", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> {
                            String filter = response.getText("search");
                            nav.replace(viewer, route.filter(
                                    filter == null ? "" : filter.trim()));
                        });
        DialogPagination pagination = pagination(route, members.size(), DominionDialogPage.LIST_PAGE_SIZE);
        for (MemberDTO member : members.subList(pagination.from(), pagination.to())) {
            Map<String, Object> values = memberValues(dominion, member);
            Component tooltip = DominionDialogPage.component(
                    configured("descriptions.member-entry", values));
            page.listAction(
                    DominionDialogPage.component(member.getPlayer().getLastKnownName()),
                    tooltip,
                    (viewer, response) -> nav.push(viewer,
                            route(DialogMenuId.MEMBER_DETAIL, dominion)
                                    .with("member", member.getId())));
        }
        listNavigation(player, page, route, pagination, members.size());
        commonFooter(page);
        return page.buildList();
    }

    DialogSpec memberDetail(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        MemberDTO member = requireMember(dominion, route.integer("member"));
        Map<String, Object> values = memberValues(dominion, member);
        DominionDialogPage page = new DominionDialogPage(config, "member-detail", values)
                .summary("info", values, Material.PLAYER_HEAD)
                .action("flags", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.MEMBER_FLAGS, dominion)
                                        .with("member", member.getId())))
                .action("template", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.TEMPLATE_PICKER, dominion)
                                        .with("member", member.getId())));
        boolean assigned = member.getGroupId() != -1;
        page.action(
                        DominionDialogPage.component(configured(
                                assigned ? "labels.remove-from-group" : "labels.assign-to-group",
                                values)),
                        DominionDialogPage.component(configured(
                                assigned ? "descriptions.remove-from-group"
                                        : "descriptions.assign-to-group",
                                values)),
                        DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> updateMemberGroup(
                                viewer, dominion, member))
                .action("remove", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> ui.confirm(viewer,
                                configured("confirm.remove-member", Map.of(
                                        "player", member.getPlayer().getLastKnownName())),
                                confirmed -> ui.submit(viewer,
                                        MemberProvider.getInstance().removeMember(
                                                viewer, dominion, member),
                                        ignored -> nav.back(viewer))));
        commonFooter(page);
        return page.build(2, null);
    }

    DialogSpec groupList(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        List<GroupDTO> groups = dominion.getGroups().stream()
                .sorted(Comparator.comparing(GroupDTO::getNamePlain, String.CASE_INSENSITIVE_ORDER))
                .toList();
        DominionDialogPage page = new DominionDialogPage(
                config, "group-list", dominionValues(dominion, false))
                .textInput("group_name", "input.create-group", "", 128)
                .keepOpenAfterAction()
                .action("primary", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH, (viewer, response) -> {
                    String name = response.getText("group_name");
                    if (name == null || name.isBlank()) return;
                    ui.submit(viewer,
                            GroupProvider.getInstance().createGroup(viewer, dominion, name.trim()),
                            ignored -> {});
                })
                .action("refresh", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.refresh(viewer));
        DialogPagination pagination = pagination(route, groups.size(), DominionDialogPage.LIST_PAGE_SIZE);
        for (GroupDTO group : groups.subList(pagination.from(), pagination.to())) {
            int members;
            try {
                members = group.getMembers().size();
            } catch (Exception exception) {
                members = 0;
            }
            Map<String, Object> values = Map.of(
                    "group", group.getNamePlain(), "members", members);
            Component tooltip = DominionDialogPage.component(
                    configured("descriptions.group-entry", values));
            page.listAction(
                    DominionDialogPage.component(group.getNamePlain()),
                    tooltip,
                    (viewer, response) -> nav.push(viewer,
                            route(DialogMenuId.GROUP_DETAIL, dominion)
                                    .with("group", group.getId())));
        }
        listNavigation(player, page, route, pagination, groups.size());
        commonFooter(page);
        return page.buildList();
    }

    DialogSpec groupDetail(Player player, DialogMenuSession session) throws Exception {
        DialogRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        GroupDTO group = requireGroup(dominion, route.integer("group"));
        Map<String, Object> values = Map.of(
                "dominion", dominion.getName(),
                "group", group.getNamePlain(),
                "members", group.getMembers().size());
        DominionDialogPage page = new DominionDialogPage(config, "group-detail", values)
                .summary("info", values, Material.BOOK)
                .textInput("group_name", "input.rename-group", group.getNamePlain(), 128)
                .action("save", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> {
                            String name = response.getText("group_name");
                            if (name == null || name.isBlank() || name.equals(group.getNamePlain())) {
                                nav.refresh(viewer);
                                return;
                            }
                            ui.submit(viewer,
                                    GroupProvider.getInstance().renameGroup(
                                            viewer, dominion, group, name.trim()),
                                    ignored -> {});
                        })
                .action("flags", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.GROUP_FLAGS, dominion)
                                        .with("group", group.getId())))
                .action("add-member", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.GROUP_MEMBER_PICKER, dominion)
                                        .with("group", group.getId()).with("mode", "add")))
                .action("members", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer,
                                route(DialogMenuId.GROUP_MEMBER_PICKER, dominion)
                                        .with("group", group.getId()).with("mode", "remove")))
                .action("delete", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> ui.confirm(viewer,
                                configured("confirm.delete-group",
                                        Map.of("group", group.getNamePlain())),
                                confirmed -> ui.submit(viewer,
                                        GroupProvider.getInstance().deleteGroup(
                                                viewer, dominion, group),
                                        ignored -> nav.back(viewer))));
        commonFooter(page);
        return page.build(2, null);
    }

    private DominionDTO currentDominion(Player player, DialogMenuSession session) {
        return requireDominion(player, session.current().integer("dom"));
    }

    private String flagTitle(DialogMenuId context, DominionDTO dominion, TemplateDTO template) {
        return switch (context) {
            case ENV_FLAGS -> configured(
                    "titles.environment-flags", Map.of("dominion", dominion.getName()));
            case GUEST_FLAGS -> configured(
                    "titles.guest-flags", Map.of("dominion", dominion.getName()));
            case MEMBER_FLAGS -> config.text("titles.member-flags");
            case GROUP_FLAGS -> config.text("titles.group-flags");
            case TEMPLATE_FLAGS -> configured(
                    "titles.template-flags", Map.of("template", template.getName()));
            default -> config.text("titles.flags");
        };
    }

    private void storeFlagDraft(DialogMenuSession session, String prefix,
                                List<? extends Flag> flags, DialogResponse response) {
        for (Flag flag : flags) {
            Boolean value = response.getBoolean(flagInputKey(flag));
            if (value != null) session.state(prefix + flag.getFlagName(), value);
        }
    }

    private void saveFlagDraft(Player player, DialogMenuSession session, String prefix,
                               DialogRoute route, DialogMenuId context, DominionDTO dominion,
                               TemplateDTO template, List<? extends Flag> flags) {
        boolean changed = flags.stream().anyMatch(flag -> {
            Boolean target = session.state(prefix + flag.getFlagName(), Boolean.class);
            return target != null
                    && target != flagState(context, route, dominion, template, flag);
        });
        if (!changed) {
            session.clearState(prefix);
            nav.refresh(player);
            return;
        }
        CompletableFuture<FlagGroupActions.Result> future = FlagGroupActions.applySequentially(
                flags,
                flag -> {
                    Boolean target = session.state(prefix + flag.getFlagName(), Boolean.class);
                    return target != null
                            && target != flagState(context, route, dominion, template, flag);
                },
                command -> Scheduler.runEntityTask(command, player),
                flag -> setFlag(player, route, context, dominion, template, flag,
                        Boolean.TRUE.equals(session.state(
                                prefix + flag.getFlagName(), Boolean.class))));
        ui.submit(player, future, result -> {
            session.clearState(prefix);
            result.failures().forEach(throwable -> Notification.error(player, throwable));
        });
    }

    private void updateMemberGroup(Player player, DominionDTO dominion, MemberDTO member) {
        if (member.getGroupId() == -1) {
            nav.push(player, route(DialogMenuId.GROUP_MEMBER_PICKER, dominion)
                    .with("member", member.getId()).with("mode", "assign"));
            return;
        }
        GroupDTO group = api.getGroup(member.getGroupId());
        if (group != null) {
            ui.submit(player,
                    GroupProvider.getInstance().removeMember(player, dominion, group, member),
                    ignored -> {});
        }
    }

    private void toggleFlagGroup(Player player, DialogRoute route, DialogMenuId context,
                                 DominionDTO dominion, TemplateDTO template,
                                 List<? extends Flag> flags) {
        boolean target = FlagGroupActions.bulkTarget(
                flags, flag -> flagState(context, route, dominion, template, flag));
        ui.submit(player, FlagGroupActions.applySequentially(
                flags,
                flag -> flagState(context, route, dominion, template, flag) != target,
                command -> Scheduler.runEntityTask(command, player),
                flag -> setFlag(player, route, context, dominion, template, flag, target)
        ), result -> result.failures().forEach(throwable -> Notification.error(player, throwable)));
    }

    private CompletableFuture<?> setFlag(Player player, DialogRoute route, DialogMenuId context,
                                         DominionDTO dominion, TemplateDTO template,
                                         Flag flag, boolean value) {
        return switch (context) {
            case ENV_FLAGS -> DominionProvider.getInstance()
                    .setDominionEnvFlag(player, dominion, (EnvFlag) flag, value);
            case GUEST_FLAGS -> DominionProvider.getInstance()
                    .setDominionGuestFlag(player, dominion, (PriFlag) flag, value);
            case MEMBER_FLAGS -> MemberProvider.getInstance().setMemberFlag(
                    player, dominion, requireMember(dominion, route.integer("member")),
                    (PriFlag) flag, value);
            case GROUP_FLAGS -> GroupProvider.getInstance().setGroupFlag(
                    player, dominion, requireGroup(dominion, route.integer("group")),
                    (PriFlag) flag, value);
            case TEMPLATE_FLAGS -> TemplateProvider.getInstance()
                    .setTemplateFlag(player, template, (PriFlag) flag, value);
            default -> CompletableFuture.completedFuture(null);
        };
    }

    private boolean flagState(DialogMenuId context, DialogRoute route, DominionDTO dominion,
                              TemplateDTO template, Flag flag) {
        return switch (context) {
            case ENV_FLAGS -> dominion.getEnvFlagValue((EnvFlag) flag);
            case GUEST_FLAGS -> dominion.getGuestFlagValue((PriFlag) flag);
            case MEMBER_FLAGS -> requireMember(dominion, route.integer("member"))
                    .getFlagValue((PriFlag) flag);
            case GROUP_FLAGS -> requireGroup(dominion, route.integer("group"))
                    .getFlagValue((PriFlag) flag);
            case TEMPLATE_FLAGS -> template.getFlagValue((PriFlag) flag);
            default -> false;
        };
    }

    private FlagGroup<?> requireFlagGroup(DialogRoute route, DialogMenuId context) {
        String groupId = route.string("flag-group");
        FlagGroup<?> group;
        if (context == DialogMenuId.ENV_FLAGS) {
            group = groupId.equals("ungrouped")
                    ? FlagGroups.getUngroupedEnvFlags()
                    : FlagGroups.getEnvFlagGroup(groupId);
        } else {
            group = groupId.equals("ungrouped")
                    ? FlagGroups.getUngroupedPriFlags()
                    : FlagGroups.getPriFlagGroup(groupId);
        }
        if (group == null) {
            throw new IllegalStateException("Flag group no longer exists: " + groupId);
        }
        return group;
    }

    private DialogRoute groupRoute(DialogRoute source, DialogMenuId context, String groupId) {
        return new DialogRoute(DialogMenuId.FLAG_LIST.name(), source.parameters(), 1, "")
                .with("context", context.name())
                .with("flag-group", groupId);
    }

    private DialogMenuId flagContext(DialogRoute route) {
        return id(route) == DialogMenuId.FLAG_LIST
                ? DialogMenuId.valueOf(route.string("context"))
                : id(route);
    }

    private static String flagInputKey(Flag flag) {
        return "flag_" + flag.getFlagName();
    }

    private static String flagDraftPrefix(DialogRoute route, DialogMenuId context, String groupId) {
        return "flag-draft:" + context + ':' + route.parameters() + ':' + groupId + ':';
    }
}
