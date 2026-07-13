package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.providers.CopyProvider;
import cn.lunadeer.dominion.providers.CopyType;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.command.CommandSender;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;

public class CopyCommand {

    public static class CopyCommandText extends ConfigurationPart {
        public String copyEnvSuccess = "Copied environment flag from {0} to {1} success.";
        public String copyGuestSuccess = "Copied guest privilege flag from {0} to {1} success.";
        public String copyMemberSuccess = "Copied members from {0} to {1} success.";
        public String copyGroupSuccess = "Copied groups from {0} to {1} success.";
        public String copyEnvironmentDescription = "Copy environment flags from one dominion to another.";
        public String copyGuestDescription = "Copy guest privilege flags from one dominion to another.";
        public String copyMemberDescription = "Copy members from one dominion to another.";
        public String copyGroupDescription = "Copy groups from one dominion to another.";
    }

    public static void copyEnvironment(CommandSender sender, String from, String to) {
        try {
            DominionDTO fromDominion = toDominionDTO(from);
            DominionDTO toDominion = toDominionDTO(to);
            CopyProvider.getInstance().copy(sender, fromDominion, toDominion, CopyType.ENVIRONMENT);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand copyEnvironmentCommand = new SecondaryCommand("copy_env", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredDominionArgument()
    ), Language.copyCommandText.copyEnvironmentDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            copyEnvironment(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static void copyGuest(CommandSender sender, String from, String to) {
        try {
            DominionDTO fromDominion = toDominionDTO(from);
            DominionDTO toDominion = toDominionDTO(to);
            CopyProvider.getInstance().copy(sender, fromDominion, toDominion, CopyType.GUEST);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand copyGuestCommand = new SecondaryCommand("copy_guest", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredDominionArgument()
    ), Language.copyCommandText.copyGuestDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            copyGuest(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static void copyMember(CommandSender sender, String from, String to) {
        try {
            DominionDTO fromDominion = toDominionDTO(from);
            DominionDTO toDominion = toDominionDTO(to);
            CopyProvider.getInstance().copy(sender, fromDominion, toDominion, CopyType.MEMBER);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand copyMemberCommand = new SecondaryCommand("copy_member", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredDominionArgument()
    ), Language.copyCommandText.copyMemberDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            copyMember(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static void copyGroup(CommandSender sender, String from, String to) {
        try {
            DominionDTO fromDominion = toDominionDTO(from);
            DominionDTO toDominion = toDominionDTO(to);
            CopyProvider.getInstance().copy(sender, fromDominion, toDominion, CopyType.GROUP);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand copyGroupCommand = new SecondaryCommand("copy_group", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredDominionArgument()
    ), Language.copyCommandText.copyGroupDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            copyGroup(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();
}
