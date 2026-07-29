package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class MonsterSpawn implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Enemy)) {
            return;
        }
        EnvFlag flag = switch (event.getSpawnReason()) {
            case SPAWNER -> Flags.MONSTER_SPAWNER;
            case SPAWNER_EGG -> Flags.MONSTER_SPAWN_EGG;
            default -> Flags.MONSTER_SPAWN;
        };
        checkEnvironmentFlag(entity.getLocation(), flag, event);
    }
}
