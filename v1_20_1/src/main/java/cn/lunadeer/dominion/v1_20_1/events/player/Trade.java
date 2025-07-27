package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Trade implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != EntityType.VILLAGER &&
                event.getRightClicked().getType() != EntityType.WANDERING_TRADER) {
            return;
        }
        checkPrivilegeFlag(event.getRightClicked().getLocation(), Flags.TRADE, event.getPlayer(), event);
    }
}
