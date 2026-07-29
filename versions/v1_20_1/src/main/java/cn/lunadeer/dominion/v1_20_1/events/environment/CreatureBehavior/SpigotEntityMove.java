package cn.lunadeer.dominion.v1_20_1.events.environment.CreatureBehavior;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public final class SpigotEntityMove {
    private SpigotEntityMove() {
    }

    public static void track(Class<? extends LivingEntity> entityType, EnvFlag flag) {
        ConcurrentHashMap<UUID, Location> locations = new ConcurrentHashMap<>();
        Scheduler.runTaskRepeat(() -> Dominion.instance.getServer().getWorlds().forEach(world ->
                world.getEntitiesByClass(entityType).forEach(entity -> {
                    Location current = entity.getLocation();
                    Location previous = locations.putIfAbsent(entity.getUniqueId(), current);
                    if (previous == null) return;
                    if (!checkEnvironmentFlag(current, flag, null)) {
                        entity.teleport(previous);
                    } else {
                        locations.put(entity.getUniqueId(), current);
                    }
                })), 20, 30);
        Scheduler.runTaskRepeat(() -> locations.keySet().removeIf(SpigotEntityMove::isMissingOrDead), 20, 6000);
    }

    private static boolean isMissingOrDead(UUID uuid) {
        Entity entity = Dominion.instance.getServer().getEntity(uuid);
        return entity == null || entity.isDead();
    }
}
