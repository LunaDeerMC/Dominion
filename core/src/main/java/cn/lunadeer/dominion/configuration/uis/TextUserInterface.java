package cn.lunadeer.dominion.configuration.uis;

import cn.lunadeer.dominion.uis.AllDominion;
import cn.lunadeer.dominion.uis.MainMenu;
import cn.lunadeer.dominion.uis.MigrateList;
import cn.lunadeer.dominion.uis.TitleList;
import cn.lunadeer.dominion.uis.dominion.DominionList;
import cn.lunadeer.dominion.uis.dominion.DominionManage;
import cn.lunadeer.dominion.uis.dominion.copy.CopyMenu;
import cn.lunadeer.dominion.uis.dominion.copy.DominionCopy;
import cn.lunadeer.dominion.uis.dominion.manage.EnvFlags;
import cn.lunadeer.dominion.uis.dominion.manage.GuestFlags;
import cn.lunadeer.dominion.uis.dominion.manage.Info;
import cn.lunadeer.dominion.uis.dominion.manage.MonsterSpawnSettings;
import cn.lunadeer.dominion.uis.dominion.manage.SetSize;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupFlags;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupList;
import cn.lunadeer.dominion.uis.dominion.manage.group.SelectMember;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberFlags;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberList;
import cn.lunadeer.dominion.uis.dominion.manage.member.SelectPlayer;
import cn.lunadeer.dominion.uis.dominion.manage.member.SelectTemplate;
import cn.lunadeer.dominion.uis.template.TemplateFlags;
import cn.lunadeer.dominion.uis.template.TemplateList;
import cn.lunadeer.dominion.utils.configuration.ConfigurationFile;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.configuration.Headers;
import cn.lunadeer.dominion.utils.configuration.PostProcess;
import cn.lunadeer.dominion.utils.stui.ViewStyles;
import net.kyori.adventure.text.format.TextColor;

@Headers({
        "This file is used to configure the text user interface (TUI) of the Dominion plugin.",
        "You can customize the colors and text displayed in the TUI.",
        "Make sure to reload the configuration after making changes."
})
public class TextUserInterface extends ConfigurationFile {

    public static class Style extends ConfigurationPart {
        public String primaryColor = "#0094D5";
        public String secondaryColor = "#7A7A7A";
        public String actionColor = "#FBFF8B";
        public String severeColor = "#FF6048";
        public String normalColor = "#8BFF7B";
    }

    public static Style style = new Style();

    public static MainMenu.MenuTuiText menuTuiText = new MainMenu.MenuTuiText();
    public static DominionList.DominionListTuiText dominionListTuiText = new DominionList.DominionListTuiText();
    public static DominionManage.DominionManageTuiText dominionManageTuiText = new DominionManage.DominionManageTuiText();
    public static SetSize.SetSizeTuiText setSizeTuiText = new SetSize.SetSizeTuiText();
    public static EnvFlags.EnvSettingTuiText envSettingTuiText = new EnvFlags.EnvSettingTuiText();
    public static GuestFlags.GuestSettingTuiText guestSettingTuiText = new GuestFlags.GuestSettingTuiText();
    public static MonsterSpawnSettings.MonsterSpawnSettingsTuiText monsterSpawnSettingsTuiText = new MonsterSpawnSettings.MonsterSpawnSettingsTuiText();
    public static Info.SizeInfoTuiText sizeInfoTuiText = new Info.SizeInfoTuiText();
    public static MigrateList.MigrateListTuiText migrateListTuiText = new MigrateList.MigrateListTuiText();
    public static MemberList.MemberListTuiText memberListTuiText = new MemberList.MemberListTuiText();
    public static SelectPlayer.SelectPlayerTuiText selectPlayerTuiText = new SelectPlayer.SelectPlayerTuiText();
    public static MemberFlags.MemberSettingTuiText memberSettingTuiText = new MemberFlags.MemberSettingTuiText();
    public static TemplateList.TemplateListTuiText templateListTuiText = new TemplateList.TemplateListTuiText();
    public static SelectTemplate.SelectTemplateTuiText selectTemplateTuiText = new SelectTemplate.SelectTemplateTuiText();
    public static GroupList.GroupListTuiText groupListTuiText = new GroupList.GroupListTuiText();
    public static GroupFlags.GroupSettingTuiText groupSettingTuiText = new GroupFlags.GroupSettingTuiText();
    public static SelectMember.SelectMemberTuiText selectMemberTuiText = new SelectMember.SelectMemberTuiText();
    public static TitleList.TitleListTuiText titleListTuiText = new TitleList.TitleListTuiText();
    public static AllDominion.AllDominionTuiText allDominionTuiText = new AllDominion.AllDominionTuiText();
    public static TemplateFlags.TemplateSettingTuiText templateSettingTuiText = new TemplateFlags.TemplateSettingTuiText();
    public static CopyMenu.CopyMenuTuiText copyMenuTuiText = new CopyMenu.CopyMenuTuiText();
    public static DominionCopy.DominionCopyTuiText dominionCopyTuiText = new DominionCopy.DominionCopyTuiText();

    @PostProcess
    public void postProcess() {
        ViewStyles.ACTION = TextColor.fromHexString(style.actionColor);
        ViewStyles.PRIMARY = TextColor.fromHexString(style.primaryColor);
        ViewStyles.SECONDARY = TextColor.fromHexString(style.secondaryColor);
        ViewStyles.SEVERE = TextColor.fromHexString(style.severeColor);
        ViewStyles.NORMAL = TextColor.fromHexString(style.normalColor);
    }
}
