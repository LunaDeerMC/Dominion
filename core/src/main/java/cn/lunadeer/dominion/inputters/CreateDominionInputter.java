package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.commands.DominionCreateCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.uis.dominion.DominionList;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

public class CreateDominionInputter {

    public static class CreateDominionInputterText extends ConfigurationPart {
        public String button = "CREATE";
        public String description = "Create a new dominion.";
        public String hint = "A new Dominion will be created around you with the input name.";
    }

    public static void createOn(CommandSender sender) {
        new InputterRunner(sender, Language.createDominionInputterText.hint) {
            @Override
            public void run(String input) {
                DominionCreateCommand.autoCreate(sender, input);
                DominionList.show(sender, "1");
            }
        };
    }

}
