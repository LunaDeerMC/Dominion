package cn.lunadeer.dominion.v26_3.protection;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import io.papermc.paper.event.entity.EntityBreakEvent;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.projectiles.BlockProjectileSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CushionBreakSourceTest {
    private EntityBreakByEntityEvent damage(Entity remover, EntityBreakEvent.RemoveCause cause) {
        return new EntityBreakByEntityEvent(mock(Cushion.class), remover, mock(DamageSource.class), cause);
    }

    @Test void playerAndPlayerProjectileShareBreakFlag() {
        Player player = mock(Player.class);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        for (Entity source : new Entity[]{player, arrow}) {
            var decision = CushionBreakSource.from(damage(source, EntityBreakEvent.RemoveCause.ENTITY));
            assertSame(Flags.CUSHION_BREAK, decision.flag());
            assertSame(player, decision.player());
        }
    }

    @Test void mobsAndTheirProjectilesShareMobFlag() {
        Skeleton skeleton = mock(Skeleton.class);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(skeleton);
        for (Entity source : new Entity[]{skeleton, arrow}) {
            var decision = CushionBreakSource.from(damage(source, EntityBreakEvent.RemoveCause.ENTITY));
            assertSame(Flags.CUSHION_MOB_DAMAGE, decision.flag());
            assertNull(decision.player());
        }
    }

    @Test void dispenserUnknownProjectileAndPhysicsAreEnvironment() {
        Arrow dispenser = mock(Arrow.class);
        when(dispenser.getShooter()).thenReturn(mock(BlockProjectileSource.class));
        for (Entity source : new Entity[]{dispenser, mock(Arrow.class), null}) {
            assertSame(Flags.CUSHION_ENVIRONMENT_BREAK,
                    CushionBreakSource.from(damage(source, EntityBreakEvent.RemoveCause.ENTITY)).flag());
        }
        for (var cause : new EntityBreakEvent.RemoveCause[]{EntityBreakEvent.RemoveCause.PHYSICS,
                EntityBreakEvent.RemoveCause.OBSTRUCTION, EntityBreakEvent.RemoveCause.DEFAULT}) {
            assertSame(Flags.CUSHION_ENVIRONMENT_BREAK,
                    CushionBreakSource.from(new EntityBreakEvent(mock(Cushion.class), cause)).flag());
        }
    }

    @Test void explosionUsesExistingFlagEvenWhenProjectileWasFiredByPlayer() {
        Fireball fireball = mock(Fireball.class);
        when(fireball.getType()).thenReturn(EntityType.FIREBALL);
        when(fireball.getShooter()).thenReturn(mock(Player.class));
        var decision = CushionBreakSource.from(damage(fireball, EntityBreakEvent.RemoveCause.EXPLOSION));
        assertSame(Flags.FIREBALL_DAMAGE_ENTITY, decision.flag());
        assertNull(decision.player());
        TNTPrimed tnt = mock(TNTPrimed.class);
        when(tnt.getType()).thenReturn(EntityType.TNT);
        assertSame(Flags.TNT_DAMAGE_ENTITY,
                CushionBreakSource.from(damage(tnt, EntityBreakEvent.RemoveCause.EXPLOSION)).flag());
    }

    @Test void damageSourceRetainsResponsiblePlayerWhenDirectEntityIsMissing() {
        var event = damage(null, EntityBreakEvent.RemoveCause.ENTITY);
        Player player = mock(Player.class);
        when(event.getDamageSource().getCausingEntity()).thenReturn(player);
        assertSame(player, CushionBreakSource.from(event).player());
    }
}
