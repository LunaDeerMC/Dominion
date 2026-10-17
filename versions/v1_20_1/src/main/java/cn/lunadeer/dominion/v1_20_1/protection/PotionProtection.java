package cn.lunadeer.dominion.v1_20_1.protection;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Animals;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;
import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlagSilence;

public final class PotionProtection {
    private static final NamespacedKey INVERTED_HEALING_AND_HARM = NamespacedKey.minecraft("inverted_healing_and_harm");

    private PotionProtection() {
    }

    public static List<PotionEffectType> effectTypes(Collection<PotionEffect> effects) {
        return effects.stream().map(PotionEffect::getType).toList();
    }

    public static List<PotionEffectType> effectTypes(AreaEffectCloud cloud) {
        List<PotionEffectType> effects = new ArrayList<>(effectTypes(cloud.getCustomEffects()));
        // This API also works before 1.20.2. The only multi-effect base potion,
        // Turtle Master, reports its harmful slowness as its primary effect.
        PotionData base = cloud.getBasePotionData();
        if (base != null && base.getType().getEffectType() != null) {
            effects.add(base.getType().getEffectType());
        }
        return effects;
    }

    public static boolean blocks(Player attacker, LivingEntity target, Collection<PotionEffectType> effects) {
        if (target == attacker) {
            return false;
        }
        Location location = target.getLocation();
        if (target instanceof Player victim) {
            // Preserve the existing PVP policy for both harmful and beneficial potions.
            return !checkPrivilegeFlag(location, Flags.PVP, attacker, null)
                    || !checkPrivilegeFlagSilence(location, Flags.PVP, victim, null);
        }

        PriFlag flag;
        if (target instanceof Animals) {
            flag = Flags.ANIMAL_KILLING;
        } else if (target instanceof Villager) {
            flag = Flags.VILLAGER_KILLING;
        } else if (target instanceof Monster) {
            flag = Flags.MONSTER_KILLING;
        } else {
            return false;
        }
        return effects.stream().anyMatch(effect -> isHarmful(effect, target))
                && !checkPrivilegeFlag(location, flag, attacker, null);
    }

    private static boolean isHarmful(PotionEffectType effect, LivingEntity target) {
        // Keys remain stable when Bukkit renames effect constants (HEAL, HARM, etc.).
        return switch (effect.getKey().toString()) {
            case "minecraft:instant_health" -> hasInvertedHealing(target);
            case "minecraft:instant_damage" -> !hasInvertedHealing(target);
            case "minecraft:speed", "minecraft:haste", "minecraft:strength", "minecraft:jump_boost",
                 "minecraft:regeneration", "minecraft:resistance", "minecraft:fire_resistance",
                 "minecraft:water_breathing", "minecraft:invisibility", "minecraft:night_vision",
                 "minecraft:health_boost", "minecraft:absorption", "minecraft:saturation",
                 "minecraft:luck", "minecraft:slow_falling", "minecraft:conduit_power",
                 "minecraft:dolphins_grace" -> false;
            // Includes debuffs, indirect damage (levitation/infestation), and unknown effects.
            default -> true;
        };
    }

    private static boolean hasInvertedHealing(LivingEntity target) {
        Tag<EntityType> inverted = Bukkit.getTag("entity_types", INVERTED_HEALING_AND_HARM, EntityType.class);
        if (inverted != null) {
            return inverted.isTagged(target.getType());
        }
        // The tag replaced categories in 1.20.5; getCategory throws on modern Paper.
        return target.getCategory() == EntityCategory.UNDEAD;
    }
}
