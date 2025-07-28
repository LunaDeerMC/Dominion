package cn.lunadeer.dominion.v1_20_1.events.player.Farming;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Fertilizer implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockFertilizeEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        checkPrivilegeFlag(event.getBlock().getLocation(), Flags.SOWING, player, event);
    }
}
