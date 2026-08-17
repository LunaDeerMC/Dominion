package cn.lunadeer.dominion.v1_20_1.events.environment.EntityProtection;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.v1_20_1.events.environment.Explosions.ExplosionSource;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class ArmorStandMobDamage implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof ArmorStand)) return;
        Entity damager = event.getDamager();
        if (damager instanceof Player) return;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player) return;
        if (ExplosionSource.isExplosion(damager)) return;
        checkEnvironmentFlag(event.getEntity().getLocation(), Flags.ARMOR_STAND_MOB_DAMAGE, event);
    }
}
