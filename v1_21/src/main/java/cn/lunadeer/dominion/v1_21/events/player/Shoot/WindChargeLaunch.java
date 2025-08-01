package cn.lunadeer.dominion.v1_21.events.player.Shoot;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

@LowestVersion(XVersionManager.ImplementationVersion.v1_21)
public class WindChargeLaunch implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }
        if (projectile.getType() != EntityType.WIND_CHARGE) {
            return;
        }
        checkPrivilegeFlag(projectile.getLocation(), Flags.SHOOT, player, event);
    }
}
