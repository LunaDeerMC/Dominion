package cn.lunadeer.dominion.v1_20_1.events.player.Combat.PVP;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Spider;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PotionProtectionTest {
    enum Delivery { SPLASH, CLOUD }

    private Player attacker;
    private MockedStatic<Others> others;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        attacker = mock(Player.class);
        others = mockStatic(Others.class);
        bukkit = mockStatic(Bukkit.class);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        others.close();
    }

    @ParameterizedTest
    @EnumSource(Delivery.class)
    void filtersEachTargetByItsOwnLocationAndExistingFlag(Delivery delivery) {
        Cow deniedCow = target(Cow.class, 1);
        Villager deniedVillager = target(Villager.class, 2);
        Spider deniedMonster = target(Spider.class, 3);
        Cow allowedCow = target(Cow.class, -1);
        Villager allowedVillager = target(Villager.class, -2);
        Spider allowedMonster = target(Spider.class, -3);
        allow(allowedCow, Flags.ANIMAL_KILLING, attacker, true);
        allow(allowedVillager, Flags.VILLAGER_KILLING, attacker, true);
        allow(allowedMonster, Flags.MONSTER_KILLING, attacker, true);
        Exposure exposure = exposure(delivery, attacker, List.of(effect("poison")),
                deniedCow, allowedCow, deniedVillager, allowedVillager, deniedMonster, allowedMonster);

        exposure.apply();

        exposure.assertAffected(allowedCow, allowedVillager, allowedMonster);
        verifyCheck(deniedCow, Flags.ANIMAL_KILLING, attacker);
        verifyCheck(allowedCow, Flags.ANIMAL_KILLING, attacker);
        verifyCheck(deniedVillager, Flags.VILLAGER_KILLING, attacker);
        verifyCheck(allowedVillager, Flags.VILLAGER_KILLING, attacker);
        verifyCheck(deniedMonster, Flags.MONSTER_KILLING, attacker);
        verifyCheck(allowedMonster, Flags.MONSTER_KILLING, attacker);
        others.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @MethodSource("harmfulEffects")
    void blocksImmediateDelayedAndIndirectHarm(Delivery delivery, String effect) {
        Cow victim = target(Cow.class, 1);
        Exposure exposure = exposure(delivery, attacker, List.of(effect(effect)), victim);

        exposure.apply();

        exposure.assertAffected();
        verifyCheck(victim, Flags.ANIMAL_KILLING, attacker);
    }

    static Stream<Arguments> harmfulEffects() {
        return effects("instant_damage", "poison", "wither", "slowness", "weakness", "levitation",
                "infested", "oozing", "weaving", "wind_charged", "unknown_future_effect");
    }

    @ParameterizedTest
    @MethodSource("beneficialEffects")
    void preservesBeneficialEffectsWithoutKillingPermission(Delivery delivery, String effect) {
        Cow animal = target(Cow.class, 1);
        Villager villager = target(Villager.class, 2);
        Spider monster = target(Spider.class, 3);
        Exposure exposure = exposure(delivery, attacker, List.of(effect(effect)), animal, villager, monster);

        exposure.apply();

        exposure.assertAffected(animal, villager, monster);
        others.verifyNoInteractions();
    }

    static Stream<Arguments> beneficialEffects() {
        return effects("instant_health", "speed", "haste", "strength", "jump_boost", "regeneration",
                "resistance", "fire_resistance", "water_breathing", "invisibility", "night_vision",
                "health_boost", "absorption", "saturation", "luck", "slow_falling", "conduit_power",
                "dolphins_grace");
    }

    private static Stream<Arguments> effects(String... keys) {
        return Arrays.stream(Delivery.values()).flatMap(delivery ->
                Arrays.stream(keys).map(key -> Arguments.of(delivery, key)));
    }

    @ParameterizedTest
    @EnumSource(Delivery.class)
    void checksEveryEffectOfAMixedPotion(Delivery delivery) {
        Cow victim = target(Cow.class, 1);
        Exposure exposure = exposure(delivery, attacker, List.of(effect("speed"), effect("poison")), victim);

        exposure.apply();

        exposure.assertAffected();
    }

    @ParameterizedTest
    @EnumSource(Delivery.class)
    void allowsEffectlessPotionsAndEntitiesOutsideExistingFlagCategories(Delivery delivery) {
        Cow cow = target(Cow.class, 1);
        Exposure empty = exposure(delivery, attacker, List.of(), cow);
        LivingEntity other = target(LivingEntity.class, 2);
        Exposure uncategorized = exposure(delivery, attacker, List.of(effect("poison")), other);

        empty.apply();
        uncategorized.apply();

        empty.assertAffected(cow);
        uncategorized.assertAffected(other);
        others.verifyNoInteractions();
    }

    @ParameterizedTest
    @EnumSource(Delivery.class)
    void reversesHealingAndHarmingForUndeadAnimalsAndMonstersOnLegacyServers(Delivery delivery) {
        Zombie zombie = target(Zombie.class, 1);
        SkeletonHorse horse = target(SkeletonHorse.class, 2);
        when(zombie.getCategory()).thenReturn(EntityCategory.UNDEAD);
        when(horse.getCategory()).thenReturn(EntityCategory.UNDEAD);
        Exposure healing = exposure(delivery, attacker, List.of(effect("instant_health")), zombie, horse);
        Exposure harming = exposure(delivery, attacker, List.of(effect("instant_damage")), zombie, horse);

        healing.apply();
        harming.apply();

        healing.assertAffected();
        harming.assertAffected(zombie, horse);
        verifyCheck(zombie, Flags.MONSTER_KILLING, attacker);
        verifyCheck(horse, Flags.ANIMAL_KILLING, attacker);
        others.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @EnumSource(Delivery.class)
    void usesTheInvertedHealingTagWhenModernServersNoLongerSupportCategories(Delivery delivery) {
        Zombie zombie = target(Zombie.class, 1);
        Cow cow = target(Cow.class, 2);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        when(cow.getType()).thenReturn(EntityType.COW);
        when(zombie.getCategory()).thenThrow(new UnsupportedOperationException());
        when(cow.getCategory()).thenThrow(new UnsupportedOperationException());
        Tag<EntityType> inverted = new Tag<>() {
            @Override
            public boolean isTagged(EntityType type) {
                return getValues().contains(type);
            }

            @Override
            public Set<EntityType> getValues() {
                return Set.of(EntityType.ZOMBIE);
            }

            @Override
            public NamespacedKey getKey() {
                return NamespacedKey.minecraft("inverted_healing_and_harm");
            }
        };
        bukkit.when(() -> Bukkit.getTag("entity_types", inverted.getKey(), EntityType.class)).thenReturn(inverted);
        Exposure healing = exposure(delivery, attacker, List.of(effect("instant_health")), zombie, cow);
        Exposure harming = exposure(delivery, attacker, List.of(effect("instant_damage")), zombie, cow);

        healing.apply();
        harming.apply();

        healing.assertAffected(cow);
        harming.assertAffected(zombie);
        verify(zombie, never()).getCategory();
        verify(cow, never()).getCategory();
    }

    @ParameterizedTest
    @MethodSource("pvpPermissions")
    void preservesBothPlayersPvpChecksAtTheVictimsLocationAndAllowsSelfEffects(
            Delivery delivery, boolean attackerAllowed, boolean victimAllowed) {
        Player victim = target(Player.class, 1);
        allow(victim, Flags.PVP, attacker, attackerAllowed);
        others.when(() -> Others.checkPrivilegeFlagSilence(victim.getLocation(), Flags.PVP, victim, null))
                .thenReturn(victimAllowed);
        Exposure exposure = exposure(delivery, attacker, List.of(effect("speed")), attacker, victim);

        exposure.apply();

        if (attackerAllowed && victimAllowed) exposure.assertAffected(attacker, victim);
        else exposure.assertAffected(attacker);
        verifyCheck(victim, Flags.PVP, attacker);
        if (attackerAllowed) {
            others.verify(() -> Others.checkPrivilegeFlagSilence(victim.getLocation(), Flags.PVP, victim, null));
        }
        others.verifyNoMoreInteractions();
    }

    static Stream<Arguments> pvpPermissions() {
        return Arrays.stream(Delivery.values()).flatMap(delivery -> Stream.of(
                Arguments.of(delivery, false, false), Arguments.of(delivery, false, true),
                Arguments.of(delivery, true, false), Arguments.of(delivery, true, true)));
    }

    @ParameterizedTest
    @EnumSource(Delivery.class)
    void leavesNonPlayerSourcesAndAlreadyCancelledEventsAlone(Delivery delivery) {
        Cow cow = target(Cow.class, 1);
        List<PotionEffect> effects = List.of(effect("poison"));
        Exposure natural = exposure(delivery, null, effects, cow);
        Exposure witch = exposure(delivery, mock(Witch.class), effects, cow);
        Exposure cancelled = exposure(delivery, attacker, effects, cow);
        if (cancelled.splash != null) cancelled.splash.setCancelled(true);
        else cancelled.cloud.setCancelled(true);

        natural.apply();
        witch.apply();
        cancelled.apply();

        natural.assertAffected(cow);
        witch.assertAffected(cow);
        assertEquals(List.of(cow), cancelled.affected());
        assertTrue(cancelled.splash != null ? cancelled.splash.isCancelled() : cancelled.cloud.isCancelled());
        others.verifyNoInteractions();
    }

    @Test
    void checksTheBaseCloudEffectEvenWhenCustomEffectsAreBeneficial() {
        Cow cow = target(Cow.class, 1);
        Exposure exposure = exposure(Delivery.CLOUD, attacker, List.of(effect("speed")), cow);
        PotionData data = mock(PotionData.class);
        PotionType base = mock(PotionType.class);
        PotionEffectType poison = effect("poison").getType();
        when(data.getType()).thenReturn(base);
        when(base.getEffectType()).thenReturn(poison);
        when(exposure.cloud.getEntity().getBasePotionData()).thenReturn(data);

        exposure.apply();

        exposure.assertAffected();
        verifyCheck(cow, Flags.ANIMAL_KILLING, attacker);
    }

    @Test
    void retainsABeneficialBaseCloudEffect() {
        Cow cow = target(Cow.class, 1);
        Exposure exposure = exposure(Delivery.CLOUD, attacker, List.of(), cow);
        PotionData data = mock(PotionData.class);
        PotionType base = mock(PotionType.class);
        PotionEffectType speed = effect("speed").getType();
        when(data.getType()).thenReturn(base);
        when(base.getEffectType()).thenReturn(speed);
        when(exposure.cloud.getEntity().getBasePotionData()).thenReturn(data);

        exposure.apply();

        exposure.assertAffected(cow);
        others.verifyNoInteractions();
    }

    @Test
    void turtleMasterIncludesHarmfulSlownessThroughTheLegacyBasePotionApi() {
        Cow cow = target(Cow.class, 1);
        Exposure exposure = exposure(Delivery.CLOUD, attacker, List.of(), cow);
        when(exposure.cloud.getEntity().getBasePotionData()).thenReturn(new PotionData(PotionType.TURTLE_MASTER));
        PotionEffectType slowness = effect("slowness").getType();
        // Legacy effect wrappers resolve their concrete Bukkit type from the server registry.
        try (MockedStatic<PotionEffectType> registry = mockStatic(PotionEffectType.class)) {
            registry.when(() -> PotionEffectType.getById(PotionEffectType.SLOW.getId())).thenReturn(slowness);

            exposure.apply();

            exposure.assertAffected();
            verifyCheck(cow, Flags.ANIMAL_KILLING, attacker);
        }
    }

    @Test
    void checksTheTargetsCurrentPositionOnEachCloudApplication() {
        Cow cow = target(Cow.class, 1);
        Exposure first = exposure(Delivery.CLOUD, attacker, List.of(effect("poison")), cow);
        first.apply();
        first.assertAffected();

        when(cow.getLocation()).thenReturn(new Location(null, -1, 64, 0));
        allow(cow, Flags.ANIMAL_KILLING, attacker, true);
        Exposure second = exposure(Delivery.CLOUD, attacker, List.of(effect("poison")), cow);
        second.apply();
        second.assertAffected(cow);
    }

    private <T extends LivingEntity> T target(Class<T> type, int x) {
        T target = mock(type);
        when(target.getLocation()).thenReturn(new Location(null, x, 64, 0));
        return target;
    }

    private PotionEffect effect(String key) {
        PotionEffectType type = mock(PotionEffectType.class);
        when(type.getKey()).thenReturn(NamespacedKey.minecraft(key));
        PotionEffect effect = mock(PotionEffect.class);
        when(effect.getType()).thenReturn(type);
        return effect;
    }

    private void allow(LivingEntity target, PriFlag flag, Player player, boolean allowed) {
        others.when(() -> Others.checkPrivilegeFlag(target.getLocation(), flag, player, null)).thenReturn(allowed);
    }

    private void verifyCheck(LivingEntity target, PriFlag flag, Player player) {
        others.verify(() -> Others.checkPrivilegeFlag(target.getLocation(), flag, player, null));
    }

    private Exposure exposure(Delivery delivery, ProjectileSource source, List<PotionEffect> effects,
                              LivingEntity... targets) {
        if (delivery == Delivery.SPLASH) {
            ThrownPotion potion = mock(ThrownPotion.class);
            when(potion.getShooter()).thenReturn(source);
            when(potion.getEffects()).thenReturn(effects);
            // The impact point deliberately differs from every target's location.
            when(potion.getLocation()).thenReturn(new Location(null, 0, 64, 0));
            Map<LivingEntity, Double> intensity = new LinkedHashMap<>();
            for (LivingEntity target : targets) intensity.put(target, 0.75);
            return new Exposure(new PotionSplashEvent(potion, intensity), null);
        }
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getSource()).thenReturn(source);
        when(cloud.getCustomEffects()).thenReturn(effects);
        when(cloud.getLocation()).thenReturn(new Location(null, 0, 64, 0));
        return new Exposure(null, new AreaEffectCloudApplyEvent(cloud, new ArrayList<>(List.of(targets))));
    }

    private record Exposure(PotionSplashEvent splash, AreaEffectCloudApplyEvent cloud) {
        void apply() {
            if (splash != null) new Piston().handler(splash);
            else new CloudEffect().handler(cloud);
        }

        List<LivingEntity> affected() {
            return splash != null ? splash.getAffectedEntities().stream()
                    .filter(target -> splash.getIntensity(target) > 0).toList() : cloud.getAffectedEntities();
        }

        void assertAffected(LivingEntity... expected) {
            assertEquals(List.of(expected), affected());
            assertFalse(splash != null ? splash.isCancelled() : cloud.isCancelled());
            if (splash != null) {
                for (LivingEntity target : expected) assertEquals(0.75, splash.getIntensity(target));
            }
        }
    }
}
