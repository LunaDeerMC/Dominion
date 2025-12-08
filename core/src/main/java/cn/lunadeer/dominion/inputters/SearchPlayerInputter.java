package cn.lunadeer.dominion.inputters;

import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.uis.dominion.manage.member.SelectPlayer;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.inputter.InputterRunner;
import org.bukkit.command.CommandSender;

public class SearchPlayerInputter {
    public static class SearchPlayerInputterText extends ConfigurationPart {
        public String button = "SEARCH";
        public String hint = "Enter the name of the player you want to search.";
        public String noResults = "No players found matching your search.";
    }

    public static void createOn(CommandSender sender, String dominionName) {
        new InputterRunner(sender, Language.searchPlayerInputterText.hint) {
            @Override
            public void run(String input) {
                // Show search results in SelectPlayer GUI
                SelectPlayer.showSearchResults(sender, dominionName, input, "1");
            }
        };
    }

    public static FunctionalButton createTuiButtonOn(CommandSender sender, String dominionName) {
        return new FunctionalButton(Language.searchPlayerInputterText.button) {
            @Override
            public void function() {
                createOn(sender, dominionName);
            }
        };
    }
}
