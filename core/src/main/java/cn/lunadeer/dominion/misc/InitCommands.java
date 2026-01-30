package cn.lunadeer.dominion.misc;

import cn.lunadeer.dominion.commands.*;
import cn.lunadeer.dominion.uis.*;
import cn.lunadeer.dominion.uis.dominion.DominionList;
import cn.lunadeer.dominion.uis.dominion.DominionManage;
import cn.lunadeer.dominion.uis.dominion.copy.CopyMenu;
import cn.lunadeer.dominion.uis.dominion.copy.DominionCopy;
import cn.lunadeer.dominion.uis.dominion.manage.EnvFlags;
import cn.lunadeer.dominion.uis.dominion.manage.GuestFlags;
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

public class InitCommands {
    public InitCommands() {
        // cn.lunadeer.dominion.commands
        new AdministratorCommand();
        new DominionCreateCommand();
        new DominionFlagCommand();
        new DominionOperateCommand();
        new GroupCommand();
        new GroupTitleCommand();
        new MemberCommand();
        new MigrationCommand();
        new TemplateCommand();
        new CopyCommand();
        // cn.lunadeer.dominion.uis
        new MainMenu();
        new AllDominion();
        new AllDominionOfPlayer();
        new MigrateList();
        new TitleList();
        // cn.lunadeer.dominion.uis.dominion
        new DominionManage();
        new DominionList();
        // cn.lunadeer.dominion.uis.dominion.manage
        new EnvFlags();
        new GuestFlags();
        new SetSize();
        // cn.lunadeer.dominion.uis.dominion.manage.group
        new GroupManage();
        new GroupFlags();
        new SelectMember();
        new GroupList();
        // cn.lunadeer.dominion.uis.dominion.manage.member
        new MemberFlags();
        new MemberList();
        new SelectPlayer();
        new SelectTemplate();
        // cn.lunadeer.dominion.uis.dominion.copy
        new CopyMenu();
        new DominionCopy();
        // cn.lunadeer.dominion.uis.template
        new TemplateList();
        new TemplateFlags();

    }
}
