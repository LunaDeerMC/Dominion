package cn.lunadeer.dominion.v1_21.events.player.Shoot;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

@LowestVersion(XVersionManager.ImplementationVersion.v1_21)
public class WindChargeExplode implements Listener {

    // 2025-08-11: the ExplosionPrimeEvent fired when Wind Charge explodes ONLY be implemented by paper (and forks).
    // So this feature only works on paper servers.

    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(ExplosionPrimeEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.WIND_CHARGE) {
            return;
        }
        Projectile projectile = (Projectile) entity;
        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }
        if (!checkPrivilegeFlag(projectile.getLocation(), Flags.SHOOT, player, event)) {
            projectile.remove();
        }
    }
}
