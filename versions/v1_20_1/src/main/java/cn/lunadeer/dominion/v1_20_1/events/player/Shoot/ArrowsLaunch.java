package cn.lunadeer.dominion.v1_20_1.events.player.Shoot;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class ArrowsLaunch implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(ProjectileLaunchEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        checkPrivilegeFlag(player.getLocation(), Flags.SHOOT, player, event);
    }
}
