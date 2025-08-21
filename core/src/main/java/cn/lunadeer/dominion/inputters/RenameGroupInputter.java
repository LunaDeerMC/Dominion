package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupFlags;
import cn.lunadeer.dominion.utils.ColorParser;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;
import static cn.lunadeer.dominion.misc.Converts.toGroupDTO;

public class RenameGroupInputter {

    public static class RenameGroupInputterText extends ConfigurationPart {
        public String button = "RENAME";
        public String hint = "Enter new group name.";
        public String description = "Rename this group.";
    }

    public static void createOn(CommandSender sender, String dominionName, String oldGroupName) {
        new InputterRunner(sender, Language.renameGroupInputterText.hint) {
            @Override
            public void run(String input) {
                DominionDTO dominion = toDominionDTO(dominionName);
                GroupDTO group = toGroupDTO(dominion, oldGroupName);
                GroupProvider.getInstance().renameGroup(sender, dominion, group, input);
                GroupFlags.show(sender, dominionName, ColorParser.getPlainText(input), "1");
            }
        };
    }

    public static FunctionalButton createTuiButtonOn(CommandSender sender, String dominionName, String oldGroupName) {
        return new FunctionalButton(Language.renameGroupInputterText.button) {
            @Override
            public void function() {
                createOn(sender, dominionName, oldGroupName);
            }
        };
    }
}
