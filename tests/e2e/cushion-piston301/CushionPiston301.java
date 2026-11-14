import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.configuration.WorldWide;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Piston;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Cushion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Destructive local harness: use a disposable world and database only. */
public class CushionPiston301 extends JavaPlugin implements Listener {
    private static final int X = 100008, Y = 80, Z = 100008;
    private record Scenario(String name, Material front, int cushionOffset, boolean allow,
                            boolean cancelled, boolean cushionDestroyed) {}
    private final List<Scenario> scenarios = List.of(
            new Scenario("water", Material.WATER, 0, false, false, false),
            new Scenario("lava", Material.LAVA, 0, false, false, false),
            new Scenario("stone", Material.STONE, 0, false, false, false),
            new Scenario("tnt", Material.TNT, 0, false, false, false),
            new Scenario("fire", Material.FIRE, 0, false, false, false),
            new Scenario("head-protected", Material.AIR, 1, false, true, false),
            new Scenario("head-allowed", Material.AIR, 1, true, false, true),
            new Scenario("water-cushion-outside", Material.WATER, 2, false, false, false),
            new Scenario("stone-protected", Material.STONE, 2, false, true, false),
            new Scenario("stone-allowed", Material.STONE, 2, true, false, true)
    );
    private final List<String> results = new ArrayList<>();
    private World world;
    private JavaPlugin dominion;
    private Scenario current;
    private Cushion cushion;
    private int checks, failures, caseIndex, extendCount, retractCount;
    private Boolean extendCancelled;
    private boolean finished;

    @Override public void onEnable() { Bukkit.getPluginManager().registerEvents(this, this); }

