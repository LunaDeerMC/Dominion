package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions.TNTDamageEntity;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.HighestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

@HighestVersion(XVersionManager.ImplementationVersion.v1_20_1)
public class ArmorStandExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof ArmorStand)) return;
        EntityType source = event.getDamager().getType();
        if (source != EntityType.MINECART_TNT && source != EntityType.PRIMED_TNT) return;
        checkEnvironmentFlag(event.getEntity().getLocation(), Flags.TNT_DAMAGE_ARMOR_STAND, event);
    }
}
