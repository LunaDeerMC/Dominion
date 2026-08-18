package cn.lunadeer.dominion.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionFlagMappingTest {

    private static final Path VERSIONS_ROOT = Path.of("../versions");

    @Test
    void listenersAreOrganizedByTypeGroupAndFlagAndHaveAtMostOneHandler() throws Exception {
        try (Stream<Path> files = Files.walk(VERSIONS_ROOT)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(source);
                if (content.contains("@EventHandler")) {
                    assertTrue(content.contains("implements Listener"),
                            source + " declares a handler but is not a listener");
                }
                if (!content.contains("implements Listener")) continue;

                Path relative = relativeToEvents(source);
                assertTrue(relative.getNameCount() >= 3,
                        source + " should be organized as type/group/flag");
                assertEquals(1, countOccurrences(content, "implements Listener"),
                        source + " should declare one listener");
                assertTrue(countOccurrences(content, "@EventHandler") <= 1,
                        source + " should contain at most one event handler");
            }
        }
    }

    @Test
    void splitListenersReferenceTheirDedicatedFlags() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("environment/CreatureSpawning/AnimalSpawn.java", "Flags.ANIMAL_SPAWN");
        expected.put("environment/CreatureSpawning/AnimalBreed.java", "Flags.ANIMAL_BREED");
        expected.put("environment/CreatureSpawning/AnimalSpawner.java", "Flags.ANIMAL_SPAWNER");
        expected.put("environment/CreatureSpawning/AnimalSpawnEgg.java", "Flags.ANIMAL_SPAWN_EGG");
        expected.put("environment/CreatureSpawning/VillagerSpawn.java", "Flags.VILLAGER_SPAWN");
        expected.put("environment/CreatureSpawning/MonsterSpawn.java", "Flags.MONSTER_SPAWN");
        expected.put("environment/CreatureBehavior/EnderManTeleport.java", "Flags.ENDER_MAN_TELEPORT");
        expected.put("environment/CreatureBehavior/WitherBreakBlock.java", "Flags.WITHER_BREAK_BLOCK");
        expected.put("environment/Explosions/BlockExplode.java", "Flags.BED_EXPLODE");
        expected.put("environment/Explosions/CreeperExplode.java", "Flags.CREEPER_EXPLODE");
        expected.put("environment/Explosions/CreeperDamageEntity/ArmorStandExploded.java",
                "Flags.CREEPER_DAMAGE_ARMOR_STAND");
        expected.put("environment/Explosions/CreeperDamageEntity/HangingEntityExploded.java",
                "Flags.CREEPER_DAMAGE_HANGING_ENTITY");
        expected.put("environment/Explosions/TNTDamageEntity/EntityExploded.java", "Flags.TNT_DAMAGE_ENTITY");
        expected.put("environment/Explosions/TNTDamageEntity/HangingExploded.java", "Flags.TNT_DAMAGE_HANGING_ENTITY");
        expected.put("environment/EntityProtection/ArmorStandMobDamage.java", "Flags.ARMOR_STAND_MOB_DAMAGE");
        expected.put("environment/EntityProtection/HangingEntityMobDamage.java", "Flags.HANGING_ENTITY_MOB_DAMAGE");
        expected.put("player/Building/PlaceLiquid.java", "Flags.PLACE_LIQUID");
        expected.put("player/Building/Place/FlowerPot.java", "Flags.PLACE_FLOWER_POT_CONTENT");
        expected.put("player/Building/PlaceEntity/ArmorStand.java", "Flags.PLACE_ARMOR_STAND");
        expected.put("player/Building/PlaceEntity/HangingEntity.java", "Flags.PLACE_HANGING_ENTITY");
        expected.put("player/Building/BreakLiquid.java", "Flags.BREAK_LIQUID");
        expected.put("player/Building/BreakBlock/FlowerPot.java", "Flags.BREAK_FLOWER_POT_CONTENT");
        expected.put("player/Building/BreakEntity/ArmorStandBroken.java", "Flags.ARMOR_STAND_DIRECT_BREAK");
        expected.put("player/Building/BreakEntity/ArmorStandShot.java", "Flags.ARMOR_STAND_PROJECTILE_BREAK");
        expected.put("player/Building/BreakEntity/HangingEntityBroken.java", "Flags.HANGING_ENTITY_DIRECT_BREAK");
        expected.put("player/Building/BreakEntity/HangingEntityShot.java", "Flags.HANGING_ENTITY_PROJECTILE_BREAK");
        expected.put("player/Access/Door.java", "Flags.DOOR");
        expected.put("player/Access/Trapdoor.java", "Flags.TRAPDOOR");
        expected.put("player/Access/FenceGate.java", "Flags.FENCE_GATE");
        expected.put("player/Storage/ArmorStandInteractive.java", "Flags.ARMOR_STAND_INTERACTIVE");
        expected.put("player/Storage/Chest.java", "Flags.CHEST");
        expected.put("player/Storage/Barrel.java", "Flags.BARREL");
        expected.put("player/Storage/ShulkerBox.java", "Flags.SHULKER_BOX");
        expected.put("player/Storage/Hopper.java", "Flags.HOPPER");
        expected.put("player/Storage/Dropper.java", "Flags.DROPPER");
        expected.put("player/Storage/Dispenser.java", "Flags.DISPENSER");
        expected.put("player/Storage/Furnace.java", "Flags.FURNACE");
        expected.put("player/Storage/BlastFurnace.java", "Flags.BLAST_FURNACE");
        expected.put("player/Storage/Smoker.java", "Flags.SMOKER");
        expected.put("player/Storage/FlowerPot.java", "Flags.FLOWER_POT");
        expected.put("player/Storage/ItemFrameContent/ItemFrameGet.java", "Flags.ITEM_FRAME_CONTENT");
        expected.put("player/Storage/ItemFrameContent/ItemFramePut.java", "Flags.ITEM_FRAME_CONTENT");
        expected.put("player/Farming/Fertilizer.java", "Flags.FERTILIZER");
        expected.put("player/Farming/PlantTree.java", "Flags.PLANT_TREE");
        expected.put("player/Projectiles/Shoot/ChargingBow.java", "Flags.PROJECTILE_CHARGE");
        expected.put("player/Projectiles/Shoot/ChargingCrossBow.java", "Flags.PROJECTILE_CHARGE");
        expected.put("player/Projectiles/Shoot/ArrowsLaunch.java", "Flags.ARROW_LAUNCH");
        expected.put("player/Projectiles/Shoot/ArrowsHit.java", "Flags.ARROW_HIT");
        expected.put("player/Projectiles/Shoot/ArrowsDoHarm.java", "Flags.ARROW_DAMAGE");
        expected.put("player/Projectiles/Trident/TridentLaunch.java", "Flags.TRIDENT_LAUNCH");
        expected.put("player/Projectiles/Trident/TridentHit.java", "Flags.TRIDENT_HIT");
        expected.put("player/Projectiles/Fireball/FireBallLaunch.java", "Flags.FIREBALL_LAUNCH");
        expected.put("player/Projectiles/Fireball/FireBallHit.java", "Flags.FIREBALL_HIT");

        Path root = VERSIONS_ROOT.resolve("v1_20_1/src/main/java/cn/lunadeer/dominion/v1_20_1/events");
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Path source = root.resolve(entry.getKey());
            assertTrue(Files.readString(source).contains(entry.getValue()),
                    source + " should reference " + entry.getValue());
        }

        String enderMan = Files.readString(root.resolve("environment/CreatureBehavior/EnderMan.java"));
        assertTrue(enderMan.contains("Flags.ENDER_MAN_PICKUP_BLOCK"));
        assertTrue(enderMan.contains("Flags.ENDER_MAN_PLACE_BLOCK"));
        String blockExplode = Files.readString(root.resolve("environment/Explosions/BlockExplode.java"));
        assertTrue(blockExplode.contains("Flags.ANCHOR_EXPLODE"));
        String burnEntity = Files.readString(root.resolve("environment/Fire/BurnEntity.java"));
        assertTrue(burnEntity.contains("Flags.BURN_ENTITY_FIRE"));
        assertTrue(burnEntity.contains("Flags.BURN_ENTITY_LAVA"));
        String flow = Files.readString(root.resolve("environment/NaturalChanges/FlowInProtection.java"));
        assertTrue(flow.contains("Flags.FLOW_IN_WATER"));
        assertTrue(flow.contains("Flags.FLOW_IN_LAVA"));
        String ice = Files.readString(root.resolve("environment/NaturalChanges/IceForm.java"));
        assertTrue(ice.contains("Flags.ICE_FORM_NATURAL"));
        assertTrue(ice.contains("Flags.ICE_FORM_FROST_WALKER"));
    }

    @Test
    void windChargeListenersUseWindChargeFlag() throws Exception {
        Path root = VERSIONS_ROOT.resolve(
                "v1_21/src/main/java/cn/lunadeer/dominion/v1_21/events/player/Projectiles/WindCharge");
        assertTrue(Files.readString(root.resolve("WindChargeLaunch.java")).contains("Flags.WIND_CHARGE_LAUNCH"));
        assertTrue(Files.readString(root.resolve("WindChargeHit.java")).contains("Flags.WIND_CHARGE_HIT"));
        assertTrue(Files.readString(root.resolve("WindChargeExplode.java")).contains("Flags.WIND_CHARGE_EXPLODE"));
    }

    @Test
    void versionSpecificStorageListenersUseDedicatedFlags() throws Exception {
        Path root = VERSIONS_ROOT.resolve(
                "v1_21_9/src/main/java/cn/lunadeer/dominion/v1_21_9/events/player/Storage/Container");
        assertTrue(Files.readString(root.resolve("CopperChest.java")).contains("Flags.COPPER_CHEST"));
        assertTrue(Files.readString(root.resolve("Shelf.java")).contains("Flags.SHELF"));
    }

    @Test
    void modernTntEntityAndHangingListenersUseModernNamesAndDamageFlag() throws Exception {
        Path root = VERSIONS_ROOT.resolve(
                "v1_21/src/main/java/cn/lunadeer/dominion/v1_21/events/environment/Explosions/TNTDamageEntity");
        for (String file : new String[]{"EntityExploded.java", "HangingExploded.java"}) {
            String source = Files.readString(root.resolve(file));
            assertTrue(source.contains("EntityType.TNT_MINECART"), file);
            assertTrue(source.contains("EntityType.TNT"), file);
        }
        assertTrue(Files.readString(root.resolve("EntityExploded.java")).contains("Flags.TNT_DAMAGE_ENTITY"));
        assertTrue(Files.readString(root.resolve("HangingExploded.java")).contains("Flags.TNT_DAMAGE_HANGING_ENTITY"));
    }

    @Test
    void sulfurCubeExplosionHandlingIsConfinedToV26_2Listeners() throws Exception {
        Path baseRoot = VERSIONS_ROOT.resolve(
                "v1_20_1/src/main/java/cn/lunadeer/dominion/v1_20_1/events/environment/Explosions");
        Path modernRoot = VERSIONS_ROOT.resolve(
                "v26_2/src/main/java/cn/lunadeer/dominion/v26_2/events/environment/Explosions");

        assertFalse(Files.readString(baseRoot.resolve("CreeperExplode.java")).contains("SulfurCube"));
        assertTrue(Files.readString(modernRoot.resolve("CreeperExplode.java")).contains("instanceof SulfurCube"));

        for (String file : new String[]{"ArmorStandExploded.java", "HangingEntityExploded.java"}) {
            String baseSource = Files.readString(baseRoot.resolve("CreeperDamageEntity").resolve(file));
            String modernSource = Files.readString(modernRoot.resolve("CreeperDamageEntity").resolve(file));
            assertFalse(baseSource.contains("SulfurCube"), file);
            assertTrue(modernSource.contains("instanceof SulfurCube"), file);
            assertTrue(modernSource.contains("Flags.CREEPER_DAMAGE_ARMOR_STAND")
                    || modernSource.contains("Flags.CREEPER_DAMAGE_HANGING_ENTITY"), file);
        }
    }

    private static Path relativeToEvents(Path source) throws IOException {
        Path current = source;
        while (current != null && !current.getFileName().toString().equals("events")) {
            current = current.getParent();
        }
        if (current == null) throw new IOException("No events directory in " + source);
        return current.relativize(source);
    }

    private static int countOccurrences(String content, String needle) {
        return (content.length() - content.replace(needle, "").length()) / needle.length();
    }
}
