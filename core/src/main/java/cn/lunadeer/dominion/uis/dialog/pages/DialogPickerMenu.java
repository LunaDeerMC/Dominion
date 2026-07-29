package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.misc.Others;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.providers.PlayerProvider;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native two-column selectors for templates, groups, members, and players. */
final class DialogPickerMenu extends AbstractDialogMenu {
    DialogPickerMenu(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    DialogSpec picker(Player player, DialogMenuSession session) throws Exception {
        DialogRoute route = session.current();
        DominionDTO dominion = route.parameters().containsKey("dom")
                ? requireDominion(player, route.integer("dom")) : null;
        List<PickerEntry> entries = new ArrayList<>();
        String title;
        if (id(route) == DialogMenuId.TEMPLATE_PICKER) {
            title = config.text("titles.select-template");
            addTemplates(player, route, dominion, entries);
        } else if (id(route) == DialogMenuId.GROUP_MEMBER_PICKER && "assign".equals(route.string("mode"))) {
            title = config.text("titles.assign-group");
            addGroups(player, route, dominion, entries);
        } else if (id(route) == DialogMenuId.GROUP_MEMBER_PICKER) {
            boolean remove = "remove".equals(route.string("mode"));
            title = config.text(remove ? "titles.remove-group-member" : "titles.add-group-member");
            addGroupMembers(player, route, dominion, entries, remove);
        } else if (id(route) == DialogMenuId.ADMIN_PLAYER_DOMINIONS) {
            title = config.text("titles.admin-player-dominions");
            addKnownPlayers(player, null, entries, false);
            for (int index = 0; index < entries.size(); index++) {
                PickerEntry original = entries.get(index);
                entries.set(index, new PickerEntry(
                        original.name(), original.description(), original.insideDominion(), original.online(),
                        () -> nav.push(player, DialogRoute.of(DialogMenuId.PLAYER_DOMINIONS)
                                .with("player", original.playerId().toString())
                                .with("playerName", original.name())),
                        original.playerId()));
            }
        } else {
            boolean transfer = id(route) == DialogMenuId.TRANSFER_PICKER;
            title = config.text(transfer ? "titles.transfer-player" : "titles.add-member");
            addKnownPlayers(player, dominion, entries, transfer);
        }

        entries = entries.stream()
                .filter(entry -> matches(route.filter(), entry.name()))
                .sorted(Comparator.comparing(PickerEntry::online).reversed()
                        .thenComparing(PickerEntry::insideDominion).reversed()
                        .thenComparing(PickerEntry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        DominionDialogPage page = new DominionDialogPage(config, "picker-list", Map.of("title", title))
                .keepOpenAfterAction();
        boolean searchable = id(route) != DialogMenuId.TEMPLATE_PICKER;
        if (searchable) {
            page.textInput("search", "input.search", route.filter(), 128);
            listSearchAction(page, route);
        }
        DialogPagination pagination = pagination(route, entries.size(), DominionDialogPage.LIST_PAGE_SIZE);
        for (PickerEntry entry : entries.subList(pagination.from(), pagination.to())) {
            String status = entry.online()
                    ? config.text("labels.online")
                    : entry.insideDominion()
                            ? config.text("labels.inside-dominion")
                            : entry.description();
            var tooltip = DominionDialogPage.component(status);
            page.listAction(
                    DominionDialogPage.component(entry.name()),
                    tooltip,
                    (viewer, response) -> entry.action().run());
        }
        listNavigation(player, page, route, pagination, entries.size());
        commonFooter(page);
        return page.buildList();
    }

    private void addTemplates(Player player, DialogRoute route, DominionDTO dominion,
                              List<PickerEntry> entries) {
        for (TemplateDTO template : TemplateProvider.getInstance().getTemplates(player.getUniqueId())) {
            entries.add(new PickerEntry(
                    template.getName(),
                    config.text("labels.template"),
                    false,
                    false,
                    () -> {
                        MemberDTO member = requireMember(dominion, route.integer("member"));
                        ui.submit(player,
                                TemplateProvider.getInstance().applyTemplate(player, dominion, member, template),
                                ignored -> nav.back(player));
                    },
                    null));
        }
    }

    private void addGroups(Player player, DialogRoute route, DominionDTO dominion,
                           List<PickerEntry> entries) {
        MemberDTO member = requireMember(dominion, route.integer("member"));
        for (GroupDTO group : dominion.getGroups()) {
            entries.add(new PickerEntry(
                    group.getNamePlain(),
                    config.text("labels.group"),
                    false,
                    false,
                    () -> ui.submit(player,
                            GroupProvider.getInstance().addMember(player, dominion, group, member),
                            ignored -> nav.back(player)),
                    null));
        }
    }

    private void addGroupMembers(Player player, DialogRoute route, DominionDTO dominion,
                                 List<PickerEntry> entries, boolean remove) throws Exception {
        GroupDTO group = requireGroup(dominion, route.integer("group"));
        List<MemberDTO> members = remove
                ? group.getMembers()
                : dominion.getMembers().stream()
                        .filter(member -> !group.getId().equals(member.getGroupId()))
                        .toList();
        for (MemberDTO member : members) {
            entries.add(new PickerEntry(
                    member.getPlayer().getLastKnownName(),
                    config.text("labels.player"),
                    false,
                    false,
                    () -> ui.submit(player,
                            remove
                                    ? GroupProvider.getInstance().removeMember(player, dominion, group, member)
                                    : GroupProvider.getInstance().addMember(player, dominion, group, member),
                            ignored -> nav.back(player)),
                    member.getPlayerUUID()));
        }
    }

    private void addKnownPlayers(Player player, DominionDTO dominion,
                                 List<PickerEntry> entries, boolean transfer) {
        Set<UUID> excluded = new HashSet<>();
        if (dominion != null) {
            excluded.add(dominion.getOwner());
            dominion.getMembers().forEach(member -> excluded.add(member.getPlayerUUID()));
        }
        for (PlayerDTO candidate : PlayerProvider.getInstance().getKnownPlayers()) {
            if ((!transfer && excluded.contains(candidate.getUuid()))
                    || (transfer && candidate.getUuid().equals(dominion.getOwner()))) {
                continue;
            }
            Player onlinePlayer = Bukkit.getPlayer(candidate.getUuid());
            boolean online = onlinePlayer != null && onlinePlayer.isOnline();
            boolean insideDominion = dominion != null && online
                    && Others.isInDominion(dominion, onlinePlayer.getLocation());
            String description = online
                    ? config.text("labels.online")
                    : insideDominion
                            ? config.text("labels.inside-dominion")
                            : config.text("labels.known-player");
            entries.add(new PickerEntry(
                    candidate.getLastKnownName(),
                    description,
                    insideDominion,
                    online,
                    () -> selectKnownPlayer(player, dominion, candidate, transfer),
                    candidate.getUuid()));
        }
    }

    private void selectKnownPlayer(Player player, DominionDTO dominion,
                                   PlayerDTO candidate, boolean transfer) {
        if (transfer) {
            ui.confirm(player, configured("confirm.transfer", Map.of(
                    "dominion", dominion.getName(),
                    "player", candidate.getLastKnownName())), confirmed ->
                    ui.submit(player,
                            DominionProvider.getInstance().transferDominion(player, dominion, candidate, true),
                            ignored -> nav.home(player)));
        } else {
            ui.submit(player, MemberProvider.getInstance().addMember(player, dominion, candidate),
                    ignored -> nav.back(player));
        }
    }

    private record PickerEntry(
            String name,
            String description,
            boolean insideDominion,
            boolean online,
            Runnable action,
            UUID playerId
    ) {
    }
}
