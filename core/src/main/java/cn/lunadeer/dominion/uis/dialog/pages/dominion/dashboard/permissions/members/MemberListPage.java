package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.members;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.providers.MemberProvider;
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
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Lists the members of one dominion. */
public final class MemberListPage extends AbstractDialogPage {
    private static final DialogListStyle STYLE = DialogListStyle.DEFAULT
            .withCompactItemWidth(104)
            .withCreateItemWidth(94)
            .withCreateButton();

    public MemberListPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        DominionDTO dominion = requireDominion(player, route.integer("dom"));
        List<MemberDTO> members = dominion.getMembers().stream()
                .filter(member -> matches(route.filter(), member.getPlayer().getLastKnownName()))
                .sorted(Comparator.comparing(member -> member.getPlayer().getLastKnownName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<String, Object> titleValues = dominionValues(dominion, false);
        if (DialogListTemplate.isSearchPage(route)) {
            return listSearchPage("member-list", titleValues, route, STYLE);
        }
        DominionDialogPage page = new DominionDialogPage(config,
                "member-list", titleValues, STYLE).keepOpenAfterAction();
        DialogPagination pagination = pagination(route, members.size(), STYLE.pageSize());
        listNavigation(page, route, pagination, members.size(), STYLE);
        DialogListTemplate.item(page,
                DominionDialogPage.component(config.text("menus.member-list.items.primary.name")),
                null, STYLE.createItemWidth(), page.icon("member"),
                (viewer, response) -> nav.push(viewer,
                        route(DialogMenuId.PLAYER_PICKER, dominion)));
        for (MemberDTO member : members.subList(pagination.from(), pagination.to())) {
            Map<String, Object> values = memberValues(dominion, member);
            DialogListTemplate.item(page,
                    DominionDialogPage.component(member.getPlayer().getLastKnownName()),
                    DominionDialogPage.component(configured("descriptions.member-entry", values)),
                    STYLE.compactItemWidth(), page.icon("member"), playerHead(member.getPlayer()),
                    (viewer, response) -> nav.push(viewer,
                            route(DialogMenuId.MEMBER_DETAIL, dominion)
                                    .with("member", member.getId())));
        }
        commonFooter(page);
        return page.buildList();
    }
}
