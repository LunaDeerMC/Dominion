package cn.lunadeer.dominion.v1_20_1.events.environment.PressurePlate;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class ByProjectile implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityInteractEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Projectile)) {
            return;
        }
        Block block = event.getBlock();
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) {
            return;
        }
        checkEnvironmentFlag(block.getLocation(), Flags.TRIG_PRESSURE_PROJ, event);
    }
}
