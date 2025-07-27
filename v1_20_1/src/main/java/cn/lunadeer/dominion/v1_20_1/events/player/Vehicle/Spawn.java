package cn.lunadeer.dominion.v1_20_1.events.player.Vehicle;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Spawn implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Vehicle)) {
            return;
        }
        checkPrivilegeFlag(entity.getLocation(), Flags.VEHICLE_SPAWN, player, event);
    }
}
