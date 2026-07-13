package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.commands.TemplateCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

public class RenameTemplateInputter {
    public static class RenameTemplateInputterText extends ConfigurationPart {
        public String button = "RENAME";
        public String hint = "Enter new template name.";
        public String description = "Rename this template.";
    }

    public static void createOn(CommandSender sender, String oldTemplateName, String pageStr) {
        new InputterRunner(sender, Language.renameTemplateInputterText.hint) {
            @Override
            public void run(String input) {
                TemplateCommand.renameTemplate(sender, oldTemplateName, input, pageStr);
            }
        };
    }

}
