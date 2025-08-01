package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.events.member.MemberAddedEvent;
import cn.lunadeer.dominion.events.member.MemberRemovedEvent;
import cn.lunadeer.dominion.events.member.MemberSetFlagEvent;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberFlags;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberList;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.Argument;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.*;

public class MemberCommand {

    public static class MemberCommandText extends ConfigurationPart {
        public String addMemberDescription = "Add a player as member to a dominion.";
        public String setMemberPrivilegeDescription = "Set privilege flag for a member in a dominion.";
        public String removeMemberDescription = "Remove a member from a dominion.";
    }

    /**
     * Command to add a member to a dominion.
     */
    public static SecondaryCommand addMember = new SecondaryCommand("member_add", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new Argument("player_name", true)
    ), Language.memberCommandText.addMemberDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            addMember(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    /**
     * Adds a member to a dominion.
     *
     * @param sender       the command sender
     * @param dominionName the name of the dominion
     * @param playerName   the name of the player to be added
     */
    public static void addMember(CommandSender sender, String dominionName, String playerName) {
        try {
            // PlayerDTO player = toPlayerDTO(playerName); // Old implementation
            // New implementation START
            // Compatible with some bot plugin, because they may not be recorded on join.
            // But this may need a better solution in the future.
            PlayerDTO player = null;
            try {
                player = toPlayerDTO(playerName);
            } catch (Exception e) {
                Player bukkitPlayer = Dominion.instance.getServer().getPlayer(playerName);
                if (bukkitPlayer != null) player = PlayerDOO.create(bukkitPlayer);
                if (player == null) throw e;
            }
            // New implementation END
            DominionDTO dominion = toDominionDTO(dominionName);
            new MemberAddedEvent(sender, dominion, player).call();
            MemberList.show(sender, dominionName, "1");
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    /**
     * Command to set a member's privilege in a dominion.
     */
    public static SecondaryCommand setMemberPrivilege = new SecondaryCommand("member_set_pri", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredMemberArgument(0),
            new CommandArguments.PriFlagArgument(),
            new CommandArguments.BollenOption(),
            new CommandArguments.OptionalPageArgument()
    ), Language.memberCommandText.setMemberPrivilegeDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            setMemberPrivilege(
                    sender,
                    getArgumentValue(0),
                    getArgumentValue(1),
                    getArgumentValue(2),
                    getArgumentValue(3),
                    getArgumentValue(4)
            );
        }
    }.needPermission(defaultPermission).register();

    /**
     * Sets a member's privilege in a dominion.
     *
     * @param sender       the command sender
     * @param dominionName the name of the dominion
     * @param playerName   the name of the player
     * @param flagName     the name of the privilege flag
     * @param valueStr     the value of the privilege flag
     * @param pageStr      the page number for the member setting display
     */
    public static void setMemberPrivilege(CommandSender sender, String dominionName, String playerName, String flagName, String valueStr, String pageStr) {
        try {
            PriFlag flag = toPriFlag(flagName);
            boolean value = toBoolean(valueStr);
            DominionDTO dominion = toDominionDTO(dominionName);
            MemberDTO member = toMemberDTO(dominion, playerName);
            new MemberSetFlagEvent(sender, dominion, member, flag, value).call();
            MemberFlags.show(sender, dominionName, playerName, pageStr);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    /**
     * Command to remove a member from a dominion.
     */
    public static SecondaryCommand removeMember = new SecondaryCommand("member_remove", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredMemberArgument(0),
            new CommandArguments.OptionalPageArgument()
    ), Language.memberCommandText.removeMemberDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            removeMember(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2));
        }
    }.needPermission(defaultPermission).register();

    /**
     * Removes a member from a dominion.
     *
     * @param sender       the command sender
     * @param dominionName the name of the dominion
     * @param playerName   the name of the player to be removed
     * @param pageStr      the page number for the member list display
     */
    public static void removeMember(CommandSender sender, String dominionName, String playerName, String pageStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            MemberDTO member = toMemberDTO(dominion, playerName);
            new MemberRemovedEvent(sender, dominion, member).call();
            MemberList.show(sender, dominionName, pageStr);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

}
