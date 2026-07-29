package cn.lunadeer.dominion.v1_20_1.events.environment.CreatureSpawning;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class VillagerBreed implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BREEDING) return;
        if (event.getEntityType() != EntityType.VILLAGER) return;
        checkEnvironmentFlag(event.getLocation(), Flags.VILLAGER_BREED, event);
    }
}
