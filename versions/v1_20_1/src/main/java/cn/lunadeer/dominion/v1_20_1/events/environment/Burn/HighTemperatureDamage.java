package cn.lunadeer.dominion.v1_20_1.events.environment.Burn;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class HighTemperatureDamage implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (!isHighTemperatureDamage(event.getCause())) {
            return;
        }
        checkEnvironmentFlag(event.getEntity().getLocation(), Flags.BURN, event);
    }

    private boolean isHighTemperatureDamage(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> true;
            default -> false;
        };
    }
}