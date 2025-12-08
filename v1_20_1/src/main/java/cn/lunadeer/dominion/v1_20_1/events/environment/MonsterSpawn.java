package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.uis.dominion.manage.MonsterSpawnSettings;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Set;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class MonsterSpawn implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Enemy)) {
            return;
        }

        // Get the dominion at the entity's location
        DominionDTO dominion = DominionAPI.getInstance().getDominion(entity.getLocation());
        if (dominion == null) {
            return;
        }

        // Check if monster_spawn flag is enabled for this dominion
        if (dominion.getEnvFlagValue(Flags.MONSTER_SPAWN)) {
            return;
        }

        // Get the blocked spawn reasons for this dominion
        Set<CreatureSpawnEvent.SpawnReason> blockedReasons = MonsterSpawnSettings.getBlockedReasons(dominion);

        // Check if this spawn reason should be blocked
        if (blockedReasons.contains(event.getSpawnReason())) {
            event.setCancelled(true);
        }
    }
}