package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.utils.BorderRenderUtil;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.VaultConnect.VaultConnect;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

import static cn.lunadeer.dominion.misc.Others.sortLocations;
import static cn.lunadeer.dominion.utils.BorderRenderUtil.DEFAULT_GLOW_COLOR;


public class SelectPointEventsHandler implements Listener {

    public static class SelectPointEventsHandlerText extends ConfigurationPart {
        public String firstPoint = "First point selected at {0}, {1}, {2}.";
        public String secondPoint = "Second point selected at {0}, {1}, {2}.";
        public String notSameWorld = "The two points are not in the same world.";
        public String price = "The price of the dominion is {0} {1}.";
        public String owner = "Owner: {0}";
        public String size = "Size: {0}x{1}x{2}";
        public String square = "Square: {0}";
        public String volume = "Volume: {0}";

        public String noDominion = "No dominion found at the location {0}, {1}, {2}.";
        public String foundDominion = "Location {0}, {1}, {2} is in dominion {3}.";
    }

    public SelectPointEventsHandler(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void selectPoint(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.matchMaterial(Configuration.selectTool)) {
            return;
        }
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        if (block == null) {
            return;
        }

        Map<Integer, Location> points = Dominion.pointsSelect.get(player.getUniqueId());
        if (points == null) {
            points = new HashMap<>();
        }

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Notification.info(player, Language.selectPointEventsHandlerText.firstPoint, block.getX(), block.getY(), block.getZ());
            Location loc = block.getLocation();
            if (Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).autoIncludeVertical) {
                loc.setY(Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).noLowerThan);
            }
            points.put(0, loc);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            Notification.info(player, Language.selectPointEventsHandlerText.secondPoint, block.getX(), block.getY(), block.getZ());
            Location loc = block.getLocation();
            if (Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).autoIncludeVertical) {
                loc.setY(Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).noHigherThan - 1);
            }
            points.put(1, loc);
        } else {
            return;
        }
        Dominion.pointsSelect.put(player.getUniqueId(), points);

        if (points.size() == 2) {
            World world = points.get(0).getWorld();
            if (world == null) {
                return;
            }
            if (!points.get(0).getWorld().getUID().equals(points.get(1).getWorld().getUID())) {
                Notification.warn(player, Language.selectPointEventsHandlerText.notSameWorld);
                return;
            }
            Location[] locs = sortLocations(points.get(0), points.get(1));
            CuboidDTO cuboid = new CuboidDTO(locs[0], locs[1]);
            try {
                if (Configuration.getPlayerLimitation(player).economy.enable) {
                    long amount;
                    if (Configuration.getPlayerLimitation(player).economy.squareOnly) {
                        amount = cuboid.getSquare();
                    } else {
                        amount = cuboid.getVolume();
                    }
                    double price = amount * Configuration.getPlayerLimitation(player).economy.pricePerBlock;
                    Notification.info(player, Language.selectPointEventsHandlerText.price, price, VaultConnect.instance.currencyNamePlural());
                }
                BorderRenderUtil.showBorder(player, points.get(0).getWorld(), cuboid, DEFAULT_GLOW_COLOR);
                Notification.info(player, Language.selectPointEventsHandlerText.size, cuboid.xLength(), cuboid.yLength(), cuboid.zLength());
                Notification.info(player, Language.selectPointEventsHandlerText.square, cuboid.getSquare());
                Notification.info(player, Language.selectPointEventsHandlerText.volume, cuboid.getVolume());
            } catch (Exception e) {
                Notification.error(player, e.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void selectBlockToShowInfo(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.matchMaterial(Configuration.infoTool)) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        DominionDTO dominion = CacheManager.instance.getDominion(block.getLocation());
        if (dominion == null) {
            Notification.info(player, Language.selectPointEventsHandlerText.noDominion, block.getX(), block.getY(), block.getZ());
        } else {
            Notification.info(player, Language.selectPointEventsHandlerText.foundDominion, block.getX(), block.getY(), block.getZ(), dominion.getName());
            CuboidDTO cuboid = dominion.getCuboid();
            Notification.info(player, Language.selectPointEventsHandlerText.owner, CacheManager.instance.getPlayerName(dominion.getOwner()));
            Notification.info(player, Language.selectPointEventsHandlerText.size, cuboid.xLength(), cuboid.yLength(), cuboid.zLength());
            Notification.info(player, Language.selectPointEventsHandlerText.square, cuboid.getSquare());
            Notification.info(player, Language.selectPointEventsHandlerText.volume, cuboid.getVolume());
            BorderRenderUtil.showBorder(player, dominion);
        }
    }
}
