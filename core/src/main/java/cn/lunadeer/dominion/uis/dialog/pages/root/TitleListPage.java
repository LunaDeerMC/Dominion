package cn.lunadeer.dominion.uis.dialog.pages.root;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.providers.PlayerProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogListStyle;
import cn.lunadeer.dominion.uis.dialog.components.DialogListTemplate;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
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

/** Selects the active title from the player's available permission groups. */
public final class TitleListPage extends AbstractDialogPage {
    private static final DialogListStyle STYLE = DialogListStyle.DEFAULT
            .withSpecialItemWidth(120);

    public TitleListPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        List<GroupDTO> titles = PlayerProvider.getInstance()
                .getAvailableGroupTitles(player.getUniqueId()).stream()
                .filter(group -> matches(route.filter(), group.getNamePlain()))
                .sorted(Comparator.comparing(GroupDTO::getNamePlain, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (DialogListTemplate.isSearchPage(route)) {
            return listSearchPage("title-list", Map.of(), route, STYLE);
        }
        DominionDialogPage page = new DominionDialogPage(config, "title-list", Map.of(), STYLE)
                .messageKey("descriptions.title-list", Map.of())
                .keepOpenAfterAction();
        DialogPagination pagination = pagination(route, titles.size(), STYLE.pageSize());
        listNavigation(page, route, pagination, titles.size(), STYLE);
        DialogListTemplate.item(page,
                DominionDialogPage.component(config.text("menus.title-list.items.disable.name")),
                null, STYLE.specialItemWidth(), page.icon("disable"),
                (viewer, response) -> ui.submit(viewer,
                        PlayerProvider.getInstance().setGroupTitle(viewer, null),
                        ignored -> nav.refresh(viewer)));
        for (GroupDTO group : titles.subList(pagination.from(), pagination.to())) {
            DominionDTO dominion = api.getDominion(group.getDomID());
            String label = configured("labels.title-option", Map.of(
                    "group", group.getNamePlain(),
                    "dominion", dominion == null ? config.text("labels.unknown") : dominion.getName()));
            DialogListTemplate.item(page, DominionDialogPage.component(label),
                    DominionDialogPage.component(config.text("input.select-title")),
                    STYLE.itemWidth(), page.icon("title"),
                    (viewer, response) -> ui.submit(viewer,
                            PlayerProvider.getInstance().setGroupTitle(viewer, group),
                            ignored -> nav.refresh(viewer)));
        }
        commonFooter(page);
        return page.buildList();
    }
}
