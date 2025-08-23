package cn.lunadeer.dominion.v1_20_1.events.player.Break;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class ItemFrameShot implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(ProjectileHitEvent event) {
        if (event.isCancelled()) return;
        Entity hit = event.getHitEntity();
        if (hit == null) {
            return;
        }
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }
        if (!(hit instanceof Hanging)) {
            return;
        }
        checkPrivilegeFlag(hit.getLocation(), Flags.BREAK_BLOCK, player, event);
    }
}
