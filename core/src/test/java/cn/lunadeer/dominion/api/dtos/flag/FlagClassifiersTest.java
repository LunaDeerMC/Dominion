package cn.lunadeer.dominion.api.dtos.flag;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FlagClassifiersTest {

    @Test
    void dispatchesEveryAnimalSpawnReason() {
        for (CreatureSpawnEvent.SpawnReason reason : CreatureSpawnEvent.SpawnReason.values()) {
            EnvFlag expected = switch (reason) {
                case BREEDING -> Flags.ANIMAL_BREED;
                case SPAWNER -> Flags.ANIMAL_SPAWNER;
                case SPAWNER_EGG -> Flags.ANIMAL_SPAWN_EGG;
                default -> Flags.ANIMAL_SPAWN;
            };
            assertSame(expected, FlagClassifiers.animalSpawn(reason), reason.name());
        }
    }

    @Test
    void dispatchesEveryVillagerSpawnReason() {
        for (CreatureSpawnEvent.SpawnReason reason : CreatureSpawnEvent.SpawnReason.values()) {
            EnvFlag expected = switch (reason) {
                case BREEDING -> Flags.VILLAGER_BREED;
                case SPAWNER -> Flags.VILLAGER_SPAWNER;
                case SPAWNER_EGG -> Flags.VILLAGER_SPAWN_EGG;
                default -> Flags.VILLAGER_SPAWN;
            };
            assertSame(expected, FlagClassifiers.villagerSpawn(reason), reason.name());
        }
    }

    @Test
    void dispatchesEveryMonsterSpawnReason() {
        for (CreatureSpawnEvent.SpawnReason reason : CreatureSpawnEvent.SpawnReason.values()) {
            EnvFlag expected = switch (reason) {
                case SPAWNER -> Flags.MONSTER_SPAWNER;
                case SPAWNER_EGG -> Flags.MONSTER_SPAWN_EGG;
                default -> Flags.MONSTER_SPAWN;
            };
            assertSame(expected, FlagClassifiers.monsterSpawn(reason), reason.name());
        }
    }

    @Test
    void classifiesAllSupportedNonTntExplosionSources() {
        assertExplosion(EntityType.CREEPER, Flags.CREEPER_EXPLODE, Flags.CREEPER_DAMAGE_ENTITY);
        assertExplosion(EntityType.WITHER_SKULL, Flags.WITHER_SKULL_EXPLODE, Flags.WITHER_SKULL_DAMAGE_ENTITY);
        assertExplosion(EntityType.ENDER_CRYSTAL, Flags.ENDER_CRYSTAL_EXPLODE, Flags.ENDER_CRYSTAL_DAMAGE_ENTITY);
        for (EntityType type : EnumSet.of(
                EntityType.FIREBALL,
                EntityType.SMALL_FIREBALL,
                EntityType.DRAGON_FIREBALL
        )) {
            assertExplosion(type, Flags.FIREBALL_EXPLODE, Flags.FIREBALL_DAMAGE_ENTITY);
        }
        assertNull(FlagClassifiers.explosionBlock(EntityType.COW));
        assertNull(FlagClassifiers.explosionEntity(EntityType.COW));
        assertNull(FlagClassifiers.explosionBlock(EntityType.PRIMED_TNT));
        assertNull(FlagClassifiers.explosionEntity(EntityType.PRIMED_TNT));
    }

    private static void assertExplosion(EntityType type, EnvFlag block, EnvFlag entity) {
        assertSame(block, FlagClassifiers.explosionBlock(type), type.name());
        assertSame(entity, FlagClassifiers.explosionEntity(type), type.name());
    }
}
