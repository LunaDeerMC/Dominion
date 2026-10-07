import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.events.dominion.modify.DominionReSizeEvent;
import cn.lunadeer.dominion.providers.DominionProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Run only on a disposable Paper server. Exercises real commands, events, SQLite and cache. */
public final class Issue296 extends JavaPlugin implements Listener {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000296");
    private static final String[] DIRECTIONS = {"east", "west", "up", "down", "north", "south"};
    private static final int[] BOUND = {3, 0, 4, 1, 2, 5};
    private static final int[] SIGN = {1, -1, 1, -1, -1, 1};
    private int index, checks, failures;
    private String prefix;
    private boolean cancelNext, negativeNext;
    private final AtomicInteger accepted = new AtomicInteger();

    @Override public void onEnable() {
        getCommand("issue296").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler public void onResize(DominionReSizeEvent event) {
        if (cancelNext) { cancelNext = false; event.setCancelled(true); }
        if (negativeNext) { negativeNext = false; event.setSize(-100); }
        event.afterModified(result -> { if (result != null) accepted.incrementAndGet(); });
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equals("verify")) {
            var fixtures = getConfig().getConfigurationSection("expected");
            if (fixtures == null) { fail("no completed run to verify"); return true; }
            for (String name : fixtures.getKeys(false)) {
                int[] expected = fixtures.getIntegerList(name).stream().mapToInt(Integer::intValue).toArray();
                verify(name, expected, "after restart");
            }
            getLogger().info("RESTART SUMMARY checks=" + checks + " failures=" + failures);
            return true;
        }
        if (prefix != null) return true;
        prefix = "e296_" + System.currentTimeMillis() + "_";
        try {
            PlayerDOO.create(OWNER, "Issue296");
            next();
        } catch (Exception exception) { fail(exception.toString()); }
        return true;
    }

    private void next() {
        if (index == 30) {
            saveConfig();
            getLogger().info("SUMMARY checks=" + checks + " failures=" + failures);
            return;
        }
        int n = index++;
        String name = prefix + n;
        int x = (n - 15) * 1000;
        int[] original = {x, 100, 0, x + 20, 120, 20};
        DominionProvider.getInstance().createDominion(Bukkit.getConsoleSender(), name, OWNER,
                Bukkit.getWorlds().get(0), cuboid(original), null, true)
                .thenAccept(created -> sync(() -> {
                    if (created == null) { fail("create " + name); return; }
                    if (n < 18) {
                        int amount = n < 6 ? 1 : n < 12 ? 100 : -100;
                        resize(name, "expand", amount, DIRECTIONS[n % 6]);
                        int[] expected = original.clone();
                        if (amount > 0) expected[BOUND[n % 6]] += SIGN[n % 6] * amount;
                        later(() -> { verify(name, expected, "six directions / amount=" + amount); next(); });
                    } else if (n < 20) {
                        concurrent(name, original, n == 18 ? "east" : "north");
                    } else if (n == 20) {
                        staleSnapshot(created, original);
                    } else if (n < 24) {
                        int amount = n == 21 ? 0 : n == 22 ? -100 : Integer.MIN_VALUE;
                        resize(name, n == 22 ? "contract" : "expand", amount, "east");
                        later(() -> { verify(name, original, "invalid amount " + amount); next(); });
                    } else if (n == 24 || n == 25) {
                        cancelNext = n == 24;
                        negativeNext = n == 25;
                        resize(name, "expand", 100, "east");
                        later(() -> {
                            verify(name, original, n == 24 ? "cancelled event" : "listener negative amount");
                            retry(name, original, "east");
                        });
                    } else if (n == 26) {
                        resize(name, "contract", 5, "east");
                        int[] expected = original.clone(); expected[3] -= 5;
                        later(() -> { verify(name, expected, "normal contraction"); next(); });
                    } else {
                        // More positive 100-block concurrent pairs, with different signed coordinates.
                        concurrent(name, original, DIRECTIONS[n % 6]);
                    }
                }));
    }

    private void concurrent(String name, int[] original, String secondDirection) {
        int before = accepted.get();
        resize(name, "expand", 100, "east");
        resize(name, "expand", 100, secondDirection);
        later(() -> {
            int successes = accepted.get() - before;
            check(successes >= 1 && successes <= 2, "concurrent success count=" + successes);
            int[] expected = original.clone(); expected[3] += 100;
            if (successes == 2) {
                int d = Arrays.asList(DIRECTIONS).indexOf(secondDirection);
                expected[BOUND[d]] += SIGN[d] * 100;
            }
            verify(name, expected, "concurrent writes must match successful events");
            retry(name, expected, secondDirection);
        });
    }

    private void staleSnapshot(DominionDTO created, int[] original) {
        try {
            DominionDTO stale = DominionDOO.select(created.getId());
            resize(created.getName(), "expand", 100, "east");
            later(() -> {
                int[] expected = original.clone(); expected[3] += 100;
                verify(created.getName(), expected, "first resize before stale snapshot");
                DominionProvider.getInstance().resizeDominion(Bukkit.getConsoleSender(), stale,
                        DominionReSizeEvent.TYPE.EXPAND, DominionReSizeEvent.DIRECTION.NORTH, 100)
                        .thenAccept(result -> sync(() -> {
                            check(result == null, "stale snapshot rejected");
                            later(() -> {
                                verify(created.getName(), expected, "stale snapshot did not undo east expansion");
                                retry(created.getName(), expected, "north");
                            });
                        }));
            });
        } catch (Exception exception) { fail(exception.toString()); }
    }

    private void retry(String name, int[] original, String direction) {
        // Wait for the throttled cache, as an actual menu refresh / retry would do.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            resize(name, "expand", 100, direction);
            int[] expected = original.clone();
            int d = Arrays.asList(DIRECTIONS).indexOf(direction);
            expected[BOUND[d]] += SIGN[d] * 100;
            later(() -> { verify(name, expected, "retry after completion"); next(); });
        }, 100L);
    }

    private void resize(String name, String operation, int amount, String direction) {
        String command = "dominion resize " + name + " " + operation + " " + amount + " " + direction;
        getLogger().info("COMMAND " + command);
        check(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command), "command dispatched");
    }

    private void verify(String name, int[] expected, String scenario) {
        try {
            getConfig().set("expected." + name, Arrays.stream(expected).boxed().toList());
            DominionDTO stored = DominionDOO.select(name);
            DominionDTO cached = CacheManager.instance.getDominion(name);
            check(stored != null && Arrays.equals(bounds(stored), expected),
                    scenario + " DB expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(bounds(stored)));
            check(cached != null && Arrays.equals(bounds(cached), expected),
                    scenario + " CACHE expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(bounds(cached)));
        } catch (Exception exception) { fail(scenario + ": " + exception); }
    }

    private static int[] bounds(DominionDTO dominion) {
        if (dominion == null) return null;
        CuboidDTO c = dominion.getCuboid();
        return new int[]{c.x1(), c.y1(), c.z1(), c.x2(), c.y2(), c.z2()};
    }
    private static CuboidDTO cuboid(int[] p) { return new CuboidDTO(p[0], p[1], p[2], p[3], p[4], p[5]); }
    private void sync(Runnable task) { Bukkit.getScheduler().runTask(this, task); }
    private void later(Runnable task) { Bukkit.getScheduler().runTaskLater(this, task, 20L); }
    private void check(boolean passed, String message) {
        checks++;
        if (passed) getLogger().info("PASS " + message); else fail(message);
    }
    private void fail(String message) { failures++; getLogger().severe("FAIL " + message); }
}
