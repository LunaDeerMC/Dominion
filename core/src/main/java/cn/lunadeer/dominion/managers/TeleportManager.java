package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.databse.FIelds.Field;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldInteger;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldString;
import cn.lunadeer.dominion.utils.databse.syntax.Delete;
import cn.lunadeer.dominion.utils.databse.syntax.Insert;
import cn.lunadeer.dominion.utils.databse.syntax.Select;
import cn.lunadeer.dominion.utils.scheduler.CancellableTask;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;
import static cn.lunadeer.dominion.utils.Misc.isPaper;
import static cn.lunadeer.dominion.utils.SafeLocationFinder.findNearestSafeLocation;

public class TeleportManager implements Listener {

    public static class TeleportManagerText extends ConfigurationPart {
        public String coolingDown = "Please wait for {0} seconds before teleporting again.";
        public String disabled = "Teleportation is disabled for your permission group.";
        public String delay = "Will teleport in {0} seconds, don't move...";
        public String unfinishedCancelled = "Cancelled previous unfinished teleportation.";
        public String cancelMove = "Cancelled teleportation due to movement.";
    }

    public TeleportManager(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static Map<UUID, Integer> teleportCooldown = new HashMap<>();
    public static Map<UUID, CancellableTask> teleportDelayTasks = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            List<Map<String, Field<?>>> res = Select.select(
                            new FieldInteger("dom_id")
                    ).from("tp_cache")
                    .where("uuid = ?", event.getPlayer().getUniqueId().toString())
                    .execute();
            if (res.isEmpty()) {
                return;
            }
            Integer dominionId = (Integer) res.get(0).get("dom_id").getValue();
            Delete.delete().from("tp_cache").where("uuid = ?", event.getPlayer().getUniqueId().toString()).execute();
            DominionDTO dominion = CacheManager.instance.getDominion(dominionId);
            if (dominion == null) {
                Notification.error(event.getPlayer(), Language.convertsText.unknownDominion, dominionId);
                return;
            }
            if (dominion.getServerId() != Configuration.multiServer.serverId) {
                return;
            }
            doTeleportSafely(event.getPlayer(), dominion.getTpLocation());
        } catch (Exception e) {
            Notification.error(event.getPlayer(), e);
        }

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!teleportDelayTasks.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        CancellableTask task = teleportDelayTasks.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
        Notification.warn(event.getPlayer(), Language.teleportManagerText.cancelMove);
    }

    /**
     * Teleports a player to a specified dominion.
     * <p>
     * This method checks if the player has the privilege to teleport to the dominion.
     * If the dominion is on the same server, it teleports the player safely to the dominion's location.
     * If the dominion is on a different server, it sends a teleport action message to the target server
     * and connects the player to that server.
     *
     * @param player   The player to be teleported.
     * @param dominion The dominion to which the player will be teleported.
     */
    public static void teleportToDominion(Player player, DominionDTO dominion) {
        // check privilege
        if (!Configuration.getPlayerLimitation(player).teleportation.enable &&
                !(player.hasPermission(adminPermission) && Configuration.adminBypass)) {
            Notification.warn(player, Language.teleportManagerText.disabled);
            return;
        }
        // if tp flag is disabled, return too
        if (!Flags.TELEPORT.getEnable()) {
            Notification.warn(player, Language.teleportManagerText.disabled);
            return;
        }
        if (!checkPrivilegeFlag(dominion, Flags.TELEPORT, player, null)) {
            return;
        }
        boolean needCooldown = Configuration.getPlayerLimitation(player).teleportation.cooldown > 0;
        int delaySec = Configuration.getPlayerLimitation(player).teleportation.delay;
        if (player.hasPermission(adminPermission) && Configuration.adminBypass) {
            needCooldown = false;
            delaySec = 0;
        }
        // cooldown
        if (needCooldown) {
            int currentTs = (int) (System.currentTimeMillis() / 1000);
            if (teleportCooldown.containsKey(player.getUniqueId())) {
                if (teleportCooldown.get(player.getUniqueId()) > currentTs) {
                    Notification.warn(player, Language.teleportManagerText.coolingDown, teleportCooldown.get(player.getUniqueId()) - currentTs);
                    return;
                }
            }
            teleportCooldown.put(player.getUniqueId(), currentTs + Configuration.getPlayerLimitation(player).teleportation.cooldown);
        }
        // delay
        if (teleportDelayTasks.containsKey(player.getUniqueId())) {
            teleportDelayTasks.get(player.getUniqueId()).cancel();
            teleportDelayTasks.remove(player.getUniqueId());
            Notification.warn(player, Language.teleportManagerText.unfinishedCancelled);
        }
        if (delaySec > 0) {
            Notification.info(player, Language.teleportManagerText.delay, delaySec);
        }
        // teleport
        CancellableTask task = Scheduler.runTaskLaterAsync(() -> {
            if (dominion.getServerId() == Configuration.multiServer.serverId) {
                doTeleportSafely(player, dominion.getTpLocation());
            } else {
                if (!Configuration.multiServer.enable) return;
                try {
                    FieldString cacheUuid = new FieldString("uuid", player.getUniqueId().toString());
                    FieldInteger cacheDomId = new FieldInteger("dom_id", dominion.getId());
                    Insert.insert().into("tp_cache")
                            .values(cacheUuid, cacheDomId)
                            .onConflict("uuid").doUpdate()
                            .execute();
                    MultiServerManager.instance.connectToServer(player, MultiServerManager.instance.getServerName(dominion.getServerId()));
                } catch (Exception e) {
                    Notification.error(player, e);
                }
            }
        }, delaySec * 20L);
        Scheduler.runTaskLaterAsync(() -> {
            teleportDelayTasks.remove(player.getUniqueId());    // remove task from map for cleanup
        }, delaySec * 20L + 1);
        teleportDelayTasks.put(player.getUniqueId(), task);
    }

    /**
     * Teleports a player to a specified location safely.
     * <p>
     * This method ensures that the player is teleported to a safe location. If the player has passengers,
     * they are removed before teleportation. The method handles both synchronous and asynchronous teleportation
     * based on the server type (Paper or non-Paper).
     *
     * @param player   The player to be teleported.
     * @param location The target location to which the player will be teleported.
     */
    public static void doTeleportSafely(Player player, Location location) {
        if (!player.getPassengers().isEmpty()) {
            player.getPassengers().forEach(player::removePassenger);
        }
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().removePassenger(player);
            player.leaveVehicle();
        }
        if (!isPaper()) {
            Location loc = findNearestSafeLocation(location);
            Bukkit.getScheduler().runTask(Dominion.instance, () -> player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN));
        } else {
            location.getWorld().getChunkAtAsyncUrgently(location).thenAccept((chunk) -> {
                Location loc = findNearestSafeLocation(location);
                player.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            });
        }
    }

}
