package cn.lunadeer.dominion.v1_20_1.events.environment.MobDropItem;

import cn.lunadeer.dominion.events.PaperOnly;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

@PaperOnly
public class MobDropItemPaper implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDeathEvent event) {
        if (event.isCancelled()) return;
        MobDropItemBukkit.Handler(event);
    }
}
