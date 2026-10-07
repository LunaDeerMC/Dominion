package cn.lunadeer.dominion.v26_3.protection;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import io.papermc.paper.event.entity.EntityBreakEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

/** Classifies the origin, not the projectile itself. Known explosions take precedence. */
public record CushionBreakSource(Flag flag, Player player) {
    public static CushionBreakSource from(EntityBreakEvent event) {
        if (!(event instanceof EntityBreakByEntityEvent damage)) {
            return environment(Flags.CUSHION_ENVIRONMENT_BREAK);
        }
        Entity direct = damage.getRemover();
        if (event.getCause() == EntityBreakEvent.RemoveCause.EXPLOSION) {
            EnvFlag explosion = explosionFlag(direct);
            if (explosion == null) explosion = explosionFlag(damage.getDamageSource().getDirectEntity());
            if (explosion == null) explosion = explosionFlag(damage.getDamageSource().getCausingEntity());
            return environment(explosion == null ? Flags.CUSHION_ENVIRONMENT_BREAK : explosion);
        }
        Object source = direct instanceof Projectile projectile ? projectile.getShooter() : direct;
        // DamageSource can retain the responsible entity when the direct projectile is unavailable.
        if (source == null) source = damage.getDamageSource().getCausingEntity();
        if (source instanceof Player player) return new CushionBreakSource(Flags.CUSHION_BREAK, player);
        if (source instanceof LivingEntity) return environment(Flags.CUSHION_MOB_DAMAGE);
        return environment(Flags.CUSHION_ENVIRONMENT_BREAK);
    }

    private static CushionBreakSource environment(EnvFlag flag) {
        return new CushionBreakSource(flag, null);
    }

    private static EnvFlag explosionFlag(Entity entity) {
        if (entity == null) return null;
        return switch (entity.getType().name()) {
            case "TNT", "TNT_MINECART" -> Flags.TNT_DAMAGE_ENTITY;
            case "CREEPER", "SULFUR_CUBE" -> Flags.CREEPER_DAMAGE_ENTITY;
            case "WITHER_SKULL" -> Flags.WITHER_SKULL_DAMAGE_ENTITY;
            case "END_CRYSTAL" -> Flags.ENDER_CRYSTAL_DAMAGE_ENTITY;
            case "FIREBALL", "SMALL_FIREBALL", "DRAGON_FIREBALL" -> Flags.FIREBALL_DAMAGE_ENTITY;
            case "FIREWORK_ROCKET" -> Flags.FIREWORK_DAMAGE_ENTITY;
            default -> null;
        };
    }
}
