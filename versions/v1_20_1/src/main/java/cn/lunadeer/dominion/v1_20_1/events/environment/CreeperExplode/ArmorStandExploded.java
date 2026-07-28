package cn.lunadeer.dominion.v1_20_1.events.environment.CreeperExplode;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.FlagClassifiers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class ArmorStandExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.ARMOR_STAND) {
            return;
        }
        EnvFlag flag = FlagClassifiers.explosionEntity(event.getDamager().getType());
        if (flag == null) return;
        checkEnvironmentFlag(entity.getLocation(), flag, event);
    }
}
