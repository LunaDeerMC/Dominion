package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class BlockExplode implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(BlockExplodeEvent event) {
        if (event.isCancelled()) return;
        BlockState explodedState;
        try {
            // The live source block has already been removed when this event fires.
            explodedState = event.getExplodedBlockState();
        } catch (NoSuchMethodError ignored) {
            // Older Spigot APIs cannot distinguish beds from anchors. Preserve protection
            // unless both kinds of block explosion are allowed at each affected location.
            event.blockList().removeIf(block ->
                    !checkEnvironmentFlag(block.getLocation(), Flags.BED_EXPLODE, null)
                            || !checkEnvironmentFlag(block.getLocation(), Flags.ANCHOR_EXPLODE, null));
            return;
        }
        if (explodedState == null) return;
        Material source = explodedState.getType();
        EnvFlag flag = source == Material.RESPAWN_ANCHOR
                ? Flags.ANCHOR_EXPLODE
                : Tag.BEDS.isTagged(source) ? Flags.BED_EXPLODE : null;
        if (flag == null) return;
        event.blockList().removeIf(block -> !checkEnvironmentFlag(block.getLocation(), flag, null));
    }
}
