package cn.lunadeer.dominion.v1_20_1.events.environment.EnderMan;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class Escape implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityTeleportEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.ENDERMAN) {
            return;
        }
        checkEnvironmentFlag(entity.getLocation(), Flags.ENDER_MAN, event);
        if (event.getTo() != null) {
            checkEnvironmentFlag(event.getTo(), Flags.ENDER_MAN, event);
        }
    }
}
