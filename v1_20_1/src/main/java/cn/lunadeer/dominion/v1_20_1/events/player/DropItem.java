package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class DropItem implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;
        if (event.getItemDrop().getItemStack().getType().isAir()) {
            return;
        }
        checkPrivilegeFlag(event.getPlayer().getLocation(), Flags.DROP_ITEM, event.getPlayer(), event);
    }
}
