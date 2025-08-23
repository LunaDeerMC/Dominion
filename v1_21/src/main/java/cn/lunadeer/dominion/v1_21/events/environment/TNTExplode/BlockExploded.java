package cn.lunadeer.dominion.v1_21.events.environment.TNTExplode;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

@LowestVersion(XVersionManager.ImplementationVersion.v1_21)
public class BlockExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.TNT_MINECART && entity.getType() != EntityType.TNT) {
            return;
        }
        event.blockList().removeIf(block -> !checkEnvironmentFlag(block.getLocation(), Flags.TNT_EXPLODE, null));
    }
}
