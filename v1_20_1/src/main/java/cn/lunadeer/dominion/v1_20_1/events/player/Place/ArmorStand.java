package cn.lunadeer.dominion.v1_20_1.events.player.Place;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class ArmorStand implements Listener {
    @EventHandler(priority = EventPriority.LOWEST) // place - armor stand
    public void handler(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof org.bukkit.entity.ArmorStand)) {
            return;
        }
        checkPrivilegeFlag(entity.getLocation(), Flags.PLACE, player, event);
    }
}
