package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.doos.MemberDOO;
import cn.lunadeer.dominion.events.member.MemberAddedEvent;
import cn.lunadeer.dominion.events.member.MemberRemovedEvent;
import cn.lunadeer.dominion.events.member.MemberSetFlagEvent;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static cn.lunadeer.dominion.misc.Asserts.*;

public class MemberProviderHandler extends MemberProvider {

    public static class MemberProviderHandlerText extends ConfigurationPart {
        public String setFlagSuccess = "Successfully set flag {0} for {1} in {2}.";
        public String ownerOnly = "Only owner can manage admin member.";
        public String groupAlready = "This member belong to group {0} so you can't manage it separately.";
        public String setFlagFailed = "Failed to set flag, reason: {0}";

        public String addMemberSuccess = "Successfully added {0} to {1}.";
        public String alreadyMember = "{0} is already a member of {1}.";
        public String cantBeOwner = "You can't add dominion owner as a member.";
        public String addMemberFailed = "Failed to add member, reason: {0}";

        public String removeMemberSuccess = "Successfully removed {0} from {1}.";
        public String removeMemberFailed = "Failed to remove member, reason: {0}";
    }

    public MemberProviderHandler(JavaPlugin plugin) {
        instance = this;
    }

    @Override
    public CompletableFuture<MemberDTO> setMemberFlag(@NotNull CommandSender operator,
                                                      @NotNull DominionDTO dominion,
                                                      @NotNull MemberDTO member,
                                                      @NotNull PriFlag flag,
                                                      boolean newValue) {
        MemberSetFlagEvent event = new MemberSetFlagEvent(operator, dominion, member, flag, newValue);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                if (event.getFlag().equals(Flags.ADMIN)) {
                    assertDominionOwner(event.getOperator(), event.getDominion());
                } else {
                    assertDominionAdmin(event.getOperator(), event.getDominion());
                }
                assertMemberBelongDominion(event.getMember(), event.getDominion());
                if (event.getMember().getGroupId() != -1) {
                    GroupDTO group = Objects.requireNonNull(CacheManager.instance.getCache(event.getDominion().getServerId())).getGroupCache().getGroup(event.getMember().getGroupId());
                    if (group == null) return null;
                    throw new DominionException(Language.memberProviderHandlerText.groupAlready, group.getNamePlain());
                }
                MemberDTO memberModified = event.getMember().setFlagValue(event.getFlag(), event.getNewValue());
                Notification.info(event.getOperator(), Language.memberProviderHandlerText.setFlagSuccess,
                        event.getFlag().getFlagName(), event.getMember().getPlayer().getLastKnownName(), event.getDominion().getName());
                return memberModified;
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.memberProviderHandlerText.setFlagFailed, e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<MemberDTO> addMember(@NotNull CommandSender operator,
                                                  @NotNull DominionDTO dominion,
                                                  @NotNull PlayerDTO player) {
        MemberAddedEvent event = new MemberAddedEvent(operator, dominion, player);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionAdmin(event.getOperator(), event.getDominion());
                if (event.getPlayer().getUuid().equals(event.getDominion().getOwner())) {
                    throw new DominionException(Language.memberProviderHandlerText.cantBeOwner);
                }
                if (event.getDominion().getMembers().stream().anyMatch(m -> m.getPlayer().getUuid().equals(event.getPlayer().getUuid()))) {
                    throw new DominionException(Language.memberProviderHandlerText.alreadyMember,
                            event.getPlayer().getLastKnownName(), dominion.getName());
                }
                MemberDTO member = MemberDOO.insert(new MemberDOO(event.getPlayer().getUuid(), event.getDominion()));
                Notification.info(event.getOperator(), Language.memberProviderHandlerText.addMemberSuccess,
                        event.getPlayer().getLastKnownName(), dominion.getName());
                return member;
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.memberProviderHandlerText.addMemberFailed, e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<MemberDTO> removeMember(@NotNull CommandSender operator,
                                                     @NotNull DominionDTO dominion,
                                                     @NotNull MemberDTO member) {
        MemberRemovedEvent event = new MemberRemovedEvent(operator, dominion, member);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionAdmin(event.getOperator(), event.getDominion());
                assertMemberBelongDominion(event.getMember(), event.getDominion());
                boolean owner = false;
                try {
                    assertDominionOwner(event.getOperator(), event.getDominion());
                    owner = true;
                } catch (DominionException ignored) {
                }
                GroupDTO group = Objects.requireNonNull(CacheManager.instance.getCache(event.getDominion().getServerId())).getGroupCache().getGroup(member.getGroupId());
                if (group != null) {
                    if (group.getFlagValue(Flags.ADMIN) && !owner) {
                        throw new DominionException(Language.groupProviderHandlerText.ownerOnly);
                    }
                } else {
                    if (event.getMember().getFlagValue(Flags.ADMIN) && !owner) {
                        throw new DominionException(Language.memberProviderHandlerText.ownerOnly);
                    }
                }
                MemberDOO.deleteById(event.getMember().getId());
                Notification.info(event.getOperator(), Language.memberProviderHandlerText.removeMemberSuccess,
                        event.getMember().getPlayer().getLastKnownName(), dominion.getName());
                return event.getMember();
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.memberProviderHandlerText.removeMemberFailed, e.getMessage());
                return null;
            }
        });
    }
}
