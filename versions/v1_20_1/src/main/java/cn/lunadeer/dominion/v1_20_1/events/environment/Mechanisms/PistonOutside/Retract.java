package cn.lunadeer.dominion.v1_20_1.events.environment.Mechanisms.PistonOutside;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonRetractEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;
import static cn.lunadeer.dominion.misc.Others.isInDominion;

public class Retract implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockPistonRetractEvent event) {
        if (event.isCancelled()) return;
        Block piston = event.getBlock();
        for (Block block : event.getBlocks()) {
            DominionDTO sourceDominion = CacheManager.instance.getDominion(block.getLocation());
            if (sourceDominion == null || isInDominion(sourceDominion, piston.getLocation())) continue;
            if (!checkEnvironmentFlag(sourceDominion, Flags.PISTON_OUTSIDE, event)) return;
        }
    }
}
