package cn.lunadeer.dominion.v1_20_1.events.environment.Fire;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class BurnEntity implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player) return;
        if (!isHighTemperatureDamage(event.getCause())) {
            return;
        }
        EnvFlag flag = switch (event.getCause()) {
            case LAVA, HOT_FLOOR -> Flags.BURN_ENTITY_LAVA;
            case FIRE, FIRE_TICK -> Flags.BURN_ENTITY_FIRE;
            default -> null;
        };
        if (flag != null) {
            checkEnvironmentFlag(event.getEntity().getLocation(), flag, event);
        }
    }

    private boolean isHighTemperatureDamage(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> true;
            default -> false;
        };
    }
}
