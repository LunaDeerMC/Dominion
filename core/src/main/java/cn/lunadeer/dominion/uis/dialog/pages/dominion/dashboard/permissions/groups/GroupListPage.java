package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.groups;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogListStyle;
import cn.lunadeer.dominion.uis.dialog.components.DialogListTemplate;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogResponse;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Lists and creates permission groups for one dominion. */
public final class GroupListPage extends AbstractDialogPage {
    private static final DialogListStyle STYLE = DialogListStyle.DEFAULT
            .withCompactItemWidth(104)
            .withCreateItemWidth(94)
            .withCreateButton();

    public GroupListPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        List<GroupDTO> groups = dominion.getGroups().stream()
                .filter(group -> matches(route.filter(), group.getNamePlain()))
                .sorted(Comparator.comparing(GroupDTO::getNamePlain, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<String, Object> titleValues = dominionValues(dominion, false);
        if (DialogListTemplate.isSearchPage(route)) {
            return listSearchPage("group-list", titleValues, route, STYLE);
        }
        DominionDialogPage page = new DominionDialogPage(
                config, "group-list", titleValues, STYLE).keepOpenAfterAction();
        DialogPagination pagination = pagination(route, groups.size(), STYLE.pageSize());
        listNavigation(page, route, pagination, groups.size(), STYLE);
        DialogListTemplate.item(page,
                DominionDialogPage.component(config.text("menus.group-list.items.primary.name")),
                null, STYLE.createItemWidth(), page.icon("group"), this::createGroup);
        for (GroupDTO group : groups.subList(pagination.from(), pagination.to())) {
            int members;
            try {
                members = group.getMembers().size();
            } catch (Exception exception) {
                members = 0;
            }
            Map<String, Object> values = Map.of("group", group.getNamePlain(), "members", members);
            DialogListTemplate.item(page, DominionDialogPage.component(group.getNamePlain()),
                    DominionDialogPage.component(configured("descriptions.group-entry", values)),
                    STYLE.compactItemWidth(), page.icon("group"),
                    (viewer, response) -> nav.push(viewer,
                            route(DialogMenuId.GROUP_DETAIL, dominion).with("group", group.getId())));
        }
        commonFooter(page);
        return page.buildList();
    }

    private void createGroup(Player player, DialogResponse response) {
        DominionDTO dominion = currentDominion(player, ui.session(player));
        ui.requestInput(player, config.text("input.create-group"), name -> {
            if (name == null || name.isBlank()) return;
            ui.submit(player, GroupProvider.getInstance().createGroup(player, dominion, name.trim()),
                    ignored -> nav.refresh(player));
        });
    }
}
