package cn.lunadeer.dominion.v1_20_1.events.environment.CreatureBehavior;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class EnderMan implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityChangeBlockEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity().getType() != EntityType.ENDERMAN) {
            return;
        }
        EnvFlag flag = event.getTo().isAir() ? Flags.ENDER_MAN_PICKUP_BLOCK : Flags.ENDER_MAN_PLACE_BLOCK;
        checkEnvironmentFlag(event.getBlock().getLocation(), flag, event);
    }
}
