package cn.lunadeer.dominion.events;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionFlagMappingTest {

    @Test
    void splitListenersReferenceTheirDedicatedFlagsOrClassifiers() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("environment/AnimalSpawn.java", "FlagClassifiers.animalSpawn");
        expected.put("environment/VillagerSpawn.java", "FlagClassifiers.villagerSpawn");
        expected.put("environment/MonsterSpawn.java", "FlagClassifiers.monsterSpawn");
        expected.put("environment/EnderMan/Escape.java", "Flags.ENDER_MAN_TELEPORT");
        expected.put("environment/EnderMan/Spawn.java", "Flags.ENDER_MAN_SPAWN");
        expected.put("environment/Wither/ExplodeBySpawn.java", "Flags.WITHER_EXPLODE");
        expected.put("environment/Wither/BreakBlockOnHarmed.java", "Flags.WITHER_BREAK_BLOCK");
        expected.put("environment/Trample/ByMob.java", "Flags.MOB_TRAMPLE");
        expected.put("environment/CreeperExplode/BedAnchorExplode.java", "Flags.BLOCK_EXPLODE");
        expected.put("environment/CreeperExplode/EntityExplode.java", "FlagClassifiers.explosionBlock");
        expected.put("environment/CreeperExplode/ArmorStandExploded.java", "FlagClassifiers.explosionEntity");
        expected.put("environment/CreeperExplode/ItemFrameExploded.java", "FlagClassifiers.explosionEntity");
        expected.put("environment/TNTExplode/EntityExploded.java", "Flags.TNT_DAMAGE_ENTITY");
        expected.put("environment/TNTExplode/HangingExploded.java", "Flags.TNT_DAMAGE_ENTITY");
        expected.put("player/Place/Liquid.java", "Flags.PLACE_LIQUID");
        expected.put("player/Place/ArmorStand.java", "Flags.PLACE_ENTITY");
        expected.put("player/Place/ItemFrame.java", "Flags.PLACE_ENTITY");
        expected.put("player/Break/Liquid.java", "Flags.BREAK_LIQUID");
        expected.put("player/Break/ArmorStandBroken.java", "Flags.BREAK_ENTITY");
        expected.put("player/Break/ArmorStandShot.java", "Flags.BREAK_ENTITY");
        expected.put("player/Break/ItemFrameBroken.java", "Flags.BREAK_ENTITY");
        expected.put("player/Break/ItemFrameShot.java", "Flags.BREAK_ENTITY");
        expected.put("player/Container/ArmorStand.java", "Flags.ARMOR_STAND_INTERACTIVE");
        expected.put("player/Container/ItemFrameGet.java", "Flags.ITEM_FRAME_CONTENT");
        expected.put("player/Container/ItemFramePut.java", "Flags.ITEM_FRAME_CONTENT");
        expected.put("player/Farming/Fertilizer.java", "Flags.FERTILIZER");
        expected.put("player/Farming/PlantTree.java", "Flags.PLANT_TREE");
        expected.put("player/Shoot/TridentLaunch.java", "Flags.TRIDENT");
        expected.put("player/Shoot/TridentHit.java", "Flags.TRIDENT");
        expected.put("player/Shoot/FireBallLaunch.java", "Flags.FIREBALL");
        expected.put("player/Shoot/FireBallHit.java", "Flags.FIREBALL");

        Path root = Path.of("../versions/v1_20_1/src/main/java/cn/lunadeer/dominion/v1_20_1/events");
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Path source = root.resolve(entry.getKey());
            assertTrue(Files.readString(source).contains(entry.getValue()),
                    source + " should reference " + entry.getValue());
        }
    }

    @Test
    void windChargeListenersUseWindChargeFlag() throws Exception {
        Path root = Path.of("../versions/v1_21/src/main/java/cn/lunadeer/dominion/v1_21/events/player/Shoot");
        for (String file : new String[]{"WindChargeLaunch.java", "WindChargeHit.java", "WindChargeExplode.java"}) {
            Path source = root.resolve(file);
            assertTrue(Files.readString(source).contains("Flags.WIND_CHARGE"), source.toString());
        }
    }

    @Test
    void modernTntEntityAndHangingListenersUseModernNamesAndDamageFlag() throws Exception {
        Path root = Path.of("../versions/v1_21/src/main/java/cn/lunadeer/dominion/v1_21/events/environment/TNTExplode");
        for (String file : new String[]{"EntityExploded.java", "HangingExploded.java"}) {
            String source = Files.readString(root.resolve(file));
            assertTrue(source.contains("Flags.TNT_DAMAGE_ENTITY"), file);
            assertTrue(source.contains("EntityType.TNT_MINECART"), file);
            assertTrue(source.contains("EntityType.TNT"), file);
        }
    }
}
