package cn.lunadeer.dominion.uis.dialog.pages.template;

import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.components.DialogPageLayout;
import cn.lunadeer.dominion.uis.dialog.components.DominionDialogPage;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.uis.dialog.pages.shared.AbstractDialogPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Map;

/** Edits one permission template. */
public final class TemplateDetailPage extends AbstractDialogPage {
    private static final DialogPageLayout LAYOUT = new DialogPageLayout(336, 2, 164, 336);

    public TemplateDetailPage(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        super(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) {
        TemplateDTO template = requireTemplate(player, session.current().integer("template"));
        Map<String, Object> values = Map.of("template", template.getName());
        DominionDialogPage page = new DominionDialogPage(config,
                "template-detail", values, LAYOUT)
                .summary("info", values)
                .textInput("template_name", "input.rename-template", template.getName(), 128)
                .action("save", values, LAYOUT.buttonWidth(), (viewer, response) -> {
                    String name = response.getText("template_name");
                    if (name == null || name.isBlank() || name.equals(template.getName())) {
                        nav.refresh(viewer);
                        return;
                    }
                    ui.submit(viewer, TemplateProvider.getInstance()
                                    .renameTemplate(viewer, template, name.trim()),
                            result -> nav.replace(viewer,
                                    DialogRoute.of(DialogMenuId.TEMPLATE_DETAIL)
                                            .with("template", result.getId())));
                })
                .action("flags", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> nav.push(viewer,
                                DialogRoute.of(DialogMenuId.TEMPLATE_FLAGS)
                                        .with("template", template.getId())))
                .action("delete", values, LAYOUT.buttonWidth(),
                        (viewer, response) -> ui.confirm(viewer,
                                configured("confirm.delete-template",
                                        Map.of("template", template.getName())),
                                confirmed -> ui.submit(viewer,
                                        TemplateProvider.getInstance().deleteTemplate(viewer, template),
                                        ignored -> nav.back(viewer))));
        commonFooter(page);
        return page.build();
    }
}
