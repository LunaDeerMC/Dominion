package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.commands.DominionOperateCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.events.dominion.DominionCreateEvent;
import cn.lunadeer.dominion.events.dominion.DominionDeleteEvent;
import cn.lunadeer.dominion.events.dominion.modify.*;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.BorderRenderUtil;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static cn.lunadeer.dominion.misc.Asserts.*;
import static cn.lunadeer.dominion.misc.Converts.toPlayer;
import static cn.lunadeer.dominion.misc.Others.getSubDominionsRecursive;

/**
 * Handles various dominion-related events such as creation, deletion, size change, renaming, transfer,
 * setting teleportation location, setting messages, setting map color, and setting flags.
 * This class implements the Listener interface to handle events in a Bukkit plugin.
 */
public class DominionProviderHandler extends DominionProvider {

    public static class DominionProviderHandlerText extends ConfigurationPart {
        public String createSuccess = "Create dominion {0} success.";
        public String createFailed = "Create dominion {0} failed, reason: {1}";

        public String expandSuccess = "Expand dominion {0} success.";
        public String expandFailed = "Expand dominion {0} failed, reason: {1}";
        public String contractSuccess = "Contract dominion {0} success.";
        public String contractFailed = "Contract dominion {0} failed, reason: {1}";

        public String deleteSuccess = "Delete dominion {0} success.";
        public String deleteFailed = "Delete dominion {0} failed, reason: {1}";
        public String deleteConfirm = "Use command '{0}' to confirm delete the dominion {1} and its subs, this operation cannot be undone.";
        public String listSubDoms = "The dominion {0} has subs: {1}";

        public String renameFailed = "Rename dominion {0} failed, reason: {1}";
        public String renameSuccess = "Rename dominion {0} to {1} success.";
        public String sameName = "The new name is the same as the old name.";

        public String giveSuccess = "Give dominion {0} to {1} success.";
        public String giveFailed = "Give dominion {0} to other failed, reason: {1}";
        public String giveConfirm = "Use command '{0}' to confirm give the dominion {1} to {2}, this operation cannot be undone.";
        public String alreadyBelong = "The dominion {0} already belongs to {1}.";
        public String cannotGiveSub = "Dominion {0} is a sub-dominion, cannot give it to others.";

        public String tpLocationNotInDominion = "The teleportation location is not in the dominion {0}.";
        public String tpLocationSetSuccess = "Set teleportation location for dominion {0} success.";
        public String tpLocationSetFailed = "Set teleportation location for dominion {0} failed, reason: {1}";

        public String setEnterMessageSuccess = "Set enter message for dominion {0} success.";
        public String setEnterMessageFailed = "Set enter message for dominion {0} failed, reason: {1}";
        public String setLeaveMessageSuccess = "Set leave message for dominion {0} success.";
        public String setLeaveMessageFailed = "Set leave message for dominion {0} failed, reason: {1}";

        public String SetMapColorSuccess = "Set map color for dominion {0} success.";
        public String SetMapColorFailed = "Set map color for dominion {0} failed, reason: {1}";

        public String setEnvFlagSuccess = "Set env flag {0} to {1} success.";
        public String setEnvFlagFailed = "Set env flag {0} to {1} failed, reason: {2}";
        public String setGuestFlagSuccess = "Set guest flag {0} to {1} success.";
        public String setGuestFlagFailed = "Set guest flag {0} to {1} failed, reason: {2}";

    }

    public DominionProviderHandler(JavaPlugin plugin) {
        instance = this;
    }

