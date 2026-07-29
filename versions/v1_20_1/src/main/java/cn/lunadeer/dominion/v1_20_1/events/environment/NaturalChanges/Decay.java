package cn.lunadeer.dominion.v1_20_1.events.environment.NaturalChanges;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class Decay implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(LeavesDecayEvent event) {
        if (event.isCancelled()) return;
        checkEnvironmentFlag(event.getBlock().getLocation(), Flags.DECAY, event);
    }
}
