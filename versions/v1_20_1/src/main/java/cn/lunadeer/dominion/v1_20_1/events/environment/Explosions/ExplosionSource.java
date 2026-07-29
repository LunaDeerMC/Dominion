package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.WitherSkull;

public enum ExplosionSource {
    CREEPER(Creeper.class),
    WITHER_SKULL(WitherSkull.class),
    ENDER_CRYSTAL(EnderCrystal.class),
    FIREBALL(Fireball.class);

    private final Class<? extends Entity> type;

    ExplosionSource(Class<? extends Entity> type) {
        this.type = type;
    }

    public boolean matches(Entity entity) {
        return type.isInstance(entity);
    }
}
