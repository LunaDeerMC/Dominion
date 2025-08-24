package cn.lunadeer.dominion.v1_20_1.events.player.Shoot;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class ChargingCrossBow implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityLoadCrossbowEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        checkPrivilegeFlag(player.getLocation(), Flags.SHOOT, player, event);
    }
}
