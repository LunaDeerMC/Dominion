package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import org.bukkit.entity.Player;

/** Routes each built-in menu to the controller responsible for its feature family. */
public final class DialogMenuController {
    private final DialogRootMenu rootMenus;
    private final DialogDominionMenu dominionMenus;
    private final DialogPermissionMenu permissionMenus;
    private final DialogPickerMenu pickerMenus;
    private final DialogTemplateMenu templateMenus;

    public DialogMenuController(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        rootMenus = new DialogRootMenu(ui, config, nav);
        dominionMenus = new DialogDominionMenu(ui, config, nav);
        permissionMenus = new DialogPermissionMenu(ui, config, nav);
        pickerMenus = new DialogPickerMenu(ui, config, nav);
        templateMenus = new DialogTemplateMenu(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) throws Exception {
        return switch (DialogMenuId.valueOf(session.current().id())) {
            case MAIN -> rootMenus.main(player, session);
            case TITLE_LIST -> rootMenus.titleList(player, session);
            case CONFIRM -> rootMenus.confirm(player, session);

            case DOMINION_LIST, ALL_DOMINIONS, CHILD_LIST, COPY_SOURCE,
                    PLAYER_DOMINIONS ->
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
                    permissionMenus.flagGroupList(player, session);
            case FLAG_LIST ->
                    permissionMenus.flagList(player, session);
            case MEMBER_LIST -> permissionMenus.memberList(player, session);
            case MEMBER_DETAIL -> permissionMenus.memberDetail(player, session);
            case GROUP_LIST -> permissionMenus.groupList(player, session);
            case GROUP_DETAIL -> permissionMenus.groupDetail(player, session);

            case GROUP_MEMBER_PICKER, PLAYER_PICKER, TRANSFER_PICKER, TEMPLATE_PICKER,
                    ADMIN_PLAYER_DOMINIONS ->
                    pickerMenus.picker(player, session);

            case TEMPLATE_LIST -> templateMenus.templateList(player, session);
            case TEMPLATE_DETAIL -> templateMenus.templateDetail(player, session);
        };
    }
}
