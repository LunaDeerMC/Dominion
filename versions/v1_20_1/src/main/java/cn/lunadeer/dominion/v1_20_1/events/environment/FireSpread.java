package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class FireSpread implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockIgniteEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (player != null) {
            return;
        }
        checkEnvironmentFlag(event.getBlock().getLocation(), Flags.FIRE_SPREAD, event);
    }
}
