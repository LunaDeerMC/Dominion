package cn.lunadeer.dominion.uis.dialog.pages;

import cn.lunadeer.dominion.uis.dialog.DialogUiText;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.DominionListPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.DashboardPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.area.AreaPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other.AppearancePage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other.CopyTypePage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other.OtherPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other.OwnershipPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.other.ResizePage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.PeoplePage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.PermissionsPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.flags.FlagGroupListPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.flags.FlagListPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.groups.GroupDetailPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.groups.GroupListPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.members.MemberDetailPage;
import cn.lunadeer.dominion.uis.dialog.pages.dominion.dashboard.permissions.members.MemberListPage;
import cn.lunadeer.dominion.uis.dialog.pages.picker.PickerListPage;
import cn.lunadeer.dominion.uis.dialog.pages.root.ConfirmPage;
import cn.lunadeer.dominion.uis.dialog.pages.root.MainPage;
import cn.lunadeer.dominion.uis.dialog.pages.root.TitleListPage;
import cn.lunadeer.dominion.uis.dialog.pages.template.TemplateDetailPage;
import cn.lunadeer.dominion.uis.dialog.pages.template.TemplateListPage;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

/** Routes each business route to the class that renders that page. */
public final class DialogMenuController {
    private final MainPage mainPage;
    private final TitleListPage titleListPage;
    private final ConfirmPage confirmPage;
    private final DominionListPage dominionListPage;
    private final DashboardPage dashboardPage;
    private final AreaPage areaPage;
    private final OtherPage otherPage;
    private final AppearancePage appearancePage;
    private final OwnershipPage ownershipPage;
    private final ResizePage resizePage;
    private final CopyTypePage copyTypePage;
    private final PermissionsPage permissionsPage;
    private final PeoplePage peoplePage;
    private final FlagGroupListPage flagGroupListPage;
    private final FlagListPage flagListPage;
    private final MemberListPage memberListPage;
    private final MemberDetailPage memberDetailPage;
    private final GroupListPage groupListPage;
    private final GroupDetailPage groupDetailPage;
    private final PickerListPage pickerListPage;
    private final TemplateListPage templateListPage;
    private final TemplateDetailPage templateDetailPage;

    public DialogMenuController(DialogMenuUi ui, DialogUiText config, DialogNavigator nav) {
        mainPage = new MainPage(ui, config, nav);
        titleListPage = new TitleListPage(ui, config, nav);
        confirmPage = new ConfirmPage(ui, config, nav);
        dominionListPage = new DominionListPage(ui, config, nav);
        dashboardPage = new DashboardPage(ui, config, nav);
        areaPage = new AreaPage(ui, config, nav);
        otherPage = new OtherPage(ui, config, nav);
        appearancePage = new AppearancePage(ui, config, nav);
        ownershipPage = new OwnershipPage(ui, config, nav);
        resizePage = new ResizePage(ui, config, nav);
        copyTypePage = new CopyTypePage(ui, config, nav);
        permissionsPage = new PermissionsPage(ui, config, nav);
        peoplePage = new PeoplePage(ui, config, nav);
        flagGroupListPage = new FlagGroupListPage(ui, config, nav);
        flagListPage = new FlagListPage(ui, config, nav);
        memberListPage = new MemberListPage(ui, config, nav);
        memberDetailPage = new MemberDetailPage(ui, config, nav);
        groupListPage = new GroupListPage(ui, config, nav);
        groupDetailPage = new GroupDetailPage(ui, config, nav);
        pickerListPage = new PickerListPage(ui, config, nav);
        templateListPage = new TemplateListPage(ui, config, nav);
        templateDetailPage = new TemplateDetailPage(ui, config, nav);
    }

    public DialogSpec render(Player player, DialogMenuSession session) throws Exception {
        return switch (DialogMenuId.valueOf(session.current().id())) {
            case MAIN -> mainPage.render(player, session);
            case TITLE_LIST -> titleListPage.render(player, session);
            case CONFIRM -> confirmPage.render(player, session);

            case DOMINION_LIST, ALL_DOMINIONS, CHILD_LIST, COPY_SOURCE, PLAYER_DOMINIONS ->
                    dominionListPage.render(player, session);
            case DASHBOARD -> dashboardPage.render(player, session);
            case AREA -> areaPage.render(player, session);
            case OTHER -> otherPage.render(player, session);
            case APPEARANCE -> appearancePage.render(player, session);
            case OWNERSHIP -> ownershipPage.render(player, session);
            case RESIZE -> resizePage.render(player, session);
            case COPY_TYPE -> copyTypePage.render(player, session);

            case PERMISSIONS -> permissionsPage.render(player, session);
            case PEOPLE -> peoplePage.render(player, session);
            case ENV_FLAGS, GUEST_FLAGS, MEMBER_FLAGS, GROUP_FLAGS, TEMPLATE_FLAGS ->
                    flagGroupListPage.render(player, session);
            case FLAG_LIST -> flagListPage.render(player, session);
            case MEMBER_LIST -> memberListPage.render(player, session);
            case MEMBER_DETAIL -> memberDetailPage.render(player, session);
            case GROUP_LIST -> groupListPage.render(player, session);
            case GROUP_DETAIL -> groupDetailPage.render(player, session);

            case GROUP_MEMBER_PICKER, PLAYER_PICKER, TRANSFER_PICKER,
                    TEMPLATE_PICKER, ADMIN_PLAYER_DOMINIONS -> pickerListPage.render(player, session);
            case TEMPLATE_LIST -> templateListPage.render(player, session);
            case TEMPLATE_DETAIL -> templateDetailPage.render(player, session);
        };
    }
}
