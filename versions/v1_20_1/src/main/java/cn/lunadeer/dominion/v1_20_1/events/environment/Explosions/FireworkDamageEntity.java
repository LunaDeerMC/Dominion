package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class FireworkDamageEntity implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void handle(EntityDamageByEntityEvent event) {
        if (!ExplosionSource.FIREWORK.matches(event.getDamager())) return;
        // Check each victim's territory, including fireworks launched outside it or without a player source.
        checkEnvironmentFlag(event.getEntity().getLocation(), Flags.FIREWORK_DAMAGE_ENTITY, event);
    }
}
