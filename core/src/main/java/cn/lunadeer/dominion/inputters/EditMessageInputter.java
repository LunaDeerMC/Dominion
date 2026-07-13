package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.events.dominion.modify.DominionSetMessageEvent;
import cn.lunadeer.dominion.uis.dominion.DominionManage;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

import static cn.lunadeer.dominion.commands.DominionOperateCommand.setMessage;

public class EditMessageInputter {
    public static class EditMessageInputterText extends ConfigurationPart {
        public String enterButton = "ENTER MSG";
        public String enterDescription = "Message shown when entering dominion.";
        public String enterHint = "Enter new enter message.";
        public String leaveButton = "LEAVE MSG";
        public String leaveDescription = "Message shown when player leaves dominion.";
        public String leaveHint = "Enter new leave message.";
    }

    public static void createLeaveOn(CommandSender sender, String dominionName) {
        new InputterRunner(sender, Language.editMessageInputterText.leaveHint) {
            @Override
            public void run(String input) {
                setMessage(sender, dominionName, DominionSetMessageEvent.TYPE.LEAVE.name(), input);
                DominionManage.show(sender, dominionName, "1");
            }
        };
    }

    public static void createEnterOn(CommandSender sender, String dominionName) {
        new InputterRunner(sender, Language.editMessageInputterText.enterHint) {
            @Override
            public void run(String input) {
                setMessage(sender, dominionName, DominionSetMessageEvent.TYPE.ENTER.name(), input);
                DominionManage.show(sender, dominionName, "1");
            }
        };
    }


}
