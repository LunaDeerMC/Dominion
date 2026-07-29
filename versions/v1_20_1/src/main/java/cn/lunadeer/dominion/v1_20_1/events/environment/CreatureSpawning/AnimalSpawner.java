package cn.lunadeer.dominion.v1_20_1.events.environment.CreatureSpawning;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class AnimalSpawner implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Animals) && !(entity instanceof Bat)) return;
        checkEnvironmentFlag(entity.getLocation(), Flags.ANIMAL_SPAWNER, event);
    }
}
