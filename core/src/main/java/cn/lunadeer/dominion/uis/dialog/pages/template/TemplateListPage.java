package cn.lunadeer.dominion.uis.dialog.pages.template;

import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.providers.TemplateProvider;
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

/** Lists and creates reusable permission templates. */
public final class TemplateListPage extends AbstractDialogPage {
    private static final DialogListStyle STYLE = DialogListStyle.DEFAULT
            .withCompactItemWidth(104)
            .withCreateItemWidth(94)
            .withCreateButton();

    public TemplateListPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        List<TemplateDTO> templates = TemplateProvider.getInstance()
                .getTemplates(player.getUniqueId()).stream()
                .filter(template -> matches(route.filter(), template.getName()))
                .sorted(Comparator.comparing(TemplateDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (DialogListTemplate.isSearchPage(route)) {
            return listSearchPage("template-list", Map.of(), route, STYLE);
        }
        DominionDialogPage page = new DominionDialogPage(config, "template-list", Map.of(), STYLE)
                .keepOpenAfterAction();
        DialogPagination pagination = pagination(route, templates.size(), STYLE.pageSize());
        listNavigation(page, route, pagination, templates.size(), STYLE);
        DialogListTemplate.item(page,
                DominionDialogPage.component(config.text("menus.template-list.items.primary.name")),
                null, STYLE.createItemWidth(), page.icon("template"),
                (viewer, response) -> ui.requestInput(viewer,
                        config.text("input.create-template"), name -> {
                            if (name == null || name.isBlank()) return;
                            ui.submit(viewer, TemplateProvider.getInstance()
                                            .createTemplate(viewer, name.trim()),
                                    ignored -> nav.refresh(viewer));
                        }));
        for (TemplateDTO template : templates.subList(pagination.from(), pagination.to())) {
            DialogListTemplate.item(page, DominionDialogPage.component(template.getName()),
                    DominionDialogPage.component(config.text("descriptions.open-template")),
                    STYLE.compactItemWidth(), page.icon("template"),
                    (viewer, response) -> nav.push(viewer,
                            DialogRoute.of(DialogMenuId.TEMPLATE_DETAIL)
                                    .with("template", template.getId())));
        }
        commonFooter(page);
        return page.buildList();
    }
}
