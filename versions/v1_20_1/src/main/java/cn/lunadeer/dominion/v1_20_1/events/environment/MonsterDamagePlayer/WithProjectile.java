package cn.lunadeer.dominion.v1_20_1.events.environment.MonsterDamagePlayer;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class WithProjectile implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(ProjectileHitEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity().getShooter() == null || event.getEntity().getShooter() == null) {
            return;
        }
        if (!(event.getEntity().getShooter() instanceof Enemy)) {
            return;
        }
        if (!(event.getHitEntity() instanceof Player player)) {
            return;
        }
        if (!checkEnvironmentFlag(player.getLocation(), Flags.MONSTER_DAMAGE, event)) {
            event.getEntity().remove();
        }
    }
}
