package cn.lunadeer.dominion.uis.dialog.components;

import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogPagination;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;

/** Shared behavior for list pages; individual pages own their style object. */
public final class DialogListTemplate {
    public static final String SEARCH_PAGE_PARAMETER = "__dominion_list_search";

    private DialogListTemplate() {
    }

    public static boolean isSearchPage(DialogRoute route) {
        return "true".equals(route.string(SEARCH_PAGE_PARAMETER));
    }

    public static DialogRoute openSearch(DialogRoute route) {
        return route.with(SEARCH_PAGE_PARAMETER, true);
    }

    public static DialogRoute closeSearch(DialogRoute route) {
        return route.without(SEARCH_PAGE_PARAMETER);
    }

    /** Adds one entry while leaving its width and icon to the owning page. */
    public static void item(DominionDialogPage page, Component name, Component tooltip,
                            int width, String icon, DialogSpec.Callback action) {
        page.listAction(name, tooltip, width, icon, action);
    }

    /** Adds a player entry with a native head and a configured atlas fallback. */
    public static void item(DominionDialogPage page, Component name, Component tooltip,
                            int width, String fallbackIcon,
                            DialogSpec.PlayerHeadIcon playerHead, DialogSpec.Callback action) {
        page.listAction(name, tooltip, width, fallbackIcon, playerHead, action);
    }

    public static void navigation(DominionDialogPage page,
                                  DialogNavigator nav,
                                  DialogRoute route,
                                  DialogPagination pagination,
                                  DialogListStyle style,
                                  Component searchLabel,
                                  DialogSpec.Callback searchAction,
                                  DialogSpec.Callback previousAction,
                                  DialogSpec.Callback nextAction) {
        navigation(page, nav, route, pagination, style, searchLabel, page.icon("search"),
                searchAction, previousAction, nextAction);
    }

    public static void navigation(DominionDialogPage page,
                                  DialogNavigator nav,
                                  DialogRoute route,
                                  DialogPagination pagination,
                                  DialogListStyle style,
                                  Component searchLabel,
                                  String searchIcon,
                                  DialogSpec.Callback searchAction,
                                  DialogSpec.Callback previousAction,
                                  DialogSpec.Callback nextAction) {
        boolean hasPrevious = pagination.page() > 1;
        boolean hasNext = pagination.page() < pagination.pages();

        DialogSpec.Callback previous = previousAction == null
                ? (viewer, response) -> nav.replace(viewer, route.page(pagination.page() - 1))
                : previousAction;
        DialogSpec.Callback next = nextAction == null
                ? (viewer, response) -> nav.replace(viewer, route.page(pagination.page() + 1))
                : nextAction;

                page.action(Component.text("‹", hasPrevious ? NamedTextColor.WHITE : NamedTextColor.GRAY),
                        null, style.navigationButtonWidth(), page.icon("previous"),
                        hasPrevious ? previous : null)
                .action(searchLabel.colorIfAbsent(NamedTextColor.WHITE), null,
                        style.searchButtonWidth(), searchIcon, searchAction)
                .action(Component.text("›", hasNext ? NamedTextColor.WHITE : NamedTextColor.GRAY),
                        null, style.navigationButtonWidth(), page.icon("next"),
                        hasNext ? next : null);
    }

    public static void summary(DominionDialogPage page, DialogUiText text,
                               DialogPagination pagination, int total) {
        page.message(DialogTextRenderer.replaceNamed(text.text("common.page"), Map.of(
                "page", pagination.page(), "pages", pagination.pages(), "total", total)));
    }

    public static DialogSpec searchPage(DialogUiText text,
                                        String menuId,
                                        Map<String, ?> titleValues,
                                        DialogRoute route,
                                        DialogNavigator nav,
                                        DialogListStyle style) {
        DialogPageLayout searchLayout = style.searchLayout();
        DominionDialogPage page = new DominionDialogPage(text, menuId, titleValues,
                searchLayout)
                .textInput("search", "input.search", route.filter(), 128)
                .action("back", Map.of(), searchLayout.buttonWidth(),
                        (viewer, response) -> nav.back(viewer))
                .action("search", Map.of(), searchLayout.buttonWidth(),
                        (viewer, response) -> {
                            String filter = response.getText("search");
                            nav.replace(viewer, closeSearch(route).filter(
                                    filter == null ? "" : filter.trim()));
                        });
        return page.build(searchLayout.columns(), null);
    }
}
