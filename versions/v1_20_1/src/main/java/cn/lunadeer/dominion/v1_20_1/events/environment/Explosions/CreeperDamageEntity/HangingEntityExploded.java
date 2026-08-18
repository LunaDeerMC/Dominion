package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions.CreeperDamageEntity;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.v1_20_1.events.environment.Explosions.ExplosionSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class HangingEntityExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(HangingBreakByEntityEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION) return;
        if (!ExplosionSource.CREEPER.matches(event.getRemover())) return;
        checkEnvironmentFlag(event.getEntity().getLocation(), Flags.CREEPER_DAMAGE_HANGING_ENTITY, event);
    }
}
