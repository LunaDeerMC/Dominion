package cn.lunadeer.dominion.v1_21.events.environment.Explosions.TNTDamageEntity;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

@LowestVersion(XVersionManager.ImplementationVersion.v1_21)
public class ArmorStandExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof ArmorStand)) return;
        EntityType source = event.getDamager().getType();
        if (source != EntityType.TNT_MINECART && source != EntityType.TNT) return;
        checkEnvironmentFlag(event.getEntity().getLocation(), Flags.ARMOR_STAND_EXPLOSION_DAMAGE, event);
    }
}
