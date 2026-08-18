package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class BlockExplode implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(BlockExplodeEvent event) {
        if (event.isCancelled()) return;
        EnvFlag flag = event.getBlock().getType() == org.bukkit.Material.RESPAWN_ANCHOR
                ? Flags.ANCHOR_EXPLODE
                : org.bukkit.Tag.BEDS.isTagged(event.getBlock().getType()) ? Flags.BED_EXPLODE : null;
        if (flag == null) return;
        event.blockList().removeIf(blockState -> !checkEnvironmentFlag(blockState.getLocation(), flag, null));
    }
}
