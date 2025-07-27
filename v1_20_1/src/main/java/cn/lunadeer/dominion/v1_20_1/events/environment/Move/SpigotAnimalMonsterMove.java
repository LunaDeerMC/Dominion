package cn.lunadeer.dominion.v1_20_1.events.environment.Move;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.SpigotOnly;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

@SpigotOnly
public class SpigotAnimalMonsterMove implements Listener {
    public SpigotAnimalMonsterMove() {
        Scheduler.runTaskRepeat(() -> {
            Dominion.instance.getServer().getWorlds().forEach(world -> {
                world.getEntities().forEach(entity -> {
                    if (!(entity instanceof LivingEntity)) {
                        return;
                    }
                    if (!entityMap.containsKey(entity.getUniqueId())) {
                        entityMap.put(entity.getUniqueId(), entity.getLocation());
                    } else {
                        Location lastLoc = entityMap.get(entity.getUniqueId());
                        Location currentLoc = entity.getLocation();
                        if (!checkEnvironmentFlag(currentLoc, Flags.ANIMAL_MOVE, null) && entity instanceof Animals) {
                            entity.teleport(lastLoc);
                        } else if (!checkEnvironmentFlag(currentLoc, Flags.MONSTER_MOVE, null) && entity instanceof Monster) {
                            entity.teleport(lastLoc);
                        } else {
                            entityMap.put(entity.getUniqueId(), currentLoc);
                        }
                    }
                });
            });
        }, 20, 30);
        Scheduler.runTaskRepeat(() -> {
            entityMap.forEach((uuid, loc) -> {
                Entity e = Dominion.instance.getServer().getEntity(uuid);
                if (e == null || e.isDead()) {
                    entityMap.remove(uuid);
                }
            });
        }, 20, 6000);
    }

    private static final ConcurrentHashMap<UUID, Location> entityMap = new ConcurrentHashMap<>();
}
