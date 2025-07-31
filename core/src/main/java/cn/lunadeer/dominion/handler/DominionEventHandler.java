package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.commands.DominionOperateCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.events.dominion.DominionCreateEvent;
import cn.lunadeer.dominion.events.dominion.DominionDeleteEvent;
import cn.lunadeer.dominion.events.dominion.modify.*;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.ParticleUtil;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static cn.lunadeer.dominion.misc.Asserts.*;
import static cn.lunadeer.dominion.misc.Converts.toPlayer;
import static cn.lunadeer.dominion.misc.Others.getSubDominionsRecursive;

/**
 * Handles various dominion-related events such as creation, deletion, size change, renaming, transfer,
 * setting teleportation location, setting messages, setting map color, and setting flags.
 * This class implements the Listener interface to handle events in a Bukkit plugin.
 */
public class DominionEventHandler implements Listener {

    private static final Logger log = LoggerFactory.getLogger(DominionEventHandler.class);

    public static class DominionEventHandlerText extends ConfigurationPart {
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

    public DominionEventHandler(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Handles the creation of a dominion.
     * This method performs various checks such as name validation, player dominion amount, dominion size, parent dominion, and intersection with other dominions.
     * It also handles the economic transaction if applicable and inserts the new dominion into the database.
     *
     * @param event the DominionCreateEvent containing the details of the dominion to be created
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionCreateEvent(DominionCreateEvent event) {
        if (event.isCancelled()) return;
        try {
            World world = event.getWorld();
            DominionDTO parent = event.getParent();
            DominionDOO toBeCreated = new DominionDOO(
                    event.getOwner(),
                    event.getName(),
                    world.getUID(),
                    event.getCuboid(),
                    parent == null ? -1 : parent.getId()
            );
            // name check
            assertDominionName(event.getName());
            // amount check
            assertPlayerDominionAmount(event.getOperator(), world.getUID());
            // size check
            assertDominionSize(event.getOperator(), world.getUID(), event.getCuboid());
            // parent check
            assertWithinParent(toBeCreated, event.getCuboid());
            assertSubDepth(event.getOperator(), toBeCreated);
            // intersect check
            assertDominionIntersect(event.getOperator(), toBeCreated, event.getCuboid());
            // handle economy
            if (!event.isSkipEconomy()) {
                assertEconomy(event.getOperator(), CuboidDTO.ZERO, toBeCreated.getCuboid());
            }
            // do db insert
            DominionDTO inserted = DominionDOO.insert(toBeCreated);
            event.setDominion(inserted);
            ParticleUtil.showBorder(event.getOperator(), inserted);
            Notification.info(event.getOperator(), Language.dominionEventHandlerText.createSuccess, event.getName());
        } catch (Exception e) {
            event.setCancelled(true);
            Notification.error(event.getOperator(), Language.dominionEventHandlerText.createFailed, event.getName(), e.getMessage());
        }
    }

    /**
     * Handles the size change of a dominion.
     * This method performs various checks such as dominion size, parent dominion, and intersection with other dominions.
     * It also handles the economic transaction if applicable and updates the dominion in the database.
     *
     * @param event the DominionSizeChangeEvent containing the details of the dominion size change
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionSizeChangeEvent(DominionReSizeEvent event) {
        if (event.isCancelled()) return;
        long amount = event.getNewCuboid().minusVolumeWith(event.getOldCuboid());
        if (amount == 0) {
            XLogger.debug("Dominion size change event cancelled, no size change.");
            return;
        }
        boolean expand = amount > 0;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionOwner(event.getOperator(), dominion);
            assertDominionSize(event.getOperator(), dominion.getWorldUid(), event.getNewCuboid());
            assertWithinParent(dominion, event.getNewCuboid());
            assertContainSubs(dominion, event.getNewCuboid());
            assertDominionIntersect(event.getOperator(), dominion, event.getNewCuboid());
            if (!event.isSkipEconomy()) {
                assertEconomy(event.getOperator(), event.getOldCuboid(), event.getNewCuboid());
            }
            DominionDTO modified = dominion.setCuboid(event.getNewCuboid());
            event.setDominion(modified);
            ParticleUtil.showBorder(event.getOperator(), modified);
            if (expand) {
                Notification.info(event.getOperator(), Language.dominionEventHandlerText.expandSuccess, dominion.getName());
            } else {
                Notification.info(event.getOperator(), Language.dominionEventHandlerText.contractSuccess, dominion.getName());
            }
        } catch (Exception e) {
            event.setCancelled(true);
            if (expand) {
                Notification.error(event.getOperator(), Language.dominionEventHandlerText.expandFailed, dominion.getName(), e.getMessage());
            } else {
                Notification.error(event.getOperator(), Language.dominionEventHandlerText.contractFailed, dominion.getName(), e.getMessage());
            }
        }
    }

    /**
     * Handles the deletion of a dominion.
     * This method performs various checks such as ownership and sub-dominions.
     * If the deletion is forced, it deletes all sub-dominions recursively.
     * It also handles the economic transaction if applicable and deletes the dominion from the database.
     *
     * @param event the DominionDeleteEvent containing the details of the dominion to be deleted
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionDeleteEvent(DominionDeleteEvent event) {
        if (event.isCancelled()) return;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionOwner(event.getOperator(), dominion);
            // check subs
            List<DominionDTO> sub_dominions = getSubDominionsRecursive(dominion);
            if (!event.isForce()) {
                event.setCancelled(true);
                if (!sub_dominions.isEmpty()) {
                    Notification.warn(event.getOperator(), Language.dominionEventHandlerText.listSubDoms, dominion.getName(), String.join(", ", sub_dominions.stream().map(DominionDTO::getName).toList()));
                }
                Notification.warn(event.getOperator(), Language.dominionEventHandlerText.deleteConfirm, DominionOperateCommand.delete.getUsage(), dominion.getName());
                return;
            }
            for (DominionDTO sub_dominion : sub_dominions) {
                DominionDOO.deleteById(sub_dominion.getId());
                Notification.info(event.getOperator(), Language.dominionEventHandlerText.deleteSuccess, sub_dominion.getName());
                if (!event.isSkipEconomy())
                    assertEconomy(event.getOperator(), sub_dominion.getCuboid(), CuboidDTO.ZERO);
            }
            DominionDOO.deleteById(dominion.getId());
            Notification.info(event.getOperator(), Language.dominionEventHandlerText.deleteSuccess, dominion.getName());
            if (!event.isSkipEconomy()) assertEconomy(event.getOperator(), dominion.getCuboid(), CuboidDTO.ZERO);
        } catch (Exception e) {
            event.setCancelled(true);
            Notification.error(event.getOperator(), Language.dominionEventHandlerText.deleteFailed, dominion.getName(), e.getMessage());
        }
    }

    /**
     * Handles the renaming of a dominion.
     * This method performs various checks such as ownership and name validation.
     * It updates the dominion's name in the database if all checks pass.
     *
     * @param event the DominionRenameEvent containing the details of the dominion to be renamed
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionRenameEvent(DominionRenameEvent event) {
        if (event.isCancelled()) return;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionOwner(event.getOperator(), dominion);
            if (Objects.equals(event.getOldName(), event.getNewName())) {
                throw new DominionException(Language.dominionEventHandlerText.sameName);
            }
            assertDominionName(event.getNewName());
            event.setDominion(dominion.setName(event.getNewName()));
            Notification.info(event.getOperator(), Language.dominionEventHandlerText.renameSuccess, event.getOldName(), event.getNewName());
        } catch (Exception e) {
            event.setCancelled(true);
            Notification.error(event.getOperator(), Language.dominionEventHandlerText.renameFailed, event.getOldName(), e.getMessage());
        }
    }

    /**
     * Handles the transfer of a dominion to a new owner.
     * This method performs various checks such as ownership, parent dominion, and player dominion amount.
     * It updates the dominion's owner in the database if all checks pass.
     *
     * @param event the DominionTransferEvent containing the details of the dominion to be transferred
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionTransferEvent(DominionTransferEvent event) {
        if (event.isCancelled()) return;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionOwner(event.getOperator(), dominion);
            if (dominion.getParentDomId() != -1) {
                throw new DominionException(Language.dominionEventHandlerText.cannotGiveSub, dominion.getName());
            }
            Player newOwner = toPlayer(event.getNewOwner().getUuid());
            if (newOwner.getUniqueId().equals(event.getOldOwner().getUuid())) {
                throw new DominionException(Language.dominionEventHandlerText.alreadyBelong, dominion.getName(), newOwner.getName());
            }
            assertPlayerDominionAmount(newOwner, dominion.getWorldUid());
            List<DominionDTO> sub_dominions = getSubDominionsRecursive(dominion);
            if (!event.isForce()) {
                event.setCancelled(true);
                if (!sub_dominions.isEmpty()) {
                    Notification.warn(event.getOperator(), Language.dominionEventHandlerText.listSubDoms, dominion.getName(), String.join(", ", sub_dominions.stream().map(DominionDTO::getName).toList()));
                }
                Notification.warn(event.getOperator(), Language.dominionEventHandlerText.giveConfirm, DominionOperateCommand.give.getUsage(), dominion.getName(), newOwner.getName());
                return;
            }
            for (DominionDTO sub_dominion : sub_dominions) {
                sub_dominion.setOwner(newOwner.getUniqueId());
                Notification.info(event.getOperator(), Language.dominionEventHandlerText.giveSuccess, sub_dominion.getName(), newOwner.getName());
            }
            event.setDominion(dominion.setOwner(newOwner.getUniqueId()));
            Notification.info(event.getOperator(), Language.dominionEventHandlerText.giveSuccess, dominion.getName(), newOwner.getName());
        } catch (Exception e) {
            event.setCancelled(true);
            Notification.error(event.getOperator(), Language.dominionEventHandlerText.giveFailed, dominion.getName(), e.getMessage());
        }
    }

    /**
     * Handles the setting of a teleportation location for a dominion.
     * This method performs various checks such as ownership and location validation.
     * It updates the dominion's teleportation location in the database if all checks pass.
     *
     * @param event the DominionSetTpLocationEvent containing the details of the teleportation location to be set
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionSetTpLocationEvent(DominionSetTpLocationEvent event) {
        if (event.isCancelled()) return;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionOwner(event.getOperator(), dominion);
            DominionDTO d = CacheManager.instance.getCache().getDominionCache().getDominion(event.getNewTpLocation());
            if (d == null || !d.getId().equals(dominion.getId())) {
                throw new DominionException(Language.dominionEventHandlerText.tpLocationNotInDominion, dominion.getName());
            }
            event.setDominion(dominion.setTpLocation(event.getNewTpLocation()));
            Notification.info(event.getOperator(), Language.dominionEventHandlerText.tpLocationSetSuccess, dominion.getName());
        } catch (Exception e) {
            event.setCancelled(true);
            Notification.error(event.getOperator(), Language.dominionEventHandlerText.tpLocationSetFailed, dominion.getName(), e.getMessage());
        }
    }

    /**
     * Handles the setting of a message for a dominion.
     * This method performs various checks such as ownership and message type validation.
     * It updates the dominion's message in the database if all checks pass.
     *
     * @param event the DominionSetMessageEvent containing the details of the message to be set
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionSetMessageEvent(DominionSetMessageEvent event) {
        if (event.isCancelled()) return;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionOwner(event.getOperator(), dominion);
            if (event.getType() == DominionSetMessageEvent.TYPE.ENTER) {
                event.setDominion(dominion.setJoinMessage(event.getNewMessage()));
                Notification.info(event.getOperator(), Language.dominionEventHandlerText.setEnterMessageSuccess, dominion.getName());
            } else {
                event.setDominion(dominion.setLeaveMessage(event.getNewMessage()));
                Notification.info(event.getOperator(), Language.dominionEventHandlerText.setLeaveMessageSuccess, dominion.getName());
            }
        } catch (Exception e) {
            event.setCancelled(true);
            if (event.getType() == DominionSetMessageEvent.TYPE.ENTER) {
                Notification.error(event.getOperator(), Language.dominionEventHandlerText.setEnterMessageFailed, dominion.getName(), e.getMessage());
            } else {
                Notification.error(event.getOperator(), Language.dominionEventHandlerText.setLeaveMessageFailed, dominion.getName(), e.getMessage());
            }
        }
    }

    /**
     * Handles the setting of a map color for a dominion.
     * This method performs various checks such as ownership validation.
     * It updates the dominion's map color in the database if all checks pass.
     *
     * @param event the DominionSetMapColorEvent containing the details of the map color to be set
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionSetMapColorEvent(DominionSetMapColorEvent event) {
        if (event.isCancelled()) return;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionOwner(event.getOperator(), dominion);
            event.setDominion(dominion.setColor(event.getNewColor()));
            Notification.info(event.getOperator(), Language.dominionEventHandlerText.SetMapColorSuccess, dominion.getName());
        } catch (Exception e) {
            event.setCancelled(true);
            Notification.error(event.getOperator(), Language.dominionEventHandlerText.SetMapColorFailed, dominion.getName(), e.getMessage());
        }
    }


    /**
     * Handles the setting of an environmental flag for a dominion.
     * This method performs various checks such as admin validation.
     * It updates the dominion's environmental flag in the database if all checks pass.
     *
     * @param event the DominionSetEnvFlagEvent containing the details of the environmental flag to be set
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionSetEnvFlagEvent(DominionSetEnvFlagEvent event) {
        if (event.isCancelled()) return;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionAdmin(event.getOperator(), dominion);
            event.setDominion(dominion.setEnvFlagValue(event.getFlag(), event.getNewValue()));
            Notification.info(event.getOperator(), Language.dominionEventHandlerText.setEnvFlagSuccess, event.getFlag().getDisplayName(), event.getNewValue());
        } catch (Exception e) {
            event.setCancelled(true);
            Notification.error(event.getOperator(), Language.dominionEventHandlerText.setEnvFlagFailed, event.getFlag().getDisplayName(), event.getNewValue(), e.getMessage());
        }
    }

    /**
     * Handles the setting of a guest flag for a dominion.
     * This method performs various checks such as admin validation.
     * It updates the dominion's guest flag in the database if all checks pass.
     *
     * @param event the DominionSetGuestFlagEvent containing the details of the guest flag to be set
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDominionSetGuestFlagEvent(DominionSetGuestFlagEvent event) {
        if (event.isCancelled()) return;
        DominionDTO dominion = event.getDominion();
        try {
            assertDominionAdmin(event.getOperator(), dominion);
            event.setDominion(dominion.setGuestFlagValue(event.getFlag(), event.getNewValue()));
            Notification.info(event.getOperator(), Language.dominionEventHandlerText.setGuestFlagSuccess, event.getFlag().getDisplayName(), event.getNewValue());
        } catch (Exception e) {
            event.setCancelled(true);
            Notification.error(event.getOperator(), Language.dominionEventHandlerText.setGuestFlagFailed, event.getFlag().getDisplayName(), event.getNewValue(), e.getMessage());
        }
    }

}
