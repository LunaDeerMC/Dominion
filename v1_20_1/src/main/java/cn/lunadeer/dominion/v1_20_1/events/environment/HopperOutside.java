package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class HopperOutside implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(InventoryMoveItemEvent event) {
        Inventory hopper = event.getDestination();
        Inventory inventory = event.getSource();
        if (hopper.getLocation() == null || inventory.getLocation() == null) {
            return;
        }
        DominionDTO hopperDom = CacheManager.instance.getDominion(hopper.getLocation());
        DominionDTO inventoryDom = CacheManager.instance.getDominion(inventory.getLocation());
        if (hopperDom == null && inventoryDom != null) {
            checkEnvironmentFlag(inventory.getLocation(), Flags.HOPPER_OUTSIDE, event);
        }
        if (hopperDom != null && inventoryDom != null) {
            if (!hopperDom.getId().equals(inventoryDom.getId())) {
                checkEnvironmentFlag(inventory.getLocation(), Flags.HOPPER_OUTSIDE, event);
            }
        }
    }
}
