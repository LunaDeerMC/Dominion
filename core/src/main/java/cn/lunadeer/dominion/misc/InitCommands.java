package cn.lunadeer.dominion.misc;

import cn.lunadeer.dominion.commands.*;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.holograme.HoloCommand;

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
        // cn.lunadeer.dominion.utils.holograme (only for debug)
        if (Configuration.debug) new HoloCommand();
    }
}
