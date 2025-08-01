package cn.lunadeer.dominion.v1_20_1.events.environment.Trample;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;
import static org.bukkit.Material.FARMLAND;

public class ByMob implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityInteractEvent event) {
        Block block = event.getBlock();
        if (block.getType() != FARMLAND) {
            return;
        }
        checkEnvironmentFlag(block.getLocation(), Flags.TRAMPLE, event);
    }
}
