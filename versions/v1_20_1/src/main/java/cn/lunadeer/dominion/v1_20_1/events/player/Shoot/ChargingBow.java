package cn.lunadeer.dominion.v1_20_1.events.player.Shoot;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class ChargingBow implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityShootBowEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        checkPrivilegeFlag(player.getLocation(), Flags.SHOOT, player, event);
    }
}
