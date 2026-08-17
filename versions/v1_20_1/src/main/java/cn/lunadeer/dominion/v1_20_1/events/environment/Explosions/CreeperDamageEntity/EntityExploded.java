package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions.CreeperDamageEntity;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.v1_20_1.events.environment.Explosions.ExplosionSource;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class EntityExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity instanceof ArmorStand || entity instanceof Hanging) return;
        if (!ExplosionSource.CREEPER.matches(event.getDamager())) return;
        checkEnvironmentFlag(entity.getLocation(), Flags.CREEPER_DAMAGE_ENTITY, event);
    }
}
