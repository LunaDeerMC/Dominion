import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;

/** Actual projectile impacts and cloud ticks on a disposable server; no synthetic events. */
public final class Issue298 extends JavaPlugin implements Listener {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000298");
    private enum Outcome { BLOCK, DAMAGE, HEAL, EFFECT }
    private enum Source { PLAYER, SELF, WITCH }
    private enum Effect {
        HARM("instant_damage", "HARMING", "INSTANT_DAMAGE"),
        HEAL("instant_health", "HEALING", "INSTANT_HEAL"),
        POISON("poison", "POISON", "POISON"),
        REGEN("regeneration", "REGENERATION", "REGEN"),
        TURTLE("slowness", "TURTLE_MASTER", "TURTLE_MASTER");
        final String key, modern, legacy;
        Effect(String key, String modern, String legacy) { this.key = key; this.modern = modern; this.legacy = legacy; }
        PotionEffectType type() { return Objects.requireNonNull(PotionEffectType.getByKey(NamespacedKey.minecraft(key))); }
    }
    private record Scenario(String name, boolean cloud, EntityType entity, double targetX, double impactX,
                            Effect effect, Outcome outcome, Source source, boolean network, boolean custom,
                            boolean multiple) {}
    private static final class Target {
        final LivingEntity entity;
        final Outcome outcome;
        final double health;
        int candidates, permitted, damageEvents;
        Target(LivingEntity entity, Outcome outcome) {
            this.entity = entity; this.outcome = outcome; this.health = entity.getHealth();
        }
    }
    private final Queue<Scenario> scenarios = new ArrayDeque<>();
    private final List<Target> targets = new ArrayList<>();
    private final Set<UUID> projectiles = new HashSet<>(), clouds = new HashSet<>();
    private final List<Entity> cleanup = new ArrayList<>();
    private World world;
    private Player visitor, playerTarget;
    private Scenario active;
    private LivingEntity source;
    private String denyName, allowName;
    private boolean running, capturing;
    private int checks, failures, completed, launched, impacts;

