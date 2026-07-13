package cn.lunadeer.dominion.uis.chest;

import cn.lunadeer.dominion.utils.chestui.ChestUiManager;
import cn.lunadeer.dominion.utils.chestui.MenuController;
import cn.lunadeer.dominion.utils.chestui.MenuNavigator;
import cn.lunadeer.dominion.utils.chestui.MenuSession;
import cn.lunadeer.dominion.utils.chestui.MenuView;
import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import org.bukkit.entity.Player;

/** Routes each built-in menu to the controller responsible for its feature family. */
final class BuiltinMenuController implements MenuController {
    private final BuiltinRootMenu rootMenus;
    private final BuiltinDominionMenu dominionMenus;
    private final BuiltinPermissionMenu permissionMenus;
    private final BuiltinPickerMenu pickerMenus;
    private final BuiltinTemplateMenu templateMenus;

    BuiltinMenuController(ChestUiManager ui, ChestUiConfig config, MenuNavigator nav) {
        rootMenus = new BuiltinRootMenu(ui, config, nav);
        dominionMenus = new BuiltinDominionMenu(ui, config, nav);
        permissionMenus = new BuiltinPermissionMenu(ui, config, nav);
        pickerMenus = new BuiltinPickerMenu(ui, config, nav);
        templateMenus = new BuiltinTemplateMenu(ui, config, nav);
    }

    @Override
    public MenuView render(Player player, MenuSession session) throws Exception {
        return switch (MenuId.valueOf(session.current().id())) {
            case MAIN -> rootMenus.main(player, session);
            case TITLE_LIST -> rootMenus.titleList(player, session);
            case CONFIRM -> rootMenus.confirm(player, session);

            case DOMINION_LIST, ALL_DOMINIONS, CHILD_LIST, COPY_SOURCE ->
                    dominionMenus.dominionList(player, session);
            case DASHBOARD -> dominionMenus.dashboard(player, session);
            case AREA -> dominionMenus.area(player, session);
            case APPEARANCE -> dominionMenus.appearance(player, session);
            case OWNERSHIP -> dominionMenus.ownership(player, session);
            case RESIZE -> dominionMenus.resize(player, session);
            case COPY_TYPE -> dominionMenus.copyType(player, session);

            case PERMISSIONS -> permissionMenus.permissions(player, session);
            case PEOPLE -> permissionMenus.people(player, session);
            case ENV_FLAGS, GUEST_FLAGS, MEMBER_FLAGS, GROUP_FLAGS, TEMPLATE_FLAGS ->
                    permissionMenus.flagList(player, session);
            case MEMBER_LIST -> permissionMenus.memberList(player, session);
            case MEMBER_DETAIL -> permissionMenus.memberDetail(player, session);
            case GROUP_LIST -> permissionMenus.groupList(player, session);
            case GROUP_DETAIL -> permissionMenus.groupDetail(player, session);

            case GROUP_MEMBER_PICKER, PLAYER_PICKER, TRANSFER_PICKER, TEMPLATE_PICKER ->
                    pickerMenus.picker(player, session);

            case TEMPLATE_LIST -> templateMenus.templateList(player, session);
            case TEMPLATE_DETAIL -> templateMenus.templateDetail(player, session);
        };
    }
}
