package cn.lunadeer.dominion.v26_3.events.environment.EntityProtection;

import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.events.PaperOnly;
import cn.lunadeer.dominion.utils.XVersionManager;
import cn.lunadeer.dominion.v26_3.protection.CushionPistonProtection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

@PaperOnly
@LowestVersion(XVersionManager.ImplementationVersion.v26_3)
public class CushionPistonExtend implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void handle(BlockPistonExtendEvent event) {
        CushionPistonProtection.check(event, event.getBlocks(), true);
    }
}
