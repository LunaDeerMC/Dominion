import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.doos.PlayerDOO;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

/** Actual server-driven firework explosions; install only on a disposable Paper server. */
public final class Issue288 extends JavaPlugin implements Listener {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000288");
    private final Queue<Runnable> cases = new ArrayDeque<>();
    private World world;
    private Player client;
    private LivingEntity victim;
    private UUID rocket;
    private String scenario;
    private int checks, failures, scenarios, damageEvents, blockedEvents, explosions, cancelledExplosions;
    private double health;
    private boolean captureSpawn, expectedDamage, expectDamageEvent, running;

    @Override public void onEnable() {
        getCommand("issue288").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) return true;
        if (args.length == 1 && (args[0].equals("verify") || args[0].equals("verify-defaults"))) {
            checks = failures = 0;
            verifyPersisted("deny", false);
            verifyPersisted("allow", args[0].equals("verify"));
            verifyPersisted("pvp", args[0].equals("verify"));
            getLogger().info((args[0].equals("verify") ? "RESTART" : "UPGRADE") + " SUMMARY checks=" + checks + " failures=" + failures);
            return true;
        }
        if (running) return true;
        client = Bukkit.getPlayerExact("Issue288Visitor");
        if (client == null) { getLogger().severe("Connect the offline test client Issue288Visitor before running issue288"); return true; }
        running = true;
        world = Bukkit.getWorlds().get(0);
        world.setTime(18000);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        client.setOp(false);
        client.setGameMode(GameMode.SURVIVAL);
        client.setInvulnerable(false);
        client.getInventory().clear();
        try {
            PlayerDOO.create(OWNER, "Issue288Owner");
            String prefix = "e288_" + System.currentTimeMillis();
            for (int i = 0; i < 3; i++) {
                String key = new String[]{"deny", "allow", "pvp"}[i];
                DominionDTO dom = DominionDOO.insert(new DominionDOO(OWNER, prefix + "_" + key,
                        world.getUID(), new CuboidDTO(100 + i * 20, 90, 0, 120 + i * 20, 120, 20), -1));
                check(!dom.getEnvFlagValue(Flags.FIREWORK_DAMAGE_ENTITY), key + " new dominion defaults to false");
                if (i != 0) dom.setEnvFlagValue(Flags.FIREWORK_DAMAGE_ENTITY, true);
                dom.setGuestFlagValue(Flags.PVP, i != 1);
                getConfig().set("fixtures." + key, dom.getName());
            }
            saveConfig();
            DominionAPI.getInstance().reloadCache();
            for (int x = 94; x <= 169; x++) for (int z = -5; z < 20; z++) {
                world.getBlockAt(x, 99, z).setType(Material.STONE);
                for (int y = 100; y < 105; y++) world.getBlockAt(x, y, z).setType(Material.AIR);
            }
            later(120, this::prepare);
        } catch (Exception exception) { fail(exception.toString()); finish(); }
        return true;
    }

    private void prepare() {
        verifyPersisted("deny", false); verifyPersisted("allow", true); verifyPersisted("pvp", true);
        for (EntityType type : new EntityType[]{EntityType.COW, EntityType.VILLAGER, EntityType.ZOMBIE}) {
            cases.add(() -> ownerless("default denial " + type, type, 110.5, 110.5, false, true));
            cases.add(() -> ownerless("outside firework / protected " + type, type, 100.5, 99.0, false, true));
            cases.add(() -> ownerless("flag enabled " + type, type, 130.5, 130.5, true, true));
        }
        cases.add(() -> ownerless("denied origin / allowed victim", EntityType.COW, 121.0, 119.0, true, true));
        cases.add(() -> ownerless("allowed origin / denied victim", EntityType.COW, 119.0, 121.0, false, true));
        cases.add(() -> ownerless("unclaimed land unchanged", EntityType.COW, 165.0, 165.0, true, true));
        cases.add(() -> ownerless("armor stand / flag denied", EntityType.ARMOR_STAND, 110.5, 110.5, false, true));
        cases.add(() -> ownerless("armor stand / flag enabled", EntityType.ARMOR_STAND, 130.5, 130.5, true, true));
        cases.add(() -> ownerless("no-star rocket still explodes", EntityType.COW, 110.5, 110.5, false, false));
        cases.add(() -> dispenser("dispenser / flag denied", 110.5, false));
        cases.add(() -> dispenser("dispenser / flag enabled", 130.5, true));
        cases.add(() -> crossbow("crossbow / flag denied", 110.5, false));
        cases.add(() -> crossbow("crossbow / flag enabled", 130.5, true));
        cases.add(() -> player("player / flag denied / PVP enabled", 110.5, false));
        cases.add(() -> player("player / flag enabled / PVP denied", 130.5, false));
        cases.add(() -> player("player / flag enabled / PVP enabled", 150.5, true));
        next();
    }

    private void begin(String name, LivingEntity target, boolean damage, boolean event) {
        scenario = name; scenarios++;
        victim = target; expectedDamage = damage; expectDamageEvent = event;
        victim.setNoDamageTicks(0); victim.setFireTicks(0);
        health = victim.getHealth(); rocket = null;
        damageEvents = blockedEvents = explosions = cancelledExplosions = 0;
        getLogger().info("SCENARIO " + scenario);
    }

    private LivingEntity mob(EntityType type, double x, double z) {
        // Keep the fixture ticking even when the disposable server has a low simulation distance.
        client.teleport(new Location(world, x, 100, 18));
        LivingEntity result = (LivingEntity) world.spawnEntity(new Location(world, x, 100, z), type);
        result.setAI(false); result.setGravity(false); result.setRemoveWhenFarAway(false);
        return result;
    }

    private void ownerless(String name, EntityType type, double victimX, double rocketX, boolean damage, boolean stars) {
        begin(name, mob(type, victimX, 10), damage, stars);
        Firework firework = world.spawn(new Location(world, rocketX, 101, 10), Firework.class);
        firework.setFireworkMeta((FireworkMeta) rocketItem(stars).getItemMeta());
        firework.setGravity(false); firework.setVelocity(new Vector());
        rocket = firework.getUniqueId();
        check(firework.getShooter() == null, scenario + " has no player shooter");
        firework.detonate();
        later(8, this::assess);
    }

    private void player(String name, double x, boolean damage) {
        client.teleport(new Location(world, x, 100, 10));
        client.setHealth(20); client.setFoodLevel(20); client.setSaturation(0);
        begin(name, client, damage, true);
        Firework firework = world.spawn(new Location(world, x, 101, 10), Firework.class);
        firework.setFireworkMeta((FireworkMeta) rocketItem(true).getItemMeta());
        firework.setVelocity(new Vector());
        rocket = firework.getUniqueId(); firework.detonate();
        later(8, this::assess);
    }

    private void dispenser(String name, double x, boolean damage) {
        begin(name, mob(EntityType.COW, x, 10), damage, true);
        var block = world.getBlockAt((int) x - 2, 100, 10);
        block.setType(Material.DISPENSER);
        Directional direction = (Directional) block.getBlockData(); direction.setFacing(BlockFace.EAST);
        block.setBlockData(direction);
        Dispenser dispenser = (Dispenser) block.getState();
        dispenser.getInventory().setItem(0, rocketItem(true));
        captureSpawn = true;
        check(dispenser.dispense(), scenario + " dispenser dispensed");
        later(20, () -> { block.setType(Material.AIR); assess(); });
    }

    private void crossbow(String name, double x, boolean damage) {
        begin(name, mob(EntityType.COW, x, 1), damage, true);
        client.teleport(new Location(world, x, 100, -2, 0, 0));
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        CrossbowMeta meta = (CrossbowMeta) crossbow.getItemMeta();
        meta.addChargedProjectile(rocketItem(true)); crossbow.setItemMeta(meta);
        client.getInventory().setItemInMainHand(crossbow);
        captureSpawn = true;
        later(3, () -> client.sendMessage("E288_FIRE"));
        later(35, this::assess);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onSpawn(EntitySpawnEvent event) {
        if (!captureSpawn || !(event.getEntity() instanceof Firework firework)) return;
        captureSpawn = false; rocket = firework.getUniqueId();
        // Keep the genuine dispenser/crossbow launch, detonate near its victim after actual flight ticks.
        later(2, () -> { if (firework.isValid()) firework.detonate(); });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onExplode(FireworkExplodeEvent event) {
        if (!event.getEntity().getUniqueId().equals(rocket)) return;
        explosions++;
        if (event.isCancelled()) cancelledExplosions++;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (victim == null || !event.getEntity().getUniqueId().equals(victim.getUniqueId())
                || !event.getDamager().getUniqueId().equals(rocket)) return;
        damageEvents++;
        if (event.isCancelled()) blockedEvents++;
        getLogger().info("DAMAGE " + scenario + " cause=" + event.getCause() + " cancelled=" + event.isCancelled()
                + " raw=" + event.getDamage());
    }

    private void assess() {
        captureSpawn = false;
        check(rocket != null, scenario + " real firework spawned");
        check(explosions > 0 && cancelledExplosions == 0, scenario + " visual explosion retained (events=" + explosions + ")");
        check(expectDamageEvent ? damageEvents > 0 : damageEvents == 0, scenario + " engine damage event count=" + damageEvents);
        check(!expectDamageEvent || (expectedDamage ? blockedEvents == 0 : blockedEvents == damageEvents),
                scenario + " cancellation count=" + blockedEvents);
        check(expectedDamage ? (victim.isDead() || victim.getHealth() < health) : (!victim.isDead() && victim.getHealth() == health),
                scenario + " health " + health + " -> " + victim.getHealth());
        if (!(victim instanceof Player)) victim.remove();
        victim = null; rocket = null;
        later(25, this::next);
    }

    private void verifyPersisted(String key, boolean expected) {
        try {
            String name = getConfig().getString("fixtures." + key);
            DominionDTO stored = DominionDOO.select(name);
            DominionDTO cached = CacheManager.instance.getDominion(name);
            check(stored != null && stored.getEnvFlagValue(Flags.FIREWORK_DAMAGE_ENTITY) == expected, key + " database flag=" + expected);
            check(cached != null && cached.getEnvFlagValue(Flags.FIREWORK_DAMAGE_ENTITY) == expected, key + " cache flag=" + expected);
        } catch (Exception exception) { fail("persistence " + exception); }
    }

    private ItemStack rocketItem(boolean stars) {
        ItemStack stack = new ItemStack(Material.FIREWORK_ROCKET);
        FireworkMeta meta = (FireworkMeta) stack.getItemMeta();
        meta.setPower(0);
        if (stars) meta.addEffect(FireworkEffect.builder().withColor(Color.RED).with(FireworkEffect.Type.BALL).build());
        stack.setItemMeta(meta); return stack;
    }

    private void next() { if (cases.isEmpty()) finish(); else cases.remove().run(); }
    private void finish() {
        running = false;
        getLogger().info("SUMMARY scenarios=" + scenarios + " checks=" + checks + " failures=" + failures);
    }
    private void later(long ticks, Runnable action) {
        Bukkit.getScheduler().runTaskLater(this, () -> { try { action.run(); } catch (Exception e) { fail(e.toString()); finish(); } }, ticks);
    }
    private void check(boolean condition, String message) {
        checks++; if (condition) getLogger().info("PASS " + message); else fail(message);
    }
    private void fail(String message) { failures++; getLogger().severe("FAIL " + message); }
}
