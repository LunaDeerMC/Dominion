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
        expected.put("environment/Explosions/BlockExplode.java", "Flags.BLOCK_EXPLODE");
        expected.put("environment/Explosions/CreeperExplode.java", "Flags.CREEPER_EXPLODE");
        expected.put("environment/Explosions/CreeperDamageEntity/ArmorStandExploded.java",
                "Flags.CREEPER_DAMAGE_ENTITY");
        expected.put("environment/Explosions/CreeperDamageEntity/ItemFrameExploded.java",
                "Flags.CREEPER_DAMAGE_ENTITY");
        expected.put("environment/Explosions/TNTDamageEntity/EntityExploded.java", "Flags.TNT_DAMAGE_ENTITY");
        expected.put("environment/Explosions/TNTDamageEntity/HangingExploded.java", "Flags.TNT_DAMAGE_ENTITY");
        expected.put("player/Building/PlaceLiquid.java", "Flags.PLACE_LIQUID");
        expected.put("player/Building/PlaceEntity/ArmorStand.java", "Flags.PLACE_ENTITY");
        expected.put("player/Building/PlaceEntity/ItemFrame.java", "Flags.PLACE_ENTITY");
        expected.put("player/Building/BreakLiquid.java", "Flags.BREAK_LIQUID");
        expected.put("player/Building/BreakEntity/ArmorStandBroken.java", "Flags.BREAK_ENTITY");
        expected.put("player/Building/BreakEntity/ArmorStandShot.java", "Flags.BREAK_ENTITY");
        expected.put("player/Building/BreakEntity/ItemFrameBroken.java", "Flags.BREAK_ENTITY");
        expected.put("player/Building/BreakEntity/ItemFrameShot.java", "Flags.BREAK_ENTITY");
        expected.put("player/Storage/ArmorStandInteractive.java", "Flags.ARMOR_STAND_INTERACTIVE");
        expected.put("player/Storage/ItemFrameContent/ItemFrameGet.java", "Flags.ITEM_FRAME_CONTENT");
        expected.put("player/Storage/ItemFrameContent/ItemFramePut.java", "Flags.ITEM_FRAME_CONTENT");
        expected.put("player/Farming/Fertilizer.java", "Flags.FERTILIZER");
        expected.put("player/Farming/PlantTree.java", "Flags.PLANT_TREE");
        expected.put("player/Projectiles/Trident/TridentLaunch.java", "Flags.TRIDENT");
        expected.put("player/Projectiles/Trident/TridentHit.java", "Flags.TRIDENT");
        expected.put("player/Projectiles/Fireball/FireBallLaunch.java", "Flags.FIREBALL");
        expected.put("player/Projectiles/Fireball/FireBallHit.java", "Flags.FIREBALL");

        Path root = VERSIONS_ROOT.resolve("v1_20_1/src/main/java/cn/lunadeer/dominion/v1_20_1/events");
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Path source = root.resolve(entry.getKey());
            assertTrue(Files.readString(source).contains(entry.getValue()),
                    source + " should reference " + entry.getValue());
        }
    }

    @Test
    void windChargeListenersUseWindChargeFlag() throws Exception {
        Path root = VERSIONS_ROOT.resolve(
                "v1_21/src/main/java/cn/lunadeer/dominion/v1_21/events/player/Projectiles/WindCharge");
        for (String file : new String[]{"WindChargeLaunch.java", "WindChargeHit.java", "WindChargeExplode.java"}) {
            Path source = root.resolve(file);
            assertTrue(Files.readString(source).contains("Flags.WIND_CHARGE"), source.toString());
        }
    }

    @Test
    void modernTntEntityAndHangingListenersUseModernNamesAndDamageFlag() throws Exception {
        Path root = VERSIONS_ROOT.resolve(
                "v1_21/src/main/java/cn/lunadeer/dominion/v1_21/events/environment/Explosions/TNTDamageEntity");
        for (String file : new String[]{"EntityExploded.java", "HangingExploded.java"}) {
            String source = Files.readString(root.resolve(file));
            assertTrue(source.contains("Flags.TNT_DAMAGE_ENTITY"), file);
            assertTrue(source.contains("EntityType.TNT_MINECART"), file);
            assertTrue(source.contains("EntityType.TNT"), file);
        }
    }

    @Test
    void sulfurCubeExplosionHandlingIsConfinedToV26_2Listeners() throws Exception {
        Path baseRoot = VERSIONS_ROOT.resolve(
                "v1_20_1/src/main/java/cn/lunadeer/dominion/v1_20_1/events/environment/Explosions");
        Path modernRoot = VERSIONS_ROOT.resolve(
                "v26_2/src/main/java/cn/lunadeer/dominion/v26_2/events/environment/Explosions");

        assertFalse(Files.readString(baseRoot.resolve("CreeperExplode.java")).contains("SulfurCube"));
        assertTrue(Files.readString(modernRoot.resolve("CreeperExplode.java")).contains("instanceof SulfurCube"));

        for (String file : new String[]{"ArmorStandExploded.java", "ItemFrameExploded.java"}) {
            String baseSource = Files.readString(baseRoot.resolve("CreeperDamageEntity").resolve(file));
            String modernSource = Files.readString(modernRoot.resolve("CreeperDamageEntity").resolve(file));
            assertFalse(baseSource.contains("SulfurCube"), file);
            assertTrue(modernSource.contains("instanceof SulfurCube"), file);
            assertTrue(modernSource.contains("Flags.CREEPER_DAMAGE_ENTITY"), file);
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
