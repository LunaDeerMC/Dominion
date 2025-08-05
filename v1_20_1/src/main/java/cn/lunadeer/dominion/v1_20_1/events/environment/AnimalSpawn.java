package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.SpawnChangeEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class AnimalSpawn implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(CreatureSpawnEvent event) {
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Animals)) {
            return;
        }
        checkEnvironmentFlag(entity.getLocation(), Flags.ANIMAL_SPAWN, event);
    }
}
