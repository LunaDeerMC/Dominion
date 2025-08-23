package cn.lunadeer.dominion.v1_20_1.events.environment.Wither;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class BreakBlockOnHarmed implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityChangeBlockEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity().getType() != org.bukkit.entity.EntityType.WITHER) {
            return;
        }
        checkEnvironmentFlag(event.getBlock().getLocation(), Flags.WITHER_SPAWN, event);
    }
}
