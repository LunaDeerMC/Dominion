package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.commands.MemberCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberList;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

public class SearchPlayerInputter {
    public static class SearchPlayerInputterText extends ConfigurationPart {
        public String button = "SEARCH";
        public String hint = "Enter the exact name of the player you want to search.";
    }

    public static void createOn(CommandSender sender, String dominionName) {
        new InputterRunner(sender, Language.searchPlayerInputterText.hint) {
            @Override
            public void run(String input) {
                MemberCommand.addMember(sender, dominionName, input);
                MemberList.show(sender, dominionName, "1");
            }
        };
    }

}
