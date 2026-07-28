package cn.lunadeer.dominion.v1_20_1.events.environment.CreeperExplode;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.FlagClassifiers;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class ItemFrameExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(HangingBreakByEntityEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION) {
            return;
        }
        Entity remover = event.getRemover();
        EnvFlag flag = FlagClassifiers.explosionEntity(remover.getType());
        if (flag == null) return;
        checkEnvironmentFlag(entity.getLocation(), flag, event);
    }
}
