package cn.lunadeer.dominion.uis.chest;

import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.utils.chestui.ChestUiManager;
import cn.lunadeer.dominion.utils.chestui.MenuNavigator;
import cn.lunadeer.dominion.utils.chestui.MenuRoute;
import cn.lunadeer.dominion.utils.chestui.MenuSession;
import cn.lunadeer.dominion.utils.chestui.MenuView;
import cn.lunadeer.dominion.utils.chestui.MenuViewBuilder;
import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Renders template discovery and template management menus. */
final class BuiltinTemplateMenu extends AbstractBuiltinMenu {
    BuiltinTemplateMenu(ChestUiManager ui, ChestUiConfig config, MenuNavigator nav) {
        super(ui, config, nav);
    }

    MenuView templateList(Player player, MenuSession session) {
        MenuRoute route = session.current();
        List<TemplateDTO> templates = TemplateProvider.getInstance().getTemplates(player.getUniqueId()).stream()
                .sorted(Comparator.comparing(TemplateDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "template-list", Map.of());
        renderPage(view, route, templates, (slot, template) -> view.itemAt(slot, "content", "content",
                Map.of("template", template.getName()), null, click -> nav.push(player,
                        MenuRoute.of(MenuId.TEMPLATE_DETAIL).with("template", template.getId()))));
        view.item("primary", Map.of(), null, click -> ui.requestInput(player, config.text("input.create-template"),
                name -> ui.submit(player, TemplateProvider.getInstance().createTemplate(player, name), ignored -> {})));
        listNavigation(player, view, route, templates.size());
        return view.build();
    }

    MenuView templateDetail(Player player, MenuSession session) {
        TemplateDTO template = requireTemplate(player, session.current().integer("template"));
        Map<String, Object> values = Map.of("template", template.getName());
        MenuViewBuilder view = new MenuViewBuilder(player, session, config, "template-detail", values);
        view.item("info", values, null, null);
        view.item("rename", values, null, click -> ui.requestInput(player, config.text("input.rename-template"),
                name -> ui.submit(player, TemplateProvider.getInstance().renameTemplate(player, template, name),
                        result -> nav.replace(player, MenuRoute.of(MenuId.TEMPLATE_DETAIL)
                                .with("template", result.getId())))));
        view.item("flags", values, null, click -> nav.push(player,
                MenuRoute.of(MenuId.TEMPLATE_FLAGS).with("template", template.getId())));
        view.item("delete", values, null, click -> ui.confirm(player,
                configured("confirm.delete-template", Map.of("template", template.getName())), confirmed ->
                        ui.submit(player, TemplateProvider.getInstance().deleteTemplate(player, template),
                                ignored -> nav.back(player))));
        commonFooter(player, view);
        return view.build();
    }
}