    @Override public void onEnable() {
        getCommand("issue298").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) || running) return true;
        visitor = Bukkit.getPlayerExact("Issue298Visitor");
        playerTarget = Bukkit.getPlayerExact("Issue298Target");
        if (visitor == null || playerTarget == null) {
            getLogger().severe("Connect Issue298Visitor and Issue298Target before running issue298");
            return true;
        }
        running = true; checks = failures = completed = 0;
        boolean baseline = args.length > 0 && args[0].equals("baseline");
        try {
            world = visitor.getWorld();
            world.setTime(18000); world.setDifficulty(Difficulty.NORMAL);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.NATURAL_REGENERATION, false);
            for (Player player : List.of(visitor, playerTarget)) {
                player.setOp(false); player.setGameMode(GameMode.SURVIVAL); player.setInvulnerable(false);
                player.setAllowFlight(true); player.setFlying(true); player.setGravity(false);
                player.getInventory().clear(); player.setFoodLevel(20); player.setSaturation(0);
                check(!player.isOp() && !player.hasPermission("dominion.admin"), player.getName() + " is a non-op guest without admin permission");
            }
            PlayerDOO.create(OWNER, "Issue298Owner");
            String prefix = "e298_" + System.currentTimeMillis();
            denyName = prefix + "_deny"; allowName = prefix + "_allow";
            fixture(denyName, 100, false); fixture(allowName, 120, true);
            DominionAPI.getInstance().reloadCache();
            for (int x = 94; x <= 146; x++) for (int z = 3; z <= 23; z++) {
                world.getBlockAt(x, 99, z).setType(Material.STONE);
                for (int y = 100; y <= 108; y++) world.getBlockAt(x, y, z).setType(Material.AIR);
            }
            buildScenarios(baseline);
            getLogger().info("START mode=" + (baseline ? "baseline" : "full") + " scenarios=" + scenarios.size()
                    + " server=" + Bukkit.getVersion());
            later(100, () -> { verifyFixtures(); next(); });
        } catch (Throwable error) { abort(error); }
        return true;
    }

    private void fixture(String name, int x, boolean allow) throws Exception {
        DominionDTO dom = DominionDOO.insert(new DominionDOO(OWNER, name, world.getUID(),
                new CuboidDTO(x, 90, 0, x + 20, 120, 25), -1));
        for (PriFlag flag : Flags.getActivePriFlagsEnable()) if (flag != Flags.ADMIN) dom.setGuestFlagValue(flag, true);
        for (PriFlag flag : List.of(Flags.ANIMAL_KILLING, Flags.VILLAGER_KILLING, Flags.MONSTER_KILLING, Flags.PVP))
            dom.setGuestFlagValue(flag, allow);
    }

    private void verifyFixtures() throws Exception {
        for (boolean allow : new boolean[]{false, true}) {
            String name = allow ? allowName : denyName;
            DominionDTO stored = DominionDOO.select(name), cached = CacheManager.instance.getDominion(name);
            check(stored != null && cached != null, name + " exists in database and cache");
            for (PriFlag flag : List.of(Flags.ANIMAL_KILLING, Flags.VILLAGER_KILLING, Flags.MONSTER_KILLING, Flags.PVP)) {
                check(stored.getGuestFlagValue(flag) == allow && cached.getGuestFlagValue(flag) == allow,
                        name + " persisted/cached " + flag.getFlagName() + "=" + allow);
                check(Others.checkPrivilegeFlagSilence(new Location(world, allow ? 130.5 : 110.5, 100, 10), flag, visitor, null) == allow,
                        "real guest permission " + flag.getFlagName() + "=" + allow);
            }
        }
    }

    private void add(String name, boolean cloud, EntityType type, double targetX, double impactX, Effect effect,
                     Outcome outcome, Source source, boolean network, boolean custom, boolean multiple) {
        scenarios.add(new Scenario((cloud ? "lingering / " : "splash / ") + name, cloud, type, targetX, impactX,
                effect, outcome, source, network, custom, multiple));
    }
    private void add(String name, boolean cloud, EntityType type, double targetX, double impactX, Effect effect, Outcome outcome) {
        add(name, cloud, type, targetX, impactX, effect, outcome, Source.PLAYER, false, false, false);
    }

    private void buildScenarios(boolean baseline) {
        scenarios.clear();
        for (boolean cloud : new boolean[]{false, true}) {
            for (EntityType type : List.of(EntityType.COW, EntityType.VILLAGER, EntityType.CREEPER)) {
                add("deny " + type, cloud, type, 110.5, 110.5, Effect.HARM, Outcome.BLOCK);
                add("allow " + type, cloud, type, 130.5, 130.5, Effect.HARM, Outcome.DAMAGE);
                if (!baseline) add("unclaimed origin / protected " + type, cloud, type, 100.5, 99.0, Effect.HARM, Outcome.BLOCK);
            }
            if (baseline) continue;
            add("denied origin / allowed victim", cloud, EntityType.COW, 121.0, 119.0, Effect.HARM, Outcome.DAMAGE);
            add("allowed origin / denied victim", cloud, EntityType.COW, 119.0, 121.0, Effect.HARM, Outcome.BLOCK);
            add("unclaimed victim", cloud, EntityType.COW, 143.0, 143.0, Effect.HARM, Outcome.DAMAGE);
            for (EntityType type : List.of(EntityType.COW, EntityType.VILLAGER, EntityType.CREEPER)) {
                add("poison denied " + type, cloud, type, 110.5, 110.5, Effect.POISON, Outcome.BLOCK);
                add("poison allowed " + type, cloud, type, 130.5, 130.5, Effect.POISON, Outcome.DAMAGE);
            }
            add("beneficial healing passes", cloud, EntityType.COW, 110.5, 110.5, Effect.HEAL, Outcome.HEAL);
            add("beneficial regeneration passes", cloud, EntityType.COW, 110.5, 110.5, Effect.REGEN, Outcome.EFFECT);
            add("healing damages undead / denied", cloud, EntityType.ZOMBIE, 110.5, 110.5, Effect.HEAL, Outcome.BLOCK);
            add("healing damages undead / allowed", cloud, EntityType.ZOMBIE, 130.5, 130.5, Effect.HEAL, Outcome.DAMAGE);
            add("harming heals undead / denied flag", cloud, EntityType.ZOMBIE, 110.5, 110.5, Effect.HARM, Outcome.HEAL);
            add("custom poison denied", cloud, EntityType.COW, 110.5, 110.5, Effect.POISON,
                    Outcome.BLOCK, Source.PLAYER, false, true, false);
            add("custom poison allowed", cloud, EntityType.COW, 130.5, 130.5, Effect.POISON,
                    Outcome.DAMAGE, Source.PLAYER, false, true, false);
            add("base turtle master denied", cloud, EntityType.COW, 110.5, 110.5, Effect.TURTLE, Outcome.BLOCK);
            add("base turtle master allowed", cloud, EntityType.COW, 130.5, 130.5, Effect.TURTLE, Outcome.EFFECT);
            add("PVP denied", cloud, EntityType.PLAYER, 110.5, 110.5, Effect.HARM, Outcome.BLOCK);
            add("PVP allowed", cloud, EntityType.PLAYER, 130.5, 130.5, Effect.HARM, Outcome.DAMAGE);
            add("PVP unclaimed origin / protected target", cloud, EntityType.PLAYER, 100.5, 99.0, Effect.POISON, Outcome.BLOCK);
            add("PVP denied origin / allowed target", cloud, EntityType.PLAYER, 121.0, 119.0, Effect.HARM, Outcome.DAMAGE);
            add("PVP allowed origin / denied target", cloud, EntityType.PLAYER, 119.0, 121.0, Effect.HARM, Outcome.BLOCK);
            add("self harm remains possible", cloud, EntityType.PLAYER, 110.5, 110.5, Effect.HARM,
                    Outcome.DAMAGE, Source.SELF, false, false, false);
            add("witch source keeps vanilla behavior", cloud, EntityType.COW, 110.5, 110.5, Effect.HARM,
                    Outcome.DAMAGE, Source.WITCH, false, false, false);
            add("mixed denied/allowed victims", cloud, EntityType.COW, 119.0, 120.0, Effect.HARM,
                    Outcome.BLOCK, Source.PLAYER, false, false, true);
            for (boolean allow : new boolean[]{false, true})
                add("client right-click " + (allow ? "allowed" : "denied"), cloud, EntityType.COW,
                        allow ? 130.5 : 110.5, allow ? 130.5 : 110.5, Effect.HARM,
                        allow ? Outcome.DAMAGE : Outcome.BLOCK, Source.PLAYER, true, false, false);
        }
    }

    private void next() throws Exception {
        if (scenarios.isEmpty()) { finish(); return; }
        active = scenarios.remove(); completed++;
        targets.clear(); projectiles.clear(); clouds.clear(); launched = impacts = 0;
        getLogger().info("SCENARIO " + completed + " " + active.name);
        for (Player player : List.of(visitor, playerTarget)) {
            clearEffects(player); player.setHealth(20); player.setNoDamageTicks(0); player.setFireTicks(0);
            player.teleport(new Location(world, active.impactX, 100, 21));
        }
        // Land next to the target: a cloud formed on its head can float above its hitbox.
        visitor.teleport(new Location(world, active.impactX, 103.5, active.network ? 7.5 : 11, 0, 90));
        source = visitor;
        if (active.source == Source.WITCH) source = spawn(EntityType.WITCH, active.impactX, 103.5, 11);
        LivingEntity victim = active.source == Source.SELF ? visitor
                : active.entity == EntityType.PLAYER ? playerTarget : spawn(active.entity, active.targetX, 100, 10);
        if (victim == playerTarget) victim.teleport(new Location(world, active.targetX, 100, 10));
        if (active.outcome == Outcome.HEAL || active.outcome == Outcome.EFFECT) victim.setHealth(3);
        victim.setNoDamageTicks(0);
        targets.add(new Target(victim, active.outcome));
        if (active.multiple) targets.add(new Target(spawn(EntityType.COW, 121.0, 100, 10), Outcome.DAMAGE));
        later(5, this::launch);
    }

    private LivingEntity spawn(EntityType type, double x, double y, double z) {
        LivingEntity entity = (LivingEntity) world.spawnEntity(new Location(world, x, y, z), type);
        entity.setAI(false); entity.setGravity(false); entity.setRemoveWhenFarAway(false);
        entity.setNoDamageTicks(0); cleanup.add(entity);
        return entity;
    }

    private ItemStack potionItem() throws Exception {
        ItemStack stack = new ItemStack(active.cloud ? Material.LINGERING_POTION : Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        if (active.custom) meta.addCustomEffect(new PotionEffect(active.effect.type(), 400, 1), true);
        else {
            PotionType potion;
            try { potion = PotionType.valueOf(active.effect.modern); }
            catch (IllegalArgumentException oldApi) { potion = PotionType.valueOf(active.effect.legacy); }
            try { PotionMeta.class.getMethod("setBasePotionType", PotionType.class).invoke(meta, potion); }
            catch (NoSuchMethodException oldApi) { meta.setBasePotionData(new PotionData(potion)); }
            check(!meta.hasCustomEffects(), active.name + " uses a vanilla base potion");
        }
        stack.setItemMeta(meta); return stack;
    }

    @SuppressWarnings("unchecked")
    private void launch() throws Exception {
        capturing = true;
        ItemStack stack = potionItem();
        if (active.network) {
            visitor.getInventory().setHeldItemSlot(0); visitor.getInventory().setItemInMainHand(stack);
            visitor.updateInventory();
            later(5, () -> visitor.sendMessage("E298_USE:" + completed));
        } else {
            Class<? extends ThrownPotion> type;
            try { type = (Class<? extends ThrownPotion>) Class.forName("org.bukkit.entity." + (active.cloud ? "LingeringPotion" : "SplashPotion")); }
            catch (ClassNotFoundException oldApi) { type = ThrownPotion.class; }
            ThrownPotion projectile = source.launchProjectile(type, new Vector(0, -1.3, 0));
            projectile.setItem(stack); projectile.setGravity(true);
            projectiles.add(projectile.getUniqueId()); cleanup.add(projectile);
        }
        // Self cases deliberately enter the genuine impact/cloud area after launching.
        if (active.source == Source.SELF) later(1, () -> visitor.teleport(new Location(world, active.targetX, 100, 10)));
        later(active.cloud ? 80 : 50, this::assess);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!capturing || !(event.getEntity() instanceof ThrownPotion potion) || potion.getShooter() != source) return;
        projectiles.add(potion.getUniqueId()); cleanup.add(potion); launched++;
        check(!event.isCancelled(), active.name + " real projectile launch is not cancelled");
        check(potion.getShooter() == source, active.name + " projectile source is the expected entity");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void beforeSplash(PotionSplashEvent event) {
        if (!projectiles.contains(event.getPotion().getUniqueId())) return;
        impacts++;
        getLogger().info("IMPACT " + active.name + " at=" + event.getPotion().getLocation());
        for (Target target : targets) if (event.getAffectedEntities().contains(target.entity)) target.candidates++;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void afterSplash(PotionSplashEvent event) {
        if (!projectiles.contains(event.getPotion().getUniqueId())) return;
        for (Target target : targets) if (!event.isCancelled() && event.getIntensity(target.entity) > 0) target.permitted++;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onLingering(LingeringPotionSplashEvent event) {
        if (!projectiles.contains(event.getEntity().getUniqueId())) return;
        impacts++; clouds.add(event.getAreaEffectCloud().getUniqueId()); cleanup.add(event.getAreaEffectCloud());
        check(!event.isCancelled(), active.name + " real lingering impact produces a cloud");
        check(event.getAreaEffectCloud().getSource() == source, active.name + " cloud retains real projectile source");
        getLogger().info("CLOUD " + active.name + " at=" + event.getAreaEffectCloud().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void beforeCloud(AreaEffectCloudApplyEvent event) {
        if (!clouds.contains(event.getEntity().getUniqueId())) return;
        for (Target target : targets) if (event.getAffectedEntities().contains(target.entity)) target.candidates++;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void afterCloud(AreaEffectCloudApplyEvent event) {
        if (!clouds.contains(event.getEntity().getUniqueId())) return;
        for (Target target : targets) if (!event.isCancelled() && event.getAffectedEntities().contains(target.entity)) target.permitted++;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (active == null) return;
        for (Target target : targets) if (event.getEntity().equals(target.entity) && !event.isCancelled()) target.damageEvents++;
    }

    private void assess() {
        capturing = false;
        check(launched == 1, active.name + " exactly one real projectile launched (" + launched + ")");
        check(impacts > 0, active.name + " server engine processed a real impact");
        for (Target target : targets) {
            String name = active.name + " / " + target.entity.getType() + " x=" + target.entity.getLocation().getX();
            check(target.candidates > 0, name + " engine selected victim before protection (" + target.candidates + ")");
            boolean blocked = target.outcome == Outcome.BLOCK;
            check(blocked ? target.permitted == 0 : target.permitted > 0,
                    name + " accepted applications=" + target.permitted);
            double health = target.entity.getHealth();
            boolean effect = target.entity.hasPotionEffect(active.effect.type());
            switch (target.outcome) {
                case BLOCK -> {
                    check(!target.entity.isDead() && health == target.health, name + " health unchanged " + target.health + " -> " + health);
                    check(!effect, name + " harmful effect was never installed");
                }
                case DAMAGE -> {
                    check(target.entity.isDead() || health < target.health, name + " health decreased " + target.health + " -> " + health);
                    check(target.damageEvents > 0, name + " engine emitted uncancelled damage");
                    if (active.effect == Effect.POISON) check(effect, name + " poison actually installed");
                }
                case HEAL -> check(health > target.health, name + " health increased " + target.health + " -> " + health);
                case EFFECT -> check(effect, name + " beneficial effect installed");
            }
        }
        for (Entity entity : cleanup) if (entity.isValid()) entity.remove();
        cleanup.clear(); targets.clear(); active = null;
        later(5, this::next);
    }

    private void clearEffects(LivingEntity entity) {
        for (PotionEffect effect : entity.getActivePotionEffects()) entity.removePotionEffect(effect.getType());
    }
    private void finish() {
        running = capturing = false;
        getLogger().info("SUMMARY scenarios=" + completed + " checks=" + checks + " failures=" + failures);
        for (Player player : List.of(visitor, playerTarget)) if (player.isOnline()) player.sendMessage("E298_DONE");
    }
    private interface Action { void run() throws Exception; }
    private void later(long ticks, Action action) {
        Bukkit.getScheduler().runTaskLater(this, () -> { try { action.run(); } catch (Throwable error) { abort(error); } }, ticks);
    }
    private void check(boolean passed, String message) {
        checks++; if (passed) getLogger().info("PASS " + message);
        else { failures++; getLogger().severe("FAIL " + message); }
    }
    private void abort(Throwable error) {
        failures++; getLogger().log(java.util.logging.Level.SEVERE, "FAIL exception", error);
        scenarios.clear(); finish();
    }
}
