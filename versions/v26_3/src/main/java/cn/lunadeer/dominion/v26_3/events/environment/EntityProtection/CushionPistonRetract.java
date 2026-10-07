package cn.lunadeer.dominion.v26_3.events.environment.EntityProtection;

import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.events.PaperOnly;
import cn.lunadeer.dominion.utils.XVersionManager;
import cn.lunadeer.dominion.v26_3.protection.CushionPistonProtection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonRetractEvent;

@PaperOnly
@LowestVersion(XVersionManager.ImplementationVersion.v26_3)
public class CushionPistonRetract implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void handle(BlockPistonRetractEvent event) {
        CushionPistonProtection.check(event, event.getBlocks(), false);
    }
}
