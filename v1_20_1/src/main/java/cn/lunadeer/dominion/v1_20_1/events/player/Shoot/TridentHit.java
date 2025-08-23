package cn.lunadeer.dominion.v1_20_1.events.player.Shoot;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class TridentHit implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(ProjectileHitEvent event) {
        if (event.isCancelled()) return;
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }
        if (projectile.getType() != EntityType.TRIDENT) {
            return;
        }
        if (!checkPrivilegeFlag(projectile.getLocation(), Flags.SHOOT, player, event)) {
            projectile.remove();
        }
    }
}
