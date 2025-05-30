package cn.lunadeer.dominion.v1_20_1.events.environment.CreeperExplode;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;
import static cn.lunadeer.dominion.misc.Others.isExplodeEntity;

public class EntityExplode implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (!isExplodeEntity(entity)) {
            return;
        }
        event.blockList().removeIf(block -> {
            DominionDTO dom = CacheManager.instance.getDominion(block.getLocation());
            return !checkEnvironmentFlag(dom, Flags.CREEPER_EXPLODE, null);
        });
    }
}
