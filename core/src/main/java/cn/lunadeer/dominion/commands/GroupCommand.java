package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupFlags;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupList;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.Argument;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.command.CommandSender;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.*;

public class GroupCommand {

    public static class GroupCommandText extends ConfigurationPart {
        public String createGroupDescription = "Create a new group in a dominion.";
        public String deleteGroupDescription = "Delete a group from a dominion.";
        public String renameGroupDescription = "Rename a group in a dominion.";
        public String setGroupFlagDescription = "Set privilege flag for a group.";
        public String addMemberDescription = "Add a member to a group.";
        public String removeMemberDescription = "Remove a member from a group.";
    }

    public static SecondaryCommand createGroup = new SecondaryCommand("group_create", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new Argument("group_name", true)
    ), Language.groupCommandText.createGroupDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            createGroup(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static void createGroup(CommandSender sender, String dominionName, String groupName) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            GroupProvider.getInstance().createGroup(sender, dominion, groupName);
            GroupList.show(sender, dominion.getName(), "1");
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand deleteGroup = new SecondaryCommand("group_delete", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredGroupArgument(0),
            new CommandArguments.OptionalPageArgument()
    ), Language.groupCommandText.deleteGroupDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            deleteGroup(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2));
        }
    }.needPermission(defaultPermission).register();

    public static void deleteGroup(CommandSender sender, String dominionName, String groupName, String pageStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            GroupDTO group = toGroupDTO(dominion, groupName);
            GroupProvider.getInstance().deleteGroup(sender, dominion, group);
            GroupList.show(sender, dominion.getName(), pageStr);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand renameGroup = new SecondaryCommand("group_rename", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredGroupArgument(0),
            new Argument("new_group_name", true)
    ), Language.groupCommandText.renameGroupDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            renameGroup(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2));
        }
    }.needPermission(defaultPermission).register();

    public static void renameGroup(CommandSender sender, String dominionName, String oldGroupName, String newGroupName) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            GroupDTO group = toGroupDTO(dominion, oldGroupName);
            GroupProvider.getInstance().renameGroup(sender, dominion, group, newGroupName);
            GroupFlags.show(sender, dominion.getName(), newGroupName, "1");
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand setGroupFlag = new SecondaryCommand("group_set_flag", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredGroupArgument(0),
            new CommandArguments.PriFlagArgument(),
            new CommandArguments.BollenOption(),
            new CommandArguments.OptionalPageArgument()
    ), Language.groupCommandText.setGroupFlagDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            setGroupFlag(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2), getArgumentValue(3), getArgumentValue(4));
        }
    }.needPermission(defaultPermission).register();

    public static void setGroupFlag(CommandSender sender, String dominionName, String groupName, String flagName, String valueStr, String pageStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            GroupDTO group = toGroupDTO(dominion, groupName);
            PriFlag flag = toPriFlag(flagName);
            boolean value = toBoolean(valueStr);
            GroupProvider.getInstance().setGroupFlag(sender, dominion, group, flag, value);
            GroupFlags.show(sender, dominionName, groupName, pageStr);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand addMember = new SecondaryCommand("group_add_member", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredGroupArgument(0),
            new CommandArguments.RequiredMemberArgument(0)
    ), Language.groupCommandText.addMemberDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            addMember(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2));
        }
    }.needPermission(defaultPermission).register();

    public static void addMember(CommandSender sender, String dominionName, String groupName, String playerName) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            MemberDTO member = toMemberDTO(dominion, playerName);
            GroupDTO group = toGroupDTO(dominion, groupName);
            GroupProvider.getInstance().addMember(sender, dominion, group, member);
            GroupList.show(sender, dominion.getName(), "1");
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand removeMember = new SecondaryCommand("group_remove_member", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredGroupArgument(0),
            new CommandArguments.RequiredMemberArgument(0),
            new CommandArguments.OptionalPageArgument()
    ), Language.groupCommandText.removeMemberDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            removeMember(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2), getArgumentValue(3));
        }
    }.needPermission(defaultPermission).register();

    public static void removeMember(CommandSender sender, String dominionName, String groupName, String playerName, String pageStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            GroupDTO group = toGroupDTO(dominion, groupName);
            MemberDTO member = toMemberDTO(dominion, playerName);
            GroupProvider.getInstance().removeMember(sender, dominion, group, member);
            GroupList.show(sender, dominion.getName(), pageStr);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

}
