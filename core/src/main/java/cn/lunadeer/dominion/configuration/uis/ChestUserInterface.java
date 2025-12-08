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
import cn.lunadeer.dominion.uis.dominion.manage.MonsterSpawnSettings;
import cn.lunadeer.dominion.uis.dominion.manage.SetSize;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupFlags;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupList;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupManage;
import cn.lunadeer.dominion.uis.dominion.manage.group.SelectMember;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberFlags;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberList;
import cn.lunadeer.dominion.uis.dominion.manage.member.SelectPlayer;
import cn.lunadeer.dominion.uis.dominion.manage.member.SelectTemplate;
import cn.lunadeer.dominion.uis.template.TemplateFlags;
import cn.lunadeer.dominion.uis.template.TemplateList;
import cn.lunadeer.dominion.utils.configuration.ConfigurationFile;
import cn.lunadeer.dominion.utils.configuration.Headers;

@Headers({
        "Brief Description:",
        "    This file defines the user interface for the chest GUI in Dominion.",
        "    Both name and lore support PlaceholderAPI and color codes.",
        "",
        "For list view layout:",
        "    You can change the number or position of the item symbol to control how",
        "    many and where items are displayed.",
        "",
        "For button material:",
        "    Use the material name in uppercase, e.g., STONE, DIAMOND_SWORD.",
        "    If you want to use player heads with textures, we support three formats:",
        "        1. PLAYER_HEAD;B64;{base64 texture value}",
        "        2. PLAYER_HEAD;URL;{texture image URL}",
        "        3. PLAYER_HEAD;NAME;{player name}",
        "    The first format uses a base64-encoded texture value, while the second",
        "    uses a URL to an image. The URL must point to a Mojang server skin image.",
        "    You can search textures on e.g. https://mcheads.ru/ to get the base64 ",
        "    texture value or the URL you need.",
})
public class ChestUserInterface extends ConfigurationFile {
    public static MainMenu.MainMenuCui mainMenuCui = new MainMenu.MainMenuCui();
    public static AllDominion.AllDominionCui allDominionCui = new AllDominion.AllDominionCui();
    public static TitleList.TitleListCui titleListCui = new TitleList.TitleListCui();
    public static MigrateList.MigrateListCui migrateListCui = new MigrateList.MigrateListCui();
    public static TemplateList.TemplateListCui templateListCui = new TemplateList.TemplateListCui();
    public static TemplateFlags.TemplateSettingCui templateSettingCui = new TemplateFlags.TemplateSettingCui();
    public static DominionList.DominionListCui dominionListCui = new DominionList.DominionListCui();
    public static DominionManage.DominionManageCui dominionManageCui = new DominionManage.DominionManageCui();
    public static CopyMenu.CopyMenuCui copyMenuCui = new CopyMenu.CopyMenuCui();
    public static DominionCopy.DominionCopyCui dominionCopyCui = new DominionCopy.DominionCopyCui();
    public static EnvFlags.EnvSettingCui envSettingCui = new EnvFlags.EnvSettingCui();
    public static GuestFlags.GuestSettingCui guestSettingCui = new GuestFlags.GuestSettingCui();
    public static MonsterSpawnSettings.MonsterSpawnSettingsCui monsterSpawnSettingsCui = new MonsterSpawnSettings.MonsterSpawnSettingsCui();
    public static SetSize.SetSizeCui setSizeCui = new SetSize.SetSizeCui();
    public static GroupList.GroupListCui groupListCui = new GroupList.GroupListCui();
    public static GroupManage.GroupManageCUI groupManageCUI = new GroupManage.GroupManageCUI();
    public static GroupFlags.GroupSettingCui groupSettingCui = new GroupFlags.GroupSettingCui();
    public static SelectMember.SelectMemberCui selectMemberCui = new SelectMember.SelectMemberCui();
    public static MemberList.MemberListCui memberListCui = new MemberList.MemberListCui();
    public static MemberFlags.MemberSettingCui memberSettingCui = new MemberFlags.MemberSettingCui();
    public static SelectPlayer.SelectPlayerCui selectPlayerCui = new SelectPlayer.SelectPlayerCui();
    public static SelectTemplate.SelectTemplateCui selectTemplateCui = new SelectTemplate.SelectTemplateCui();
}
