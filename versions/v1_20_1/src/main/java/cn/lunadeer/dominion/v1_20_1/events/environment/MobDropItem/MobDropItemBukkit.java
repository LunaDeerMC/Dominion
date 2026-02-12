package cn.lunadeer.dominion.v1_20_1.events.environment.MobDropItem;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.events.SpigotOnly;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

@SpigotOnly
public class MobDropItemBukkit implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDeathEvent event) {
        Handler(event);
    }

    public static void Handler(EntityDeathEvent event) {
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
