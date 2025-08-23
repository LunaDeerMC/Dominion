package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class SnowAccumulation implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockFormEvent event) {
        if (event.isCancelled()) return;
        if (!event.getNewState().getType().name().contains("SNOW")) {
            return;
        }
        checkEnvironmentFlag(event.getBlock().getLocation(), Flags.SNOW_ACCUMULATION, event);
    }
}
