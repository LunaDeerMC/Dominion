package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.commands.TemplateCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.uis.template.TemplateList;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

public class CreateTemplateInputter {
    public static class CreateTemplateInputterText extends ConfigurationPart {
        public String button = "CREATE";
        public String hint = "Enter the name of the new template.";
    }

    public static void createOn(CommandSender sender) {
        new InputterRunner(sender, Language.createTemplateInputterText.hint) {
            @Override
            public void run(String input) {
                TemplateCommand.createTemplate(sender, input);
                TemplateList.show(sender, "1");
            }
        };
    }

}
