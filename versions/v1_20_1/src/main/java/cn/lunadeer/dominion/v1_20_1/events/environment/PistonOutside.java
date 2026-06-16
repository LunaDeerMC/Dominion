package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;
import static cn.lunadeer.dominion.misc.Others.isInDominion;

public class PistonOutside implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockPistonExtendEvent event) {
        if (event.isCancelled()) return;

        Block piston = event.getBlock();
        for (Block block : event.getBlocks()) {
            Block targetBlock = block.getRelative(event.getDirection());
            DominionDTO targetDominion = CacheManager.instance.getDominion(targetBlock.getLocation());
            if (!isOutsideAction(piston, targetDominion)) continue;
            if (!checkEnvironmentFlag(targetDominion, Flags.PISTON_OUTSIDE, event)) return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockPistonRetractEvent event) {
        if (event.isCancelled()) return;

        Block piston = event.getBlock();
        for (Block block : event.getBlocks()) {
            DominionDTO sourceDominion = CacheManager.instance.getDominion(block.getLocation());
            if (!isOutsideAction(piston, sourceDominion)) continue;
            if (!checkEnvironmentFlag(sourceDominion, Flags.PISTON_OUTSIDE, event)) return;
        }
    }

    private boolean isOutsideAction(Block piston, DominionDTO protectedDominion) {
        return protectedDominion != null && !isInDominion(protectedDominion, piston.getLocation());
    }
}
