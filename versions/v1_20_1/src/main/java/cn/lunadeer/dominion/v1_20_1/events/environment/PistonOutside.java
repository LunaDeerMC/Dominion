package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class PistonOutside implements Listener {
    private static final int MAX_INVOLVED_DOMINIONS = 8;

    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockPistonExtendEvent event) {
        if (event.isCancelled()) return;
        checkPistonOutside(event.getBlock(), event.getBlocks(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockPistonRetractEvent event) {
        if (event.isCancelled()) return;
        checkPistonOutside(event.getBlock(), event.getBlocks(), event);
    }

    private void checkPistonOutside(Block piston, Collection<Block> movedBlocks, Cancellable event) {
        List<DominionDTO> involvedDominions = collectInvolvedDominions(piston, movedBlocks);
        if (involvedDominions.size() < 2) return;
        for (DominionDTO dominion : involvedDominions) {
            if (!checkEnvironmentFlag(dominion, Flags.PISTON_OUTSIDE, event)) return;
        }
    }

    private List<DominionDTO> collectInvolvedDominions(Block piston, Collection<Block> movedBlocks) {
        List<DominionDTO> involvedDominions = new ArrayList<>(MAX_INVOLVED_DOMINIONS);
        Set<Integer> involvedDominionIds = new HashSet<>(MAX_INVOLVED_DOMINIONS);

        addDominion(piston, involvedDominions, involvedDominionIds);
        for (Block block : movedBlocks) {
            if (involvedDominions.size() >= MAX_INVOLVED_DOMINIONS) break;
            addDominion(block, involvedDominions, involvedDominionIds);
        }
        return involvedDominions;
    }

    private void addDominion(Block block, List<DominionDTO> involvedDominions, Set<Integer> involvedDominionIds) {
        DominionDTO dominion = CacheManager.instance.getDominion(block.getLocation());
        if (dominion == null || !involvedDominionIds.add(dominion.getId())) return;
        involvedDominions.add(dominion);
    }
}
