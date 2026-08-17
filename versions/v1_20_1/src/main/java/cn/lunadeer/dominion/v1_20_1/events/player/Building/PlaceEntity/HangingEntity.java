package cn.lunadeer.dominion.v1_20_1.events.player.Building.PlaceEntity;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingPlaceEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class HangingEntity implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(HangingPlaceEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Hanging)) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        checkPrivilegeFlag(entity.getLocation(), Flags.PLACE_HANGING_ENTITY, player, event);
    }
}
