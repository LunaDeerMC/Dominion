package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class PickUp implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerAttemptPickupItemEvent event) {
        if (event.getItem().getItemStack().getType().isAir()) {
            return;
        }
        checkPrivilegeFlag(event.getPlayer().getLocation(), Flags.PICK_UP, event.getPlayer(), event);
    }
}
