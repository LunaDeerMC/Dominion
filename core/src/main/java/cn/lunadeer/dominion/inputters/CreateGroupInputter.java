package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupList;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;

public class CreateGroupInputter {

    public static class CreateGroupInputterText extends ConfigurationPart {
        public String button = "CREATE";
        public String hint = "Enter new group name you want to create.";
    }

    public static void createOn(CommandSender sender, String dominionName) {
        new InputterRunner(sender, Language.createGroupInputterText.hint) {
            @Override
            public void run(String input) {
                DominionDTO dominion = toDominionDTO(dominionName);
                GroupProvider.getInstance().createGroup(sender, dominion, input);
                GroupList.show(sender, dominionName, "1");
            }
        };
    }

    public static FunctionalButton createTuiButtonOn(CommandSender sender, String dominionName) {
        return new FunctionalButton(Language.createGroupInputterText.button) {
            @Override
            public void function() {
                createOn(sender, dominionName);
            }
        };
    }
}
