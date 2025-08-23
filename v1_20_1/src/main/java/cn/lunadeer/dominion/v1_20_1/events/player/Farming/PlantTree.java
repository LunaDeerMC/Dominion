package cn.lunadeer.dominion.v1_20_1.events.player.Farming;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class PlantTree implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (!Tag.SAPLINGS.isTagged(event.getBlock().getType())) return;
        Player player = event.getPlayer();
        checkPrivilegeFlag(event.getBlock().getLocation(), Flags.SOWING, player, event);
    }
}

