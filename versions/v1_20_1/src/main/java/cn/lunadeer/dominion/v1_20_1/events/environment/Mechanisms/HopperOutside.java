package cn.lunadeer.dominion.v1_20_1.events.environment.Mechanisms;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class HopperOutside implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(InventoryMoveItemEvent event) {
        if (event.isCancelled()) return;
        Inventory hopper = event.getDestination();
        Inventory inventory = event.getSource();
        Location hopperLocation = hopper.getLocation();
        Location inventoryLocation = inventory.getLocation();
        if (hopperLocation == null || inventoryLocation == null) {
            return;
        }

        DominionDTO inventoryDom = CacheManager.instance.getDominion(
                inventoryLocation.getWorld(),
                inventoryLocation.getBlockX(),
                inventoryLocation.getBlockY(),
                inventoryLocation.getBlockZ()
        );
        if (inventoryDom == null) {
            return;
        }

        DominionDTO hopperDom = CacheManager.instance.getDominion(
                hopperLocation.getWorld(),
                hopperLocation.getBlockX(),
                hopperLocation.getBlockY(),
                hopperLocation.getBlockZ()
        );
        if (hopperDom == null || !hopperDom.getId().equals(inventoryDom.getId())) {
            checkEnvironmentFlag(inventoryDom, Flags.HOPPER_OUTSIDE, event);
        }
    }
}
