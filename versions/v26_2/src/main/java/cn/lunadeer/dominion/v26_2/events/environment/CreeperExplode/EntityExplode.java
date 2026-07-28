package cn.lunadeer.dominion.v26_2.events.environment.CreeperExplode;

import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.FlagClassifiers;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

@LowestVersion(XVersionManager.ImplementationVersion.v26_2)
public class EntityExplode implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        EnvFlag flag = FlagClassifiers.explosionBlock(entity.getType());
        if (flag == null) return;
        event.blockList().removeIf(block -> !checkEnvironmentFlag(block.getLocation(), flag, null));
    }
}
