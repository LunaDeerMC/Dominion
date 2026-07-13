package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.providers.PlayerProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import static cn.lunadeer.dominion.misc.Converts.*;

public class GroupTitleCommand {

    public static class GroupTitleCommandText extends ConfigurationPart {
        public String groupNotBelonging = "Don't belong to group {0}.";
        public String usingTitleSuccess = "Using title {0} successfully.";
        public String usingTitleFail = "Failed to use title, reason: {0}";
        public String useTitleDescription = "Use a group title or disable current title.";
    }

    public static SecondaryCommand useTitle = new SecondaryCommand("title_use", List.of(
            new CommandArguments.PlayerTitleIdArgument()
    ), Language.groupTitleCommandText.useTitleDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            useTitle(sender, getArgumentValue(0));
        }
    }.needPermission("dominion.default").register();

    /**
     * Uses a title for a player.
     * This method allows a player to use a specific group title. It verifies the player's ownership or membership
     * in the dominion associated with the group title and sets the title for the player if the checks pass.
     *
     * @param sender          The command sender.
     * @param groupTitleIdStr The ID of the group title as a string. -1 for disuse current title.
     */
    public static void useTitle(CommandSender sender, String groupTitleIdStr) {
        try {
            Player player = toPlayer(sender);
            int titleId = toIntegrity(groupTitleIdStr);
            if (titleId == -1) {
                PlayerProvider.getInstance().setGroupTitle(player, null);
                return;
            }
            GroupDTO group = toGroupDTO(titleId);
            PlayerProvider.getInstance().setGroupTitle(player, group);
        } catch (Exception e) {
            Notification.error(sender, Language.groupTitleCommandText.usingTitleFail, e.getMessage());
        }
    }

}
