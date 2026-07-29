package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class CreeperExplode implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        if (!ExplosionSource.CREEPER.matches(event.getEntity())) return;
        event.blockList().removeIf(block -> !checkEnvironmentFlag(block.getLocation(), Flags.CREEPER_EXPLODE, null));
    }
}
