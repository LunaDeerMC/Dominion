package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.flags;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroup;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogListStyle;
import cn.lunadeer.dominion.uis.dialog.components.DialogListTemplate;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Lists flag groups for environment, guest, member, group, or template flags. */
public final class FlagGroupListPage extends AbstractFlagPage {
    private static final DialogListStyle STYLE = DialogListStyle.DEFAULT
            .withCompactItemWidth(104);

    public FlagGroupListPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
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
                .filter(group -> matches(route.filter(), group.getId().equals("ungrouped")
                        ? config.text("labels.ungrouped") : group.getDisplayName()))
                .toList();
        String title = flagTitle(context, dominion, template);
        if (DialogListTemplate.isSearchPage(route)) {
            return listSearchPage("flag-group-list", Map.of("title", title), route, STYLE);
        }
        DominionDialogPage page = new DominionDialogPage(
                config, "flag-group-list", Map.of("title", title), STYLE)
                .keepOpenAfterAction();
        DialogPagination pagination = pagination(route, groups.size(), STYLE.pageSize());
        listNavigation(page, route, pagination, groups.size(), STYLE);
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
                    "enabled", enabled, "total", flags.size());
            DialogListTemplate.item(page,
                    DominionDialogPage.component(String.valueOf(values.get("group"))),
                    DominionDialogPage.component(configured("descriptions.flag-group", values)),
                    STYLE.compactItemWidth(), group.getIcon(),
                    (viewer, response) -> nav.push(viewer,
                            groupRoute(route, context, group.getId())));
        }
        commonFooter(page);
        return page.buildList();
    }
}
