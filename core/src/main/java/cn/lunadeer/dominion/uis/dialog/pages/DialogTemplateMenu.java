package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Native template list and detail dialogs. */
final class DialogTemplateMenu extends AbstractDialogMenu {
    DialogTemplateMenu(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    DialogSpec templateList(Player player, DialogMenuSession session) {
        DialogRoute route = session.current();
        List<TemplateDTO> templates = TemplateProvider.getInstance().getTemplates(player.getUniqueId()).stream()
                .sorted(Comparator.comparing(TemplateDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        DominionDialogPage page = new DominionDialogPage(config, "template-list", Map.of())
                .textInput("template_name", "input.create-template", "", 128)
                .keepOpenAfterAction()
                .action("primary", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH, (viewer, response) -> {
                    String name = response.getText("template_name");
                    if (name == null || name.isBlank()) return;
                    ui.submit(viewer,
                            TemplateProvider.getInstance().createTemplate(viewer, name.trim()),
                            ignored -> {});
                })
                .action("refresh", Map.of(), DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.refresh(viewer));
        DialogPagination pagination = pagination(route, templates.size(), DominionDialogPage.LIST_PAGE_SIZE);
        for (TemplateDTO template : templates.subList(pagination.from(), pagination.to())) {
            Component tooltip = DominionDialogPage.component(
                    config.text("descriptions.open-template"));
            page.listAction(
                    DominionDialogPage.component(template.getName()),
                    tooltip,
                    (viewer, response) -> nav.push(viewer,
                            DialogRoute.of(DialogMenuId.TEMPLATE_DETAIL)
                                    .with("template", template.getId())));
        }
        listNavigation(player, page, route, pagination, templates.size());
        commonFooter(page);
        return page.buildList();
    }

    DialogSpec templateDetail(Player player, DialogMenuSession session) {
        TemplateDTO template = requireTemplate(player, session.current().integer("template"));
        Map<String, Object> values = Map.of("template", template.getName());
        DominionDialogPage page = new DominionDialogPage(config, "template-detail", values)
                .summary("info", values, Material.WRITABLE_BOOK)
                .textInput("template_name", "input.rename-template", template.getName(), 128)
                .action("save", values, DominionDialogPage.TWO_COLUMN_WIDTH, (viewer, response) -> {
                    String name = response.getText("template_name");
                    if (name == null || name.isBlank() || name.equals(template.getName())) {
                        nav.refresh(viewer);
                        return;
                    }
                    ui.submit(viewer,
                            TemplateProvider.getInstance().renameTemplate(viewer, template, name.trim()),
                            result -> nav.replace(viewer,
                                    DialogRoute.of(DialogMenuId.TEMPLATE_DETAIL)
                                            .with("template", result.getId())));
                })
                .action("flags", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> nav.push(viewer,
                                DialogRoute.of(DialogMenuId.TEMPLATE_FLAGS)
                                        .with("template", template.getId())))
                .action("delete", values, DominionDialogPage.TWO_COLUMN_WIDTH,
                        (viewer, response) -> ui.confirm(viewer,
                                configured("confirm.delete-template",
                                        Map.of("template", template.getName())),
                                confirmed -> ui.submit(viewer,
                                        TemplateProvider.getInstance().deleteTemplate(
                                                viewer, template),
                                        ignored -> nav.back(viewer))));
        commonFooter(page);
        return page.build(2, null);
    }
}
