package cn.lunadeer.dominion.v1_20_1.events.environment.EntityProtection;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.v1_20_1.events.environment.Explosions.ExplosionSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class HangingEntityMobDamage implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(HangingBreakByEntityEvent event) {
        if (event.isCancelled() || event.getCause() != HangingBreakEvent.RemoveCause.ENTITY) return;
        Entity remover = event.getRemover();
        if (remover instanceof Player) return;
        if (remover instanceof Projectile projectile && projectile.getShooter() instanceof Player) return;
        if (ExplosionSource.isExplosion(remover)) return;
        checkEnvironmentFlag(event.getEntity().getLocation(), Flags.HANGING_ENTITY_MOB_DAMAGE, event);
    }
}