    @Override
    public CompletableFuture<DominionDTO> createDominion(@NotNull CommandSender operator,
                                                         @NotNull String name, @NotNull UUID owner,
                                                         @NotNull World world, @NotNull CuboidDTO cuboid,
                                                         @Nullable DominionDTO parent, boolean skipEconomy) {
        DominionCreateEvent event = new DominionCreateEvent(operator, name, owner, world, cuboid, null);
        event.setSkipEconomy(skipEconomy);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                DominionDOO toBeCreated = new DominionDOO(
                        event.getOwner(),
                        event.getName(),
                        event.getWorld().getUID(),
                        event.getCuboid(),
                        parent == null ? -1 : parent.getId()
                );
                // name check
                assertDominionName(event.getName());
                // amount check
                assertPlayerDominionAmount(event.getOperator(), event.getWorld().getUID(), parent != null);
                // size check
                assertDominionSize(event.getOperator(), event.getWorld().getUID(), event.getCuboid());
                // parent check
                assertWithinParent(event.getOperator(), toBeCreated, event.getCuboid());
                assertSubDepth(event.getOperator(), toBeCreated);
                // intersect check
                assertDominionIntersect(event.getOperator(), toBeCreated, event.getCuboid());
                // handle economy
                if (!event.isSkipEconomy()) {
                    assertEconomy(event.getOperator(), CuboidDTO.ZERO, toBeCreated.getCuboid(), parent != null);
                }
                // do db insert
                DominionDTO inserted = DominionDOO.insert(toBeCreated);
                BorderRenderUtil.showBorder(event.getOperator(), inserted);
                Notification.info(event.getOperator(), Language.dominionProviderHandlerText.createSuccess, event.getName());
                return inserted;
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.dominionProviderHandlerText.createFailed, event.getName(), e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> resizeDominion(@NotNull CommandSender operator,
                                                         @NotNull DominionDTO dominion,
                                                         @NotNull DominionReSizeEvent.TYPE type,
                                                         @NotNull DominionReSizeEvent.DIRECTION direction,
                                                         int size) {
        DominionReSizeEvent event = new DominionReSizeEvent(operator, dominion, type, direction, size);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            long amount = event.getNewCuboid().minusVolumeWith(event.getOldCuboid());
            if (amount == 0) {
                return null;
            }
            boolean expand = amount > 0;
            try {
                assertDominionOwner(event.getOperator(), event.getDominion());
                assertDominionSize(event.getOperator(), event.getDominion().getWorldUid(), event.getNewCuboid());
                assertWithinParent(event.getOperator(), event.getDominion(), event.getNewCuboid());
                assertContainSubs(event.getOperator(), event.getDominion(), event.getNewCuboid());
                assertDominionIntersect(event.getOperator(), event.getDominion(), event.getNewCuboid());
                if (!event.isSkipEconomy()) {
                    assertEconomy(event.getOperator(), event.getOldCuboid(), event.getNewCuboid(), dominion.getParentDomId() != -1);
                }
                event.setDominion(event.getDominion().setCuboid(event.getNewCuboid()));
                BorderRenderUtil.showBorder(event.getOperator(), event.getDominion());
                if (expand) {
                    Notification.info(event.getOperator(), Language.dominionProviderHandlerText.expandSuccess, event.getDominion().getName());
                } else {
                    Notification.info(event.getOperator(), Language.dominionProviderHandlerText.contractSuccess, event.getDominion().getName());
                }
                return event.getDominion();
            } catch (Exception e) {
                if (expand) {
                    Notification.error(event.getOperator(), Language.dominionProviderHandlerText.expandFailed, event.getDominion().getName(), e.getMessage());
                } else {
                    Notification.error(event.getOperator(), Language.dominionProviderHandlerText.contractFailed, event.getDominion().getName(), e.getMessage());
                }
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> deleteDominion(@NotNull CommandSender operator,
                                                         @NotNull DominionDTO dominion,
                                                         boolean skipEconomy,
                                                         boolean force) {
        DominionDeleteEvent event = new DominionDeleteEvent(operator, dominion);
        event.setForce(force);
        event.setSkipEconomy(skipEconomy);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return CompletableFuture.supplyAsync(() -> {
            try {
                assertDominionOwner(event.getOperator(), event.getDominion());
                // check subs
                List<DominionDTO> sub_dominions = getSubDominionsRecursive(event.getDominion());
                if (!event.isForce()) {
                    if (!sub_dominions.isEmpty()) {
                        Notification.warn(event.getOperator(), Language.dominionProviderHandlerText.listSubDoms, event.getDominion().getName(), String.join(", ", sub_dominions.stream().map(DominionDTO::getName).toList()));
                    }
                    Notification.warn(event.getOperator(), Language.dominionProviderHandlerText.deleteConfirm, DominionOperateCommand.delete.getUsage(), event.getDominion().getName());
                    return null;
                }
                for (DominionDTO sub_dominion : sub_dominions) {
                    DominionDOO.deleteById(sub_dominion.getId());
                    Notification.info(event.getOperator(), Language.dominionProviderHandlerText.deleteSuccess, sub_dominion.getName());
                    if (!event.isSkipEconomy())
                        assertEconomy(event.getOperator(), sub_dominion.getCuboid(), CuboidDTO.ZERO, true);
                }
                DominionDOO.deleteById(event.getDominion().getId());
                Notification.info(event.getOperator(), Language.dominionProviderHandlerText.deleteSuccess, event.getDominion().getName());
                if (!event.isSkipEconomy())
                    assertEconomy(event.getOperator(), event.getDominion().getCuboid(), CuboidDTO.ZERO, event.getDominion().getParentDomId() != -1);
                return event.getDominion();
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.dominionProviderHandlerText.deleteFailed, event.getDominion().getName(), e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> renameDominion(@NotNull CommandSender operator,
                                                         @NotNull DominionDTO dominion,
                                                         @NotNull String newName) {
        DominionRenameEvent event = new DominionRenameEvent(operator, dominion, newName);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionOwner(event.getOperator(), dominion);
                if (Objects.equals(event.getOldName(), event.getNewName())) {
                    throw new DominionException(Language.dominionProviderHandlerText.sameName);
                }
                assertDominionName(event.getNewName());
                event.setDominion(dominion.setName(event.getNewName()));
                Notification.info(event.getOperator(), Language.dominionProviderHandlerText.renameSuccess, event.getOldName(), event.getNewName());
                return event.getDominion();
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.dominionProviderHandlerText.renameFailed, event.getOldName(), e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> transferDominion(@NotNull CommandSender operator,
                                                           @NotNull DominionDTO dominion,
                                                           @NotNull PlayerDTO newOwner,
                                                           boolean force) {
        DominionTransferEvent event = new DominionTransferEvent(operator, dominion, newOwner);
        event.setForce(force);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionOwner(event.getOperator(), event.getDominion());
                if (dominion.getParentDomId() != -1) {
                    throw new DominionException(Language.dominionProviderHandlerText.cannotGiveSub, event.getDominion().getName());
                }
                Player newOwnerBukkit = toPlayer(event.getNewOwner().getUuid());
                if (newOwnerBukkit.getUniqueId().equals(event.getOldOwner().getUuid())) {
                    throw new DominionException(Language.dominionProviderHandlerText.alreadyBelong,
                            event.getDominion().getName(), newOwnerBukkit.getName());
                }
                assertPlayerDominionAmount(newOwnerBukkit, event.getDominion().getWorldUid(), false);
                List<DominionDTO> sub_dominions = getSubDominionsRecursive(event.getDominion());
                if (!event.isForce()) {
                    if (!sub_dominions.isEmpty()) {
                        Notification.warn(event.getOperator(), Language.dominionProviderHandlerText.listSubDoms,
                                event.getDominion().getName(), String.join(", ", sub_dominions.stream().map(DominionDTO::getName).toList()));
                    }
                    Notification.warn(event.getOperator(), Language.dominionProviderHandlerText.giveConfirm,
                            DominionOperateCommand.give.getUsage(), event.getDominion().getName(), newOwnerBukkit.getName());
                    return null;
                }
                for (DominionDTO sub_dominion : sub_dominions) {
                    sub_dominion.setOwner(newOwnerBukkit.getUniqueId());
                    Notification.info(event.getOperator(), Language.dominionProviderHandlerText.giveSuccess,
                            sub_dominion.getName(), newOwnerBukkit.getName());
                }
                event.setDominion(event.getDominion().setOwner(newOwnerBukkit.getUniqueId()));
                Notification.info(event.getOperator(), Language.dominionProviderHandlerText.giveSuccess,
                        event.getDominion().getName(), newOwnerBukkit.getName());
                return event.getDominion();
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.dominionProviderHandlerText.giveFailed,
                        event.getDominion().getName(), e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> setDominionTpLocation(@NotNull CommandSender operator,
                                                                @NotNull DominionDTO dominion,
                                                                @NotNull Location newTpLocation) {
        DominionSetTpLocationEvent event = new DominionSetTpLocationEvent(operator, dominion, newTpLocation);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionOwner(event.getOperator(), event.getDominion());
                DominionDTO d = CacheManager.instance.getCache().getDominionCache().getDominion(event.getNewTpLocation());
                if (d == null || !d.getId().equals(event.getDominion().getId())) {
                    throw new DominionException(Language.dominionProviderHandlerText.tpLocationNotInDominion, event.getDominion().getName());
                }
                event.setDominion(event.getDominion().setTpLocation(event.getNewTpLocation()));
                Notification.info(event.getOperator(), Language.dominionProviderHandlerText.tpLocationSetSuccess, event.getDominion().getName());
                return event.getDominion();
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.dominionProviderHandlerText.tpLocationSetFailed, event.getDominion().getName(), e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> setDominionMessage(@NotNull CommandSender operator,
                                                             @NotNull DominionDTO dominion,
                                                             @NotNull DominionSetMessageEvent.TYPE type,
                                                             @NotNull String newMessage) {
        DominionSetMessageEvent event = new DominionSetMessageEvent(operator, dominion, type, newMessage);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionOwner(event.getOperator(), event.getDominion());
                if (event.getType() == DominionSetMessageEvent.TYPE.ENTER) {
                    event.setDominion(event.getDominion().setJoinMessage(event.getNewMessage()));
                    Notification.info(event.getOperator(), Language.dominionProviderHandlerText.setEnterMessageSuccess, event.getDominion().getName());
                } else {
                    event.setDominion(event.getDominion().setLeaveMessage(event.getNewMessage()));
                    Notification.info(event.getOperator(), Language.dominionProviderHandlerText.setLeaveMessageSuccess, event.getDominion().getName());
                }
                return event.getDominion();
            } catch (Exception e) {
                if (event.getType() == DominionSetMessageEvent.TYPE.ENTER) {
                    Notification.error(event.getOperator(), Language.dominionProviderHandlerText.setEnterMessageFailed, event.getDominion().getName(), e.getMessage());
                } else {
                    Notification.error(event.getOperator(), Language.dominionProviderHandlerText.setLeaveMessageFailed, event.getDominion().getName(), e.getMessage());
                }
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> setDominionMapColor(@NotNull CommandSender operator,
                                                              @NotNull DominionDTO dominion,
                                                              @NotNull Color newColor) {
        DominionSetMapColorEvent event = new DominionSetMapColorEvent(operator, dominion, newColor);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionOwner(event.getOperator(), event.getDominion());
                event.setDominion(event.getDominion().setColor(event.getNewColor()));
                Notification.info(event.getOperator(), Language.dominionProviderHandlerText.SetMapColorSuccess, event.getDominion().getName());
                return event.getDominion();
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.dominionProviderHandlerText.SetMapColorFailed, event.getDominion().getName(), e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> setDominionEnvFlag(@NotNull CommandSender operator,
                                                             @NotNull DominionDTO dominion,
                                                             @NotNull EnvFlag flag,
                                                             boolean newValue) {
        DominionSetEnvFlagEvent event = new DominionSetEnvFlagEvent(operator, dominion, flag, newValue);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionAdmin(event.getOperator(), event.getDominion());
                event.setDominion(event.getDominion().setEnvFlagValue(event.getFlag(), event.getNewValue()));
                Notification.info(event.getOperator(), Language.dominionProviderHandlerText.setEnvFlagSuccess, event.getFlag().getDisplayName(), event.getNewValue());
                return event.getDominion();
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.dominionProviderHandlerText.setEnvFlagFailed, event.getFlag().getDisplayName(), event.getNewValue(), e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DominionDTO> setDominionGuestFlag(@NotNull CommandSender operator,
                                                               @NotNull DominionDTO dominion,
                                                               @NotNull PriFlag flag,
                                                               boolean newValue) {
        DominionSetGuestFlagEvent event = new DominionSetGuestFlagEvent(operator, dominion, flag, newValue);
        if (!event.call())
            return CompletableFuture.completedFuture(null);
        return event.getFutureToComplete().completeAsync(() -> {
            try {
                assertDominionAdmin(event.getOperator(), event.getDominion());
                event.setDominion(event.getDominion().setGuestFlagValue(event.getFlag(), event.getNewValue()));
                Notification.info(event.getOperator(), Language.dominionProviderHandlerText.setGuestFlagSuccess, event.getFlag().getDisplayName(), event.getNewValue());
                return event.getDominion();
            } catch (Exception e) {
                Notification.error(event.getOperator(), Language.dominionProviderHandlerText.setGuestFlagFailed, event.getFlag().getDisplayName(), event.getNewValue(), e.getMessage());
                return null;
            }
        });
    }

}
