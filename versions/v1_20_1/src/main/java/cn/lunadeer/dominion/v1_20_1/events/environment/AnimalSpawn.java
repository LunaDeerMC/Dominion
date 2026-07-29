package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class AnimalSpawn implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Animals) && !(entity instanceof Bat)) {
            return;
        }
        EnvFlag flag = switch (event.getSpawnReason()) {
            case BREEDING -> Flags.ANIMAL_BREED;
            case SPAWNER -> Flags.ANIMAL_SPAWNER;
            case SPAWNER_EGG -> Flags.ANIMAL_SPAWN_EGG;
            default -> Flags.ANIMAL_SPAWN;
        };
        checkEnvironmentFlag(entity.getLocation(), flag, event);
    }
}
