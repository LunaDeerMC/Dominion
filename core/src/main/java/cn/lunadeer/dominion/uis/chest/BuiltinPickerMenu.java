package cn.lunadeer.dominion.uis.chest;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.providers.PlayerProvider;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.utils.chestui.ChestUiManager;
import cn.lunadeer.dominion.utils.chestui.MenuNavigator;
import cn.lunadeer.dominion.utils.chestui.MenuRoute;
import cn.lunadeer.dominion.utils.chestui.MenuSession;
import cn.lunadeer.dominion.utils.chestui.MenuView;
import cn.lunadeer.dominion.utils.chestui.MenuViewBuilder;
import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Builds reusable selection lists for templates, groups, members, and known players. */
final class BuiltinPickerMenu extends AbstractBuiltinMenu {
    BuiltinPickerMenu(ChestUiManager ui, ChestUiConfig config, MenuNavigator nav) {
        super(ui, config, nav);
    }

    MenuView picker(Player player, MenuSession session) throws Exception {
        MenuRoute route = session.current();
        DominionDTO dominion = route.parameters().containsKey("dom")
                ? requireDominion(player, route.integer("dom")) : null;
        List<PickerEntry> entries = new ArrayList<>();
        String title;
        if (id(route) == MenuId.TEMPLATE_PICKER) {
            title = config.text("titles.select-template");
            addTemplates(player, route, dominion, entries);
        } else if (id(route) == MenuId.GROUP_MEMBER_PICKER && "assign".equals(route.string("mode"))) {
            title = config.text("titles.assign-group");
            addGroups(player, route, dominion, entries);
        } else if (id(route) == MenuId.GROUP_MEMBER_PICKER) {
            boolean remove = "remove".equals(route.string("mode"));
            title = config.text(remove ? "titles.remove-group-member" : "titles.add-group-member");
            addGroupMembers(player, route, dominion, entries, remove);
        } else {
            boolean transfer = id(route) == MenuId.TRANSFER_PICKER;
            title = config.text(transfer ? "titles.transfer-player" : "titles.add-member");
            addKnownPlayers(player, dominion, entries, transfer);
        }

        entries = entries.stream()
                .filter(entry -> matches(route.filter(), entry.name()))
                .sorted(Comparator.comparing(PickerEntry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "picker-list", Map.of("title", title));
        renderPage(view, route, entries, (slot, entry) -> view.itemAt(slot, "content", "content",
                Map.of("name", entry.name(), "description", entry.description()), entry.head(),
                click -> entry.action().run()));
        if (id(route) == MenuId.PLAYER_PICKER
                || id(route) == MenuId.TRANSFER_PICKER
                || id(route) == MenuId.GROUP_MEMBER_PICKER) {
            view.item("search", Map.of(), null, click -> search(player));
        }
        listNavigation(player, view, route, entries.size());
        return view.build();
    }

    private void addTemplates(Player player, MenuRoute route, DominionDTO dominion, List<PickerEntry> entries) {
        for (TemplateDTO template : TemplateProvider.getInstance().getTemplates(player.getUniqueId())) {
            entries.add(new PickerEntry(template.getName(), config.text("labels.template"), null, () -> {
                MemberDTO member = requireMember(dominion, route.integer("member"));
                ui.submit(player, TemplateProvider.getInstance().applyTemplate(player, dominion, member, template),
                        ignored -> nav.back(player));
            }));
        }
    }

    private void addGroups(Player player, MenuRoute route, DominionDTO dominion, List<PickerEntry> entries) {
        MemberDTO member = requireMember(dominion, route.integer("member"));
        for (GroupDTO group : dominion.getGroups()) {
            entries.add(new PickerEntry(group.getNamePlain(), config.text("labels.group"), null,
                    () -> ui.submit(player,
                            GroupProvider.getInstance().addMember(player, dominion, group, member),
                            ignored -> nav.back(player))));
        }
    }

    private void addGroupMembers(Player player, MenuRoute route, DominionDTO dominion,
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
                    member.getPlayerUUID(),
                    () -> ui.submit(player,
                            remove
                                    ? GroupProvider.getInstance().removeMember(player, dominion, group, member)
                                    : GroupProvider.getInstance().addMember(player, dominion, group, member),
                            ignored -> nav.back(player))));
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
            entries.add(new PickerEntry(
                    candidate.getLastKnownName(),
                    config.text("labels.known-player"),
                    candidate.getUuid(),
                    () -> selectKnownPlayer(player, dominion, candidate, transfer)));
        }
    }

    private void selectKnownPlayer(Player player, DominionDTO dominion, PlayerDTO candidate, boolean transfer) {
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

    private record PickerEntry(String name, String description, UUID head, Runnable action) {
    }
}
