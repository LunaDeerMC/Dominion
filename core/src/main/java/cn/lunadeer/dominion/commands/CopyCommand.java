package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.uis.dominion.manage.EnvFlags;
import cn.lunadeer.dominion.uis.dominion.manage.GuestFlags;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupList;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberList;
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
            for (EnvFlag flag : fromDominion.getEnvironmentFlagValue().keySet()) {
                if (toDominion.getEnvFlagValue(flag) == fromDominion.getEnvFlagValue(flag)) continue;
                toDominion.setEnvFlagValue(flag, fromDominion.getEnvFlagValue(flag));
            }
            Notification.info(sender, Language.copyCommandText.copyEnvSuccess, fromDominion.getName(), toDominion.getName());
            EnvFlags.show(sender, to, "1");
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
            for (PriFlag flag : fromDominion.getGuestPrivilegeFlagValue().keySet()) {
                if (toDominion.getGuestFlagValue(flag) == fromDominion.getGuestFlagValue(flag)) continue;
                toDominion.setGuestFlagValue(flag, fromDominion.getGuestFlagValue(flag));
            }
            Notification.info(sender, Language.copyCommandText.copyGuestSuccess, fromDominion.getName(), toDominion.getName());
            GuestFlags.show(sender, to, "1");
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
            for (MemberDTO member : fromDominion.getMembers()) {
                try {
                    MemberDTO toMember = CacheManager.instance.getMember(toDominion, member.getPlayerUUID());
                    if (toMember == null) {
                        toMember = MemberProvider.getInstance().addMember(sender, toDominion, member.getPlayer()).get();
                        if (toMember == null) continue;
                    }
                    for (PriFlag flag : member.getFlagsValue().keySet()) {
                        if (toMember.getFlagValue(flag) == member.getFlagValue(flag)) continue;
                        MemberProvider.getInstance().setMemberFlag(sender,
                                toDominion,
                                toMember,
                                flag,
                                member.getFlagValue(flag));
                    }
                } catch (Exception e) {
                    Notification.warn(sender, e.getMessage());
                }
            }
            Notification.info(sender, Language.copyCommandText.copyMemberSuccess, fromDominion.getName(), toDominion.getName());
            MemberList.show(sender, to, "1");
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
            copyMember(sender, from, to); // copy member first
            DominionDTO fromDominion = toDominionDTO(from);
            DominionDTO toDominion = toDominionDTO(to);
            for (GroupDTO group : fromDominion.getGroups()) {
                try {
                    GroupDTO toGroup = toDominion.getGroups().stream()
                            .filter(g -> g.getNamePlain().equals(group.getNamePlain()))
                            .findFirst()
                            .orElse(null);
                    if (toGroup == null) {
                        // create group in target dominion
                        GroupDTO groupCreated = GroupProvider.getInstance().createGroup(sender, toDominion, group.getNameRaw()).get();
                        if (groupCreated == null) continue;
                        toGroup = groupCreated;
                    }
                    // set group flags
                    for (PriFlag flag : group.getFlagsValue().keySet()) {
                        if (toGroup.getFlagValue(flag) == group.getFlagValue(flag)) continue;
                        GroupProvider.getInstance().setGroupFlag(sender,
                                toDominion,
                                toGroup,
                                flag,
                                group.getFlagValue(flag));
                    }
                    // set group members
                    for (MemberDTO fromMember : fromDominion.getMembers()) {
                        MemberDTO toMember = CacheManager.instance.getMember(toDominion, fromMember.getPlayerUUID());
                        if (toMember == null) continue;
                        if (toMember.getGroupId().equals(toGroup.getId())) continue;
                        GroupProvider.getInstance().addMember(sender, toDominion, toGroup, toMember);
                    }
                } catch (Exception e) {
                    Notification.warn(sender, e.getMessage());
                }
            }
            Notification.info(sender, Language.copyCommandText.copyGroupSuccess, fromDominion.getName(), toDominion.getName());
            GroupList.show(sender, to, "1");
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
