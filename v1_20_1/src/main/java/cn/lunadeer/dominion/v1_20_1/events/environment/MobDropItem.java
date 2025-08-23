package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobDropItem implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDeathEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        DominionDTO dom = CacheManager.instance.getDominion(entity.getLocation());
        if (dom == null) {
            return;
        }
        if (!Flags.MOB_DROP_ITEM.getEnable()) {
            return;
        }
        if (dom.getEnvironmentFlagValue().get(Flags.MOB_DROP_ITEM)) {
            return;
        }
        event.getDrops().clear();
    }
}
