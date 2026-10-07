import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.providers.DominionProvider;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Disposable Paper server only: real client interactions, Dominion providers, SQLite and explosions. */
public final class Issue287 extends JavaPlugin implements Listener {
    private enum Source { ANCHOR, BED, TNT }
    private record Scenario(String name, Source source, boolean enabled, boolean inside,
                            boolean useAllowed, boolean adjacent) {}
    private record Values(boolean anchor, boolean bed, boolean tnt, boolean useAllowed) {}
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000287");
    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("anchor_outside_false", Source.ANCHOR, false, false, false, false),
            new Scenario("anchor_outside_true", Source.ANCHOR, true, false, false, false),
            new Scenario("bed_outside_false", Source.BED, false, false, false, false),
            new Scenario("bed_outside_true", Source.BED, true, false, false, false),
            new Scenario("tnt_outside_false", Source.TNT, false, false, false, false),
            new Scenario("tnt_outside_true", Source.TNT, true, false, false, false),
            new Scenario("anchor_inside_use_denied", Source.ANCHOR, true, true, false, false),
            new Scenario("anchor_inside_damage_false", Source.ANCHOR, false, true, true, false),
            new Scenario("anchor_inside_damage_true", Source.ANCHOR, true, true, true, false),
            new Scenario("bed_inside_use_denied", Source.BED, true, true, false, false),
            new Scenario("bed_inside_damage_false", Source.BED, false, true, true, false),
            new Scenario("bed_inside_damage_true", Source.BED, true, true, true, false),
            new Scenario("anchor_adjacent_false_true", Source.ANCHOR, false, false, false, true),
            new Scenario("anchor_adjacent_true_false", Source.ANCHOR, true, false, false, true),
            new Scenario("bed_adjacent_false_true", Source.BED, false, false, false, true),
            new Scenario("bed_adjacent_true_false", Source.BED, true, false, false, true)
    );

    private final Map<World, DominionDTO[]> claims = new HashMap<>();
    private final List<Block> primaryTargets = new ArrayList<>();
    private final List<Block> adjacentTargets = new ArrayList<>();
    private final List<Block> outsideTargets = new ArrayList<>();
    private Player player;
    private World overworld, nether, world;
    private Scenario current;
    private Block source;
    private String run;
    private int index, completed, checks, failures, explosions, interactions;
    private boolean running, observing, deniedInteraction;

    @Override public void onEnable() {
        getCommand("issue287").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (running) { sender.sendMessage("Issue287 is already running."); return true; }
        if (args.length != 1 || !args[0].matches("[A-Za-z0-9_-]{1,80}")) {
            sender.sendMessage("Usage: issue287 <run-token>; start the provided client.js first.");
            return true;
        }
        player = Bukkit.getPlayerExact("Issue287Bot");
        if (player == null || player.isOp()) {
            sender.sendMessage("Connect the non-OP Issue287Bot first.");
            return true;
        }
        run = args[0];
        index = completed = checks = failures = 0;
        claims.clear();
        running = true;
        try {
            writeReady("\"waiting\":true");
            overworld = findWorld(World.Environment.NORMAL);
            nether = findWorld(World.Environment.NETHER);
            getLogger().info("RUNTIME run=" + run + " server=" + Bukkit.getVersion()
                    + " Dominion=" + Bukkit.getPluginManager().getPlugin("Dominion").getDescription().getVersion()
                    + " playerUUID=" + player.getUniqueId() + " op=" + player.isOp());
            PlayerDOO.create(OWNER, "Issue287Owner");
            createClaim(overworld, false, () -> createClaim(overworld, true,
                    () -> createClaim(nether, false, () -> createClaim(nether, true, this::next))));
        } catch (Exception exception) { abort(exception); }
        return true;
    }

    private World findWorld(World.Environment environment) {
        return Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == environment).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing world: " + environment));
    }

    private void createClaim(World w, boolean adjacent, Runnable next) {
        String name = "e287_" + (adjacent ? "adjacent_" : "primary_") + w.getName();
        CuboidDTO bounds = new CuboidDTO(0, 95, adjacent ? 8 : -8, 16, 110, adjacent ? 24 : 8);
        try {
            DominionDTO existing = DominionDOO.select(name);
            if (existing != null) {
                CuboidDTO c = existing.getCuboid();
                if (!existing.getOwner().equals(OWNER) || !existing.getWorldUid().equals(w.getUID())
                        || c.x1() != bounds.x1() || c.x2() != bounds.x2()
                        || c.y1() != bounds.y1() || c.y2() != bounds.y2()
                        || c.z1() != bounds.z1() || c.z2() != bounds.z2()) {
                    throw new IllegalStateException("Existing fixture has unexpected owner/bounds: " + name);
                }
                remember(w, adjacent, existing);
                next.run();
                return;
            }
            DominionProvider.getInstance().createDominion(Bukkit.getConsoleSender(), name, OWNER, w, bounds, null, true)
                    .whenComplete((created, error) -> sync(() -> {
                        if (error != null) { abort(error); return; }
                        if (created == null) { abort(new IllegalStateException("Cannot create " + name)); return; }
                        remember(w, adjacent, created);
                        next.run();
                    }));
        } catch (Exception exception) { abort(exception); }
    }

    private void remember(World w, boolean adjacent, DominionDTO claim) {
        claims.computeIfAbsent(w, ignored -> new DominionDTO[2])[adjacent ? 1 : 0] = claim;
    }

    private Values values(boolean enabled) {
        return switch (current.source()) {
            case ANCHOR -> new Values(enabled, !enabled, false, current.useAllowed());
            case BED -> new Values(!enabled, enabled, false, current.useAllowed());
            case TNT -> new Values(!enabled, !enabled, enabled, current.useAllowed());
        };
    }

    private void next() {
        if (!running) return;
        if (index == SCENARIOS.size()) { finish(); return; }
        current = SCENARIOS.get(index++);
        world = current.source() == Source.BED ? nether : overworld;
        explosions = interactions = 0;
        deniedInteraction = false;
        try {
            for (int i = 0; i < 2; i++) {
                DominionDTO claim = CacheManager.instance.getDominion(claims.get(world)[i].getId());
                Values v = values(i == 0 ? current.enabled() : !current.enabled());
                claim.setEnvFlagValue(Flags.ANCHOR_EXPLODE, v.anchor());
                claim.setEnvFlagValue(Flags.BED_EXPLODE, v.bed());
                claim.setEnvFlagValue(Flags.TNT_EXPLODE, v.tnt());
                claim.setGuestFlagValue(Flags.ANCHOR, v.useAllowed());
                claim.setGuestFlagValue(Flags.BED, v.useAllowed());
            }
            // Allow Dominion's throttled cache refresh to finish before sending client input.
            later(this::prepare, 100L);
        } catch (Exception exception) { abort(exception); }
    }

    private void prepare() {
        try {
            int z = current.adjacent() ? 8 : 0;
            int x = current.inside() ? 1 : -1;
            for (int bx = -8; bx < 9; bx++) for (int by = 99; by < 107; by++) for (int bz = z - 7; bz < z + 8; bz++) {
                world.getBlockAt(bx, by, bz).setType(by == 99 ? Material.BEDROCK : Material.AIR, false);
            }
            primaryTargets.clear(); adjacentTargets.clear(); outsideTargets.clear();
            int wallX = current.inside() ? 3 : 0;
            if (current.adjacent()) {
                glass(primaryTargets, wallX, wallX + 2, 5, 8);
                glass(adjacentTargets, wallX, wallX + 2, 8, 11);
            } else {
                glass(primaryTargets, wallX, wallX + 2, -2, 3);
            }
            glass(outsideTargets, -2, -1, z + 2, z + 3);
            source = world.getBlockAt(x, 100, z);
            if (current.source() == Source.ANCHOR) {
                source.setType(Material.RESPAWN_ANCHOR, false);
                RespawnAnchor data = (RespawnAnchor) source.getBlockData();
                data.setCharges(1);
                source.setBlockData(data, false);
            } else if (current.source() == Source.BED) {
                placeBed(source, Bed.Part.HEAD);
                placeBed(world.getBlockAt(x - 1, 100, z), Bed.Part.FOOT);
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.setInvulnerable(true);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.getInventory().clear();
            player.teleport(new Location(world, x - 1.8, 100, z + 0.5));
            check(!player.isOp() && !player.getUniqueId().equals(OWNER), "non-owner/non-OP client");
            check((CacheManager.instance.getDominion(source.getLocation()) != null) == current.inside(), "source claim membership");
            verifyFlags(0, values(current.enabled()));
            verifyFlags(1, values(!current.enabled()));
            checkTargets(primaryTargets, claims.get(world)[0].getId(), "primary");
            checkTargets(adjacentTargets, claims.get(world)[1].getId(), "adjacent");
            checkTargets(outsideTargets, null, "outside");
            getLogger().info("CASE=" + current.name() + " source=" + x + ",100," + z + " world=" + world.getName()
                    + " primaryGlass=" + primaryTargets.size() + " adjacentGlass=" + adjacentTargets.size()
                    + " outsideGlass=" + outsideTargets.size());
            observing = true;
            if (current.source() == Source.TNT) {
                later(() -> {
                    TNTPrimed tnt = world.spawn(source.getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
                    tnt.setFuseTicks(1);
                    later(this::result, 40L);
                }, 20L);
            } else {
                writeReady("\"case\":\"" + current.name() + "\",\"world\":\"" + world.getUID()
                        + "\",\"x\":" + x + ",\"y\":100,\"z\":" + z);
                later(this::result, 140L);
            }
        } catch (Exception exception) { abort(exception); }
    }

    private void glass(List<Block> targets, int x1, int x2, int z1, int z2) {
        for (int x = x1; x < x2; x++) for (int y = 100; y < 103; y++) for (int z = z1; z < z2; z++) {
            Block block = world.getBlockAt(x, y, z);
            block.setType(Material.GLASS, false);
            targets.add(block);
        }
    }

    private void placeBed(Block block, Bed.Part part) {
        block.setType(Material.RED_BED, false);
        Bed bed = (Bed) block.getBlockData();
        bed.setFacing(BlockFace.EAST);
        bed.setPart(part);
        block.setBlockData(bed, false);
    }

    private void verifyFlags(int which, Values expected) throws Exception {
        int id = claims.get(world)[which].getId();
        DominionDTO stored = DominionDOO.select(id), cached = CacheManager.instance.getDominion(id);
        for (DominionDTO claim : List.of(stored, cached)) {
            check(claim.getEnvFlagValue(Flags.ANCHOR_EXPLODE) == expected.anchor()
                            && claim.getEnvFlagValue(Flags.BED_EXPLODE) == expected.bed()
                            && claim.getEnvFlagValue(Flags.TNT_EXPLODE) == expected.tnt()
                            && claim.getGuestFlagValue(Flags.ANCHOR) == expected.useAllowed()
                            && claim.getGuestFlagValue(Flags.BED) == expected.useAllowed(),
                    (claim == stored ? "database" : "cache") + " flags claim=" + id + " expected=" + expected);
        }
    }

    private void checkTargets(List<Block> targets, Integer expectedClaim, String group) {
        check(targets.stream().allMatch(block -> {
            DominionDTO claim = CacheManager.instance.getDominion(block.getLocation());
            return expectedClaim == null ? claim == null : claim != null && claim.getId().equals(expectedClaim);
        }), group + " target claim membership");
    }

    private boolean isOurExplosion(Location location) {
        return observing && location.getWorld().equals(world) && location.distanceSquared(source.getLocation()) < 4;
    }

    @EventHandler(priority = EventPriority.MONITOR) public void onBlockExplosion(BlockExplodeEvent event) {
        if (!isOurExplosion(event.getBlock().getLocation())) return;
        explosions++;
        getLogger().info("BLOCK_EVENT case=" + current.name() + " liveType=" + event.getBlock().getType()
                + " snapshotType=" + snapshotType(event) + " list=" + event.blockList().size()
                + " primaryInList=" + event.blockList().stream().filter(primaryTargets::contains).count()
                + " adjacentInList=" + event.blockList().stream().filter(adjacentTargets::contains).count()
                + " cancelled=" + event.isCancelled());
        check(!event.isCancelled(), "explosion is not globally cancelled");
    }

    // Diagnostic only: compile against the project's oldest Bukkit API as the production plugin does.
    private String snapshotType(BlockExplodeEvent event) {
        try { return ((BlockState) event.getClass().getMethod("getExplodedBlockState").invoke(event)).getType().name(); }
        catch (ReflectiveOperationException ignored) { return "unavailable"; }
    }

    @EventHandler(priority = EventPriority.MONITOR) public void onEntityExplosion(EntityExplodeEvent event) {
        if (!isOurExplosion(event.getLocation())) return;
        explosions++;
        getLogger().info("ENTITY_EVENT case=" + current.name() + " entity=" + event.getEntityType()
                + " primaryInList=" + event.blockList().stream().filter(primaryTargets::contains).count()
                + " cancelled=" + event.isCancelled());
        check(!event.isCancelled(), "TNT is not globally cancelled");
    }

    @EventHandler(priority = EventPriority.MONITOR) public void onInteract(PlayerInteractEvent event) {
        if (!observing || event.getPlayer() != player || !source.equals(event.getClickedBlock())) return;
        interactions++;
        deniedInteraction |= event.useInteractedBlock() == Event.Result.DENY;
        getLogger().info("INTERACT case=" + current.name() + " action=" + event.getAction()
                + " useBlock=" + event.useInteractedBlock() + " useItem=" + event.useItemInHand());
    }

    private void result() {
        observing = false;
        boolean explodes = !current.inside() || current.useAllowed();
        long primary = destroyed(primaryTargets), adjacent = destroyed(adjacentTargets), outside = destroyed(outsideTargets);
        getLogger().info("RESULT case=" + current.name() + " events=" + explosions + " interactions=" + interactions
                + " primaryDestroyed=" + primary + "/" + primaryTargets.size()
                + " adjacentDestroyed=" + adjacent + "/" + adjacentTargets.size()
                + " outsideDestroyed=" + outside + "/" + outsideTargets.size() + " sourceAfter=" + source.getType());
        check(explosions == (explodes ? 1 : 0), "expected explosion count");
        check(current.source() == Source.TNT ? interactions == 0 : interactions > 0, "real client interaction received");
        check(destroyedAsExpected(primary, explodes && current.enabled()), "primary block damage obeys flag");
        if (current.adjacent()) check(destroyedAsExpected(adjacent, explodes && !current.enabled()), "adjacent opposite flag obeyed");
        check(destroyedAsExpected(outside, explodes), "unclaimed block damage remains enabled");
        if (current.inside()) check(deniedInteraction == !current.useAllowed(), "separate use privilege obeyed");
        if (!explodes) check(source.getType() == (current.source() == Source.ANCHOR ? Material.RESPAWN_ANCHOR : Material.RED_BED), "denied source stays intact");
        completed++;
        later(this::next, 10L);
    }

    private long destroyed(List<Block> targets) { return targets.stream().filter(b -> b.getType() != Material.GLASS).count(); }
    private boolean destroyedAsExpected(long count, boolean allowed) { return allowed ? count > 0 : count == 0; }
    private void check(boolean pass, String description) {
        checks++;
        if (pass) getLogger().info("PASS case=" + current.name() + " " + description);
        else { failures++; getLogger().severe("FAIL case=" + current.name() + " " + description); }
    }
    private void abort(Throwable error) {
        failures++;
        getLogger().log(java.util.logging.Level.SEVERE, "FAIL fixture/setup error", error);
        finish();
    }
    private void finish() {
        observing = running = false;
        getLogger().info("SUMMARY scenarios=" + completed + "/" + SCENARIOS.size() + " checks=" + checks + " failures=" + failures);
        try { writeReady("\"done\":true,\"scenarios\":" + completed + ",\"checks\":" + checks + ",\"failures\":" + failures); }
        catch (Exception error) { getLogger().log(java.util.logging.Level.SEVERE, "Cannot publish final result", error); }
    }
    private void writeReady(String fields) throws Exception {
        Files.createDirectories(getDataFolder().toPath());
        Files.writeString(getDataFolder().toPath().resolve("ready.json"), "{\"run\":\"" + run + "\"," + fields + "}");
    }
    private void sync(Runnable task) { Bukkit.getScheduler().runTask(this, task); }
    private void later(Runnable task, long ticks) { Bukkit.getScheduler().runTaskLater(this, () -> { if (running) task.run(); }, ticks); }
}
