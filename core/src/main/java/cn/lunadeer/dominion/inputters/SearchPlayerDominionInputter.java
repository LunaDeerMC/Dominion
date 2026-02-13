package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.uis.AllDominion;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

public class SearchPlayerDominionInputter {
    public static class SearchPlayerDominionInputterText extends ConfigurationPart {
        public String button = "SEARCH";
        public String hint = "Enter the name of the player whose dominions you want to search.";
        public String noResults = "No dominions found for this player.";
    }

    public static void createOn(CommandSender sender) {
        new InputterRunner(sender, Language.searchPlayerDominionInputterText.hint) {
            @Override
            public void run(String input) {
                AllDominion.showSearchResults(sender, input, "1");
            }
        };
    }

    public static FunctionalButton createTuiButtonOn(CommandSender sender) {
        return new FunctionalButton(Language.searchPlayerDominionInputterText.button) {
            @Override
            public void function() {
                createOn(sender);
            }
        };
    }
}