package cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.flags;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroup;
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
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/** Toggles the individual flags in one flag group. */
public final class FlagListPage extends AbstractFlagPage {
    private static final DialogListStyle STYLE = DialogListStyle.DEFAULT
            .withItemWidth(108)
            .withCompactItemWidth(108);

    public FlagListPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
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
        String draftPrefix = draftPrefix(route, context, group.getId());
        DialogPagination pagination = pagination(route, flags.size(), STYLE.pageSize());
        List<? extends Flag> visible = flags.subList(pagination.from(), pagination.to());
        DominionDialogPage page = new DominionDialogPage(config, "flag-list",
                Map.of("title", title), STYLE).keepOpenAfterAction();
        boolean bulkTarget = FlagGroupActions.bulkTarget(flags,
                flag -> effectiveFlagState(session, draftPrefix, context, route,
                        dominion, template, flag));
        DialogListTemplate.navigation(page, nav, route, pagination, STYLE,
                DominionDialogPage.component(config.text(
                        bulkTarget ? "labels.enable-all" : "labels.disable-all")),
                page.icon(bulkTarget ? "enabled" : "disabled"),
                (viewer, response) -> toggleFlagGroup(viewer, session, draftPrefix, route,
                        context, dominion, template, flags),
                (viewer, response) -> nav.replace(viewer, route.page(pagination.page() - 1)),
                (viewer, response) -> nav.replace(viewer, route.page(pagination.page() + 1)));
        DialogListTemplate.summary(page, config, pagination, flags.size());
        for (Flag flag : visible) {
            boolean state = effectiveFlagState(session, draftPrefix, context, route,
                    dominion, template, flag);
            page.action(flagActionLabel(flag, state),
                    Component.text(flag.getDescription(), net.kyori.adventure.text.format.NamedTextColor.GRAY),
                    STYLE.pageLayout().buttonWidth(),
                    flag.getIcon(),
                    (viewer, response) -> toggleFlag(viewer, session, draftPrefix, route,
                            context, dominion, template, flag));
        }
        page.backExit((viewer, response) -> {
            session.clearState(draftPrefix);
            nav.back(viewer);
        });
        return page.buildList();
    }
}
