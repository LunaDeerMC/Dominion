import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Limitation;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.events.dominion.DominionCreateEvent;
import cn.lunadeer.dominion.utils.VaultConnect.VaultConnect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Opt-in destructive fixture for an otherwise disposable localhost Paper server. */
public final class VaultE2E extends JavaPlugin implements Listener {
    private static final String LEGACY = "net.milkbowl.vault.economy.Economy";
    private static final String MODERN = "net.milkbowl.vault2.economy.Economy";
    private boolean busy, autoStarted;
    private int checks, failures, totalChecks, totalFailures, phases, claimSequence;
    private String phase, pendingClaim;
    private Consumer<DominionCreateEvent> createListener;
    private final String runId = Long.toString(System.currentTimeMillis(), 36);
    private final int baseX = 10000 + (int) (System.currentTimeMillis() % 100000) * 32;

    @Override public void onEnable() {
        getCommand("vaulte2e").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        log("READY manual=" + Boolean.getBoolean("vaultprobe.manual"));
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (autoStarted || Boolean.getBoolean("vaultprobe.manual")) return;
        autoStarted = true;
        Bukkit.getScheduler().runTaskLater(this, () -> suite(event.getPlayer()), 60L);
    }
    @EventHandler public void onCreate(DominionCreateEvent event) {
        if (createListener != null && event.getName().equals(pendingClaim)) createListener.accept(event);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = args.length > 0 ? Bukkit.getPlayerExact(args[0])
                : sender instanceof Player p ? p : Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) sender.sendMessage("Connect a player first.");
        else if (busy) sender.sendMessage("Probe is already running.");
        else suite(player);
        return true;
    }
    private void suite(Player player) {
        if (busy) return;
        busy = true;
        totalChecks = totalFailures = phases = 0;
        player.setOp(false);
        PermissionAttachment attachment = player.addAttachment(this, "dominion.admin", false);
        Limitation.Economy economy = Configuration.getPlayerLimitation(player).economy;
        boolean enable = economy.enable, square = economy.squareOnly, free = economy.freeForSubs;
        double price = economy.pricePerBlock, refund = economy.refundRate;
        Map<Integer, Location> selection = Dominion.pointsSelect.get(player.getUniqueId());
        economy.enable = true; economy.squareOnly = true; economy.pricePerBlock = 0.1;
        economy.refundRate = 0.5; economy.freeForSubs = false;
        Runnable done = () -> {
            Bukkit.getServicesManager().unregisterAll(this);
            resetAdapter();
            economy.enable = enable; economy.squareOnly = square; economy.freeForSubs = free;
            economy.pricePerBlock = price; economy.refundRate = refund;
            player.removeAttachment(attachment);
            if (selection == null) Dominion.pointsSelect.remove(player.getUniqueId());
            else Dominion.pointsSelect.put(player.getUniqueId(), selection);
            createListener = null; pendingClaim = null; busy = false;
            log("ALL SUMMARY phases=" + phases + " checks=" + totalChecks + " failures=" + totalFailures);
            if (player.isOnline()) player.sendMessage("E298_DONE");
        };
        Runnable failuresModern = () -> {
            if (available(MODERN)) failurePhase(player, true, done);
            else { log("SKIP modern controls: Vault2 API absent under original Vault"); done.run(); }
        };
        Runnable failuresLegacy = () -> failurePhase(player, false, failuresModern);
        baseline(player, () -> {
            if (available(MODERN)) modernSuccess(player, failuresLegacy);
            else failuresLegacy.run();
        });
    }
    private void baseline(Player player, Runnable next) {
        begin("authentic-essentials-baseline", player);
        try {
            check(registration(LEGACY) != null, "authentic legacy service registered");
            check(!available(MODERN) || registration(MODERN) == null, "baseline has no modern service");
            seedEssentials(player, 1000);
            operations(player, () -> essentialsBalance(player), "Vault");
            seedEssentials(player, 1000);
            create(player, true, () -> {
                attempt("paid claim balance", () -> equal(essentialsBalance(player), 960, "Essentials after paid claim"));
                finish(next);
            });
        } catch (Throwable error) { failure("baseline", error); finish(next); }
    }
    private void modernSuccess(Player player, Runnable next) {
        begin("modern-priority-control", player);
        try {
            Control modern = register(true);
            modern.balance = 1000;
            double legacyBefore = essentialsBalance(player);
            check(registration(LEGACY) != null && registration(MODERN) != null, "both service APIs registered");
            operations(player, () -> modern.balance, "Vault2");
            modern.reset(1000, Mode.SUCCESS);
            create(player, true, () -> {
                equal(modern.balance, 960, "modern paid claim balance");
                check(modern.withdrawals == 1, "paid claim withdrew exactly once");
                attempt("legacy untouched", () -> equal(essentialsBalance(player), legacyBefore, "real legacy balance untouched"));
                Bukkit.getServicesManager().unregisterAll(this);
                finish(next);
            });
        } catch (Throwable error) { failure("modern success", error); Bukkit.getServicesManager().unregisterAll(this); finish(next); }
    }
    private void operations(Player player, Balance actual, String expectedAdapter) throws Exception {
        resetAdapter();
        check(!VaultConnect.instance.currencyNamePlural().isBlank(), "currency name available");
        check(adapter().equals(expectedAdapter), "adapter selected=" + expectedAdapter);
        equal(VaultConnect.instance.getBalance(player), 1000, "initial connector balance");
        VaultConnect.instance.withdrawPlayer(player, 10);
        equal(actual.get(), 990, "provider after withdrawal");
        equal(VaultConnect.instance.getBalance(player), 990, "connector after withdrawal");
        VaultConnect.instance.depositPlayer(player, 5);
        equal(actual.get(), 995, "provider after deposit");
    }
    private void failurePhase(Player player, boolean modern, Runnable next) {
        begin((modern ? "modern" : "legacy") + "-failure-controls", player);
        try {
            // Under Unlocked the legacy sentinel proves modern errors never initiate legacy transactions.
            Control legacy = register(false);
            Control selected = modern ? register(true) : legacy;
            resetAdapter();
            check(adapterAfterConnect().equals(modern ? "Vault2" : "Vault"), "correct failure-test adapter");
            selected.reset(1, Mode.SUCCESS);
            expectFailure("insufficient balance", () -> VaultConnect.instance.withdrawPlayer(player, 10));
            equal(selected.balance, 1, "insufficient balance unchanged");
            check(selected.withdrawals == 0, "insufficient balance rejected before withdrawal");
            for (Mode mode : List.of(Mode.FAILURE, Mode.THROW, Mode.THROW_AFTER)) {
                for (boolean withdraw : List.of(true, false)) {
                    selected.reset(1000, mode);
                    if (modern) legacy.reset(1000, Mode.SUCCESS);
                    expectFailure(mode + " " + (withdraw ? "withdraw" : "deposit"), () -> {
                        if (withdraw) VaultConnect.instance.withdrawPlayer(player, 10);
                        else VaultConnect.instance.depositPlayer(player, 10);
                    });
                    check(selected.withdrawals == (withdraw ? 1 : 0) && selected.deposits == (withdraw ? 0 : 1),
                            mode + " provider called exactly once, no opposite transaction");
                    equal(selected.balance, mode == Mode.THROW_AFTER ? (withdraw ? 990 : 1010) : 1000,
                            mode + " no duplicate transaction after error");
                    if (modern) check(legacy.withdrawals == 0 && legacy.deposits == 0, "no legacy transaction fallback");
                }
            }
            failedClaims(player, selected, modern ? legacy : null, List.of(Mode.FAILURE, Mode.THROW_AFTER), 0, () -> {
                Bukkit.getServicesManager().unregisterAll(this);
                finish(next);
            });
        } catch (Throwable error) { failure("failure controls", error); Bukkit.getServicesManager().unregisterAll(this); finish(next); }
    }
    private void failedClaims(Player player, Control selected, Control fallback, List<Mode> modes, int index, Runnable next) {
        if (index == modes.size()) { next.run(); return; }
        Mode mode = modes.get(index);
        selected.reset(1000, mode);
        if (fallback != null) fallback.reset(1000, Mode.SUCCESS);
        create(player, false, () -> {
            check(selected.withdrawals == 1 && selected.deposits == 0, mode + " failed claim withdraws exactly once");
            equal(selected.balance, mode == Mode.THROW_AFTER ? 960 : 1000, mode + " failed claim ledger reflects single provider invocation");
            if (fallback != null) check(fallback.withdrawals == 0 && fallback.deposits == 0, "failed claim never falls back");
            failedClaims(player, selected, fallback, modes, index + 1, next);
        });
    }
    private void create(Player player, boolean success, Runnable next) {
        String name = "ve_" + runId + "_" + (++claimSequence);
        int x = baseX + claimSequence * 64;
        AtomicBoolean completed = new AtomicBoolean();
        pendingClaim = name;
        createListener = event -> {
            check(event.getOperator() == player, "create event retains connected player");
            check(!event.isSkipEconomy(), "create event charges economy");
            check(event.getCuboid().getSquare() == 400, "command selection is 400 square blocks");
            event.getFutureToComplete().whenComplete((created, error) -> Bukkit.getScheduler().runTask(this, () -> {
                if (!completed.compareAndSet(false, true)) return;
                attempt("claim result", () -> {
                    check(error == null, "create future completes normally (rejections return null)");
                    var stored = DominionDOO.select(name);
                    check((created != null) == success, "create result success=" + success);
                    check((stored != null) == success, "SQLite claim exists=" + success);
                    if (success && created != null && stored != null) check(created.getId().equals(stored.getId()), "returned/database IDs match");
                    log("CREATE RESULT name=" + name + " expectedSuccess=" + success + " db=" + (stored == null ? "null" : stored.getId()));
                });
                createListener = null;
                next.run();
            }));
        };
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (completed.compareAndSet(false, true)) { check(false, "create timed out after 20 seconds"); createListener = null; next.run(); }
        }, 400L);
        Map<Integer, Location> selection = new HashMap<>();
        selection.put(0, new Location(player.getWorld(), x, 80, 10000));
        selection.put(1, new Location(player.getWorld(), x + 19, 99, 10019));
        Dominion.pointsSelect.put(player.getUniqueId(), selection);
        check(player.hasPermission("dominion.default"), "normal command permission");
        check(Bukkit.dispatchCommand(player, "dominion create " + name), "paid create dispatched as connected player");
    }
    private void seedEssentials(Player player, double amount) throws Exception {
        Object user = essentialsUser(player);
        user.getClass().getMethod("setMoney", BigDecimal.class).invoke(user, BigDecimal.valueOf(amount));
        equal(essentialsBalance(player), amount, "seed real Essentials balance");
    }
    private Object essentialsUser(Player player) throws Exception {
        Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials == null) throw new IllegalStateException("Essentials required");
        return essentials.getClass().getMethod("getUser", UUID.class).invoke(essentials, player.getUniqueId());
    }
    private double essentialsBalance(Player player) throws Exception {
        Object user = essentialsUser(player);
        return ((BigDecimal) user.getClass().getMethod("getMoney").invoke(user)).doubleValue();
    }
    private Class<?> api(String name) throws ClassNotFoundException {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null) throw new ClassNotFoundException("Vault plugin missing");
        return Class.forName(name, true, vault.getClass().getClassLoader());
    }
    private boolean available(String name) { try { api(name); return true; } catch (ClassNotFoundException missing) { return false; } }
    private RegisteredServiceProvider<?> registration(String name) throws ClassNotFoundException {
        return Bukkit.getServicesManager().getRegistration(api(name));
    }
    private enum Mode { SUCCESS, FAILURE, THROW, THROW_AFTER }
    private static final class Control {
        volatile double balance;
        volatile int withdrawals, deposits;
        volatile Mode mode = Mode.SUCCESS;
        void reset(double amount, Mode outcome) { balance = amount; mode = outcome; withdrawals = deposits = 0; }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Control register(boolean modern) throws Exception {
        Class<?> api = api(modern ? MODERN : LEGACY);
        Control control = new Control();
        Object provider = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("toString") || name.equals("getName")) return "VaultE2E controlled " + (modern ? "modern" : "legacy");
            if (name.equals("hashCode")) return System.identityHashCode(proxy);
            if (name.equals("equals")) return proxy == args[0];
            if (name.contains("CurrencyName") || name.startsWith("currencyName") || name.equals("getDefaultCurrency")) return "probe-coins";
            if (name.equals("getBalance") || name.equals("balance")) {
                if (modern) return BigDecimal.valueOf(control.balance);
                return control.balance;
            }
            if (name.equals("withdraw") || name.equals("deposit") || name.equals("withdrawPlayer") || name.equals("depositPlayer")) {
                boolean withdraw = name.startsWith("withdraw");
                double amount = 0;
                for (Object arg : args) if (arg instanceof Number n) amount = n.doubleValue();
                if (withdraw) control.withdrawals++; else control.deposits++;
                if (control.mode == Mode.THROW) throw new IllegalStateException("probe throws before mutation");
                boolean success = control.mode != Mode.FAILURE;
                if (success) control.balance += withdraw ? -amount : amount;
                if (control.mode == Mode.THROW_AFTER) throw new IllegalStateException("probe throws after mutation");
                String prefix = modern ? "net.milkbowl.vault2" : "net.milkbowl.vault";
                Class<?> response = Class.forName(prefix + ".economy.EconomyResponse", true, api.getClassLoader());
                Class<? extends Enum> type = (Class<? extends Enum>) Class.forName(prefix + ".economy.EconomyResponse$ResponseType", true, api.getClassLoader());
                Object result = Enum.valueOf(type, success ? "SUCCESS" : "FAILURE");
                if (modern) return response.getConstructor(BigDecimal.class, BigDecimal.class, type, String.class)
                        .newInstance(BigDecimal.valueOf(success ? amount : 0), BigDecimal.valueOf(control.balance), result, success ? "" : "probe refused transaction");
                return response.getConstructor(double.class, double.class, type, String.class)
                        .newInstance(success ? amount : 0, control.balance, result, success ? "" : "probe refused transaction");
            }
            if (method.getReturnType() == boolean.class) return true;
            if (method.getReturnType() == int.class) return 2;
            if (method.getReturnType() == String.class) return "probe";
            if (Collection.class.isAssignableFrom(method.getReturnType())) return List.of();
            throw new UnsupportedOperationException("Unexpected probe call: " + method);
        });
        Bukkit.getServicesManager().register((Class) api, provider, this, ServicePriority.Highest);
        log("CONTROL registered synthetic " + (modern ? "modern" : "legacy") + " provider");
        return control;
    }
    private void resetAdapter() {
        try { Field field = VaultConnect.class.getDeclaredField("vaultInstance"); field.setAccessible(true); field.set(VaultConnect.instance, null); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private String adapter() throws Exception {
        Field field = VaultConnect.class.getDeclaredField("vaultInstance"); field.setAccessible(true);
        Object adapter = field.get(VaultConnect.instance);
        return adapter == null ? "null" : adapter.getClass().getSimpleName();
    }
    private String adapterAfterConnect() throws Exception { VaultConnect.instance.currencyNamePlural(); return adapter(); }
    private void begin(String name, Player player) {
        phase = name; checks = failures = 0;
        log("BEGIN player=" + player.getName());
        check(player.isOnline() && !player.isOp() && !player.hasPermission("dominion.admin"), "connected non-admin player");
        for (String pluginName : List.of("Vault", "Essentials", "Dominion")) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            log("PLUGIN " + pluginName + "=" + (plugin == null ? "absent" : plugin.getDescription().getVersion()
                    + " enabled=" + plugin.isEnabled() + " authors=" + plugin.getDescription().getAuthors()));
        }
        for (String api : List.of(LEGACY, MODERN)) {
            try { RegisteredServiceProvider<?> provider = registration(api);
                log("SERVICE " + api + "=" + (provider == null ? "absent" : provider.getProvider().getClass().getName()));
            } catch (ClassNotFoundException missing) { log("SERVICE " + api + " class absent"); }
        }
        resetAdapter();
    }
    private interface Action { void run() throws Exception; }
    private interface Balance { double get() throws Exception; }
    private void attempt(String name, Action action) { try { action.run(); } catch (Throwable error) { failure(name, error); } }
    private void expectFailure(String name, Action action) {
        try { action.run(); check(false, name + " must propagate an error"); }
        catch (Exception expected) { check(true, name + " rejected: " + expected.getMessage()); }
    }
    private void equal(double actual, double expected, String label) { check(Math.abs(actual - expected) < 0.00001, label + " expected=" + expected + " actual=" + actual); }
    private void check(boolean passed, String message) { checks++; if (!passed) failures++; log((passed ? "PASS " : "FAIL ") + message); }
    private void failure(String name, Throwable error) {
        while (error instanceof InvocationTargetException && error.getCause() != null) error = error.getCause();
        check(false, name + " exception=" + error);
    }
    private void finish(Runnable next) {
        log("SUMMARY checks=" + checks + " failures=" + failures);
        totalChecks += checks; totalFailures += failures; phases++;
        Bukkit.getScheduler().runTaskLater(this, next, 20L);
    }
    private void log(String message) { getLogger().info("VAULT_E2E phase=" + phase + " " + message); }
}
