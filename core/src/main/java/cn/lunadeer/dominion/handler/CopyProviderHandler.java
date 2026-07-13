package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.providers.CopyProvider;
import cn.lunadeer.dominion.providers.CopyType;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.providers.GroupProvider;
import cn.lunadeer.dominion.providers.MemberProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static cn.lunadeer.dominion.misc.Asserts.assertDominionAdmin;

public final class CopyProviderHandler extends CopyProvider {
    public CopyProviderHandler() {
        instance = this;
    }

    @Override
    public CompletableFuture<DominionDTO> copy(@NotNull CommandSender operator,
                                               @NotNull DominionDTO source,
                                               @NotNull DominionDTO target,
                                               @NotNull CopyType type) {
        return onOperatorThread(operator, () -> {
            try {
                if (source.getId().equals(target.getId())) {
                    throw new DominionException("Source and target dominion must be different.");
                }
                assertDominionAdmin(operator, source);
                assertDominionAdmin(operator, target);
                return CompletableFuture.completedFuture(true);
            } catch (Exception e) {
                Notification.error(operator, e);
                return CompletableFuture.completedFuture(false);
            }
        }).thenCompose(valid -> valid ? CompletableFuture.supplyAsync(() -> {
            try {
                switch (type) {
                    case ENVIRONMENT -> copyEnvironment(operator, source, target);
                    case GUEST -> copyGuest(operator, source, target);
                    case MEMBER -> copyMembers(operator, source, target);
                    case GROUP -> copyGroups(operator, source, target);
                }
                String message = switch (type) {
                    case ENVIRONMENT -> Language.copyCommandText.copyEnvSuccess;
                    case GUEST -> Language.copyCommandText.copyGuestSuccess;
                    case MEMBER -> Language.copyCommandText.copyMemberSuccess;
                    case GROUP -> Language.copyCommandText.copyGroupSuccess;
                };
                Notification.info(operator, message, source.getName(), target.getName());
                return target;
            } catch (Exception e) {
                Notification.error(operator, e);
                return null;
            }
        }) : CompletableFuture.completedFuture(null));
    }

    private static void copyEnvironment(CommandSender operator, DominionDTO source, DominionDTO target) {
        for (Map.Entry<EnvFlag, Boolean> entry : source.getEnvironmentFlagValue().entrySet()) {
            if (target.getEnvFlagValue(entry.getKey()) == entry.getValue()) continue;
            onOperatorThread(operator, () -> DominionProvider.getInstance()
                    .setDominionEnvFlag(operator, target, entry.getKey(), entry.getValue())).join();
        }
    }

    private static void copyGuest(CommandSender operator, DominionDTO source, DominionDTO target) {
        for (Map.Entry<PriFlag, Boolean> entry : source.getGuestPrivilegeFlagValue().entrySet()) {
            if (target.getGuestFlagValue(entry.getKey()) == entry.getValue()) continue;
            onOperatorThread(operator, () -> DominionProvider.getInstance()
                    .setDominionGuestFlag(operator, target, entry.getKey(), entry.getValue())).join();
        }
    }

    private static Map<Integer, MemberDTO> copyMembers(CommandSender operator, DominionDTO source, DominionDTO target) {
        Map<Integer, MemberDTO> copied = new HashMap<>();
        for (MemberDTO sourceMember : source.getMembers()) {
            MemberDTO targetMember = CacheManager.instance.getMember(target, sourceMember.getPlayerUUID());
            if (targetMember == null) {
                targetMember = onOperatorThread(operator, () -> MemberProvider.getInstance()
                        .addMember(operator, target, sourceMember.getPlayer())).join();
            }
            if (targetMember == null) continue;
            copied.put(sourceMember.getId(), targetMember);
            if (targetMember.getGroupId() != -1) continue;
            for (Map.Entry<PriFlag, Boolean> entry : sourceMember.getFlagsValue().entrySet()) {
                if (targetMember.getFlagValue(entry.getKey()) == entry.getValue()) continue;
                MemberDTO memberToUpdate = targetMember;
                onOperatorThread(operator, () -> MemberProvider.getInstance()
                        .setMemberFlag(operator, target, memberToUpdate, entry.getKey(), entry.getValue())).join();
            }
        }
        return copied;
    }

    private static void copyGroups(CommandSender operator, DominionDTO source, DominionDTO target) throws Exception {
        Map<Integer, MemberDTO> members = copyMembers(operator, source, target);
        for (GroupDTO sourceGroup : source.getGroups()) {
            GroupDTO targetGroup = target.getGroups().stream()
                    .filter(group -> group.getNamePlain().equalsIgnoreCase(sourceGroup.getNamePlain()))
                    .findFirst().orElse(null);
            if (targetGroup == null) {
                targetGroup = onOperatorThread(operator, () -> GroupProvider.getInstance()
                        .createGroup(operator, target, sourceGroup.getNameRaw())).join();
            }
            if (targetGroup == null) continue;
            for (Map.Entry<PriFlag, Boolean> entry : sourceGroup.getFlagsValue().entrySet()) {
                if (targetGroup.getFlagValue(entry.getKey()) == entry.getValue()) continue;
                GroupDTO groupToUpdate = targetGroup;
                onOperatorThread(operator, () -> GroupProvider.getInstance()
                        .setGroupFlag(operator, target, groupToUpdate, entry.getKey(), entry.getValue())).join();
            }
            for (MemberDTO targetMember : mapGroupMembers(sourceGroup.getMembers(), members)) {
                if (targetGroup.getId().equals(targetMember.getGroupId())) continue;
                GroupDTO groupToUpdate = targetGroup;
                onOperatorThread(operator, () -> GroupProvider.getInstance()
                        .addMember(operator, target, groupToUpdate, targetMember)).join();
            }
        }
    }

    static List<MemberDTO> mapGroupMembers(List<MemberDTO> actualSourceMembers,
                                           Map<Integer, MemberDTO> copiedMembers) {
        return actualSourceMembers.stream()
                .map(member -> copiedMembers.get(member.getId()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static <T> CompletableFuture<T> onOperatorThread(
            CommandSender operator, Supplier<CompletableFuture<T>> operation) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Runnable invoke = () -> {
            try {
                operation.get().whenComplete((value, throwable) -> {
                    if (throwable == null) result.complete(value);
                    else result.completeExceptionally(throwable);
                });
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        };
        if (operator instanceof Player player) Scheduler.runEntityTask(invoke, player);
        else Scheduler.runTask(invoke);
        return result;
    }
}