    @EventHandler public void ready(ServerLoadEvent event) {
        world = Bukkit.getWorlds().getFirst();
        dominion = (JavaPlugin) Bukkit.getPluginManager().getPlugin("Dominion");
        log("ENV core=" + Bukkit.getVersion() + " dominion=" + dominion.getDescription().getVersion());
        Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> finish(new AssertionError("Harness timed out")), 1200);
        List<CompletableFuture<?>> loads = new ArrayList<>();
        for (int x = (X >> 4) - 1; x <= (X >> 4) + 1; x++) {
            for (int z = (Z >> 4) - 1; z <= (Z >> 4) + 1; z++) loads.add(world.getChunkAtAsync(x, z, true));
        }
        CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).whenComplete((unused, error) -> {
            if (error != null) { finish(error); return; }
            Bukkit.getRegionScheduler().execute(this, world, X >> 4, Z >> 4, () -> {
                try {
                    world.getChunkAt(X >> 4, Z >> 4).addPluginChunkTicket(this);
                    configurePermission(false);
                    directionChecks();
                    nextScenario();
                } catch (Throwable t) { finish(t); }
            });
        });
    }

    private void configurePermission(boolean allow) throws Exception {
        File file = new File(dominion.getDataFolder(), "world-wide/" + world.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("enabled", true);
        config.set("flag-schema-version", 5);
        for (var flag : Flags.getActiveFlags()) {
            if (flag instanceof EnvFlag) config.set(flag.getConfigurationNameKey(), true);
        }
        config.set(Flags.CUSHION_ENVIRONMENT_BREAK.getConfigurationNameKey(), allow);
        config.save(file);
        WorldWide.load(Bukkit.getConsoleSender(), dominion);
        check(WorldWide.isWorldWideEnabled(world), "real world-wide configuration enabled");
        check(WorldWide.getEnvFlagValue(world, Flags.CUSHION_ENVIRONMENT_BREAK) == allow,
                "real cushion_environment_break=" + allow);
    }

    private void directionChecks() throws Exception {
        Method checkMethod = Class.forName("cn.lunadeer.dominion.v26_3.protection.CushionPistonProtection", true,
                dominion.getClass().getClassLoader()).getMethod("check", BlockPistonEvent.class, List.class, boolean.class);
        Block piston = world.getBlockAt(X, Y, Z);
        for (BlockFace face : List.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN)) {
            Piston state = (Piston) Material.STICKY_PISTON.createBlockData();
            state.setFacing(face); piston.setBlockData(state, false);
            Block moved = piston.getRelative(face, 2);
            for (boolean extending : new boolean[]{true, false}) {
                for (Material material : List.of(Material.AIR, Material.WATER, Material.LAVA, Material.STONE)) {
                    moved.setType(material, false);
                    BlockPistonEvent event = extending
                            ? new BlockPistonExtendEvent(piston, List.of(moved), face)
                            : new BlockPistonRetractEvent(piston, List.of(moved), face);
                    checkMethod.invoke(null, event, List.of(moved), extending);
                    check(!event.isCancelled(), "helper " + face + " extending=" + extending + " material=" + material);
                    moved.setType(Material.AIR, false);
                }
            }
        }
        piston.setType(Material.AIR, false);
    }

    private void nextScenario() throws Exception {
        current = null;
        if (cushion != null && !cushion.isDead()) cushion.remove();
        cushion = null;
        if (caseIndex == scenarios.size()) { finish(null); return; }
        // No physics while clearing prevents previous circuits from creating measured events.
        for (int dx = -1; dx <= 5; dx++) for (int dz = -1; dz <= 1; dz++) for (int y = Y - 1; y <= Y + 5; y++) {
            world.getBlockAt(X + dx, y, Z + dz).setType(y == Y - 1 ? Material.STONE : Material.AIR, false);
        }
        Scenario scenario = scenarios.get(caseIndex++);
        configurePermission(scenario.allow());
        Piston state = (Piston) Material.STICKY_PISTON.createBlockData();
        state.setFacing(BlockFace.EAST);
        world.getBlockAt(X, Y, Z).setBlockData(state, false);
        world.getBlockAt(X + 1, Y, Z).setType(scenario.front(), false);
        if (scenario.cushionOffset() != 0) {
            cushion = world.spawn(new Location(world, X + scenario.cushionOffset() + .5, Y, Z + .5), Cushion.class);
            check(!cushion.isDead(), scenario.name() + " cushion spawned");
        }
        extendCount = 0; retractCount = 0; extendCancelled = null; current = scenario;
        world.getBlockAt(X - 1, Y, Z).setType(Material.REDSTONE_BLOCK, true);
        later(() -> {
            check(extendCount == 1, scenario.name() + " real extend event count=" + extendCount);
            check(extendCancelled != null && extendCancelled == scenario.cancelled(),
                    scenario.name() + " cancelled=" + extendCancelled);
            boolean extended = ((Piston) world.getBlockAt(X, Y, Z).getBlockData()).isExtended();
            check(extended != scenario.cancelled(), scenario.name() + " actual piston extended=" + extended);
            if (cushion != null) check(cushion.isDead() == scenario.cushionDestroyed(),
                    scenario.name() + " actual cushion destroyed=" + cushion.isDead());
            if (cushion != null && !cushion.isDead()) cushion.remove();
            world.getBlockAt(X - 1, Y, Z).setType(Material.AIR, true);
            later(() -> {
                check(retractCount == (scenario.cancelled() ? 0 : 1), scenario.name() + " real retract events=" + retractCount);
                check(!((Piston) world.getBlockAt(X, Y, Z).getBlockData()).isExtended(), scenario.name() + " retracted state");
                nextScenario();
            }, 10);
        }, 10);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void extended(BlockPistonExtendEvent event) {
        if (!isMeasured(event)) return;
        extendCount++; extendCancelled = event.isCancelled();
        log("NATURAL " + current.name() + " extend cancelled=" + event.isCancelled() + " blocks="
                + event.getBlocks().stream().map(b -> b.getType() + ":" + b.getBoundingBox()).toList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void retracted(BlockPistonRetractEvent event) {
        if (!isMeasured(event)) return;
        retractCount++;
        check(!event.isCancelled(), current.name() + " retract not cancelled");
    }

    private boolean isMeasured(BlockPistonEvent event) {
        Block block = event.getBlock();
        return current != null && block.getWorld().equals(world) && block.getX() == X && block.getY() == Y && block.getZ() == Z;
    }
    private interface Step { void run() throws Exception; }
    private void later(Step step, long ticks) {
        Bukkit.getRegionScheduler().runDelayed(this, world, X >> 4, Z >> 4, task -> {
            try { step.run(); } catch (Throwable t) { finish(t); }
        }, ticks);
    }
    private void check(boolean passed, String message) {
        checks++; if (!passed) failures++;
        log((passed ? "PASS " : "FAIL ") + message);
    }
    private void log(String message) { results.add(message); getLogger().info(message); }
    private synchronized void finish(Throwable error) {
        if (finished) return; finished = true;
        if (error != null) { failures++; error.printStackTrace(); log("ERROR " + error); }
        log("SUMMARY checks=" + checks + " failures=" + failures);
        try { Files.write(Path.of("cushion-piston301-results.txt"), results); } catch (Exception e) { e.printStackTrace(); }
        Bukkit.getGlobalRegionScheduler().execute(this, Bukkit::shutdown);
    }
}
