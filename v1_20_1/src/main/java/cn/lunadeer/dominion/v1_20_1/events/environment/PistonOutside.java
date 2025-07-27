package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class PistonOutside implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        DominionDTO pistonDom = CacheManager.instance.getDominion(piston.getLocation());
        BlockFace direction = event.getDirection();
        Block endBlockAfterPush = piston.getRelative(direction, event.getBlocks().size() + 1);
        DominionDTO endBlockDom = CacheManager.instance.getDominion(endBlockAfterPush.getLocation());
        if (pistonDom != null && endBlockDom == null) {
            checkEnvironmentFlag(piston.getLocation(), Flags.PISTON_OUTSIDE, event);
        }
        if (pistonDom == null && endBlockDom != null) {
            checkEnvironmentFlag(endBlockAfterPush.getLocation(), Flags.PISTON_OUTSIDE, event);
        }
        if (pistonDom != null && endBlockDom != null) {
            if (!pistonDom.getId().equals(endBlockDom.getId())) {
                if (!endBlockDom.getEnvironmentFlagValue().get(Flags.PISTON_OUTSIDE) || !pistonDom.getEnvironmentFlagValue().get(Flags.PISTON_OUTSIDE)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
