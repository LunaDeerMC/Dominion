package cn.lunadeer.dominion.v1_20_1.events.player.Building.PlaceEntity;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class EnderCrystal implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.EnderCrystal crystal)) return;

        // This event precedes both spawning the crystal and attempting dragon respawn.
        checkPrivilegeFlag(crystal.getLocation(), Flags.PLACE_ENDER_CRYSTAL, player, event);
    }
}
