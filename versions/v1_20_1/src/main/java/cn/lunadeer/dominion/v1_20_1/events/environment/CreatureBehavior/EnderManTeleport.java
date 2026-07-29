package cn.lunadeer.dominion.v1_20_1.events.environment.CreatureBehavior;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class EnderManTeleport implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityTeleportEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.ENDERMAN) {
            return;
        }
        checkEnvironmentFlag(entity.getLocation(), Flags.ENDER_MAN_TELEPORT, event);
        if (event.getTo() != null) {
            checkEnvironmentFlag(event.getTo(), Flags.ENDER_MAN_TELEPORT, event);
        }
    }
}
