package cn.lunadeer.dominion.utils.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

import static cn.lunadeer.dominion.utils.Misc.isPaper;

public class Scheduler {
    public static Scheduler instance;
    private final JavaPlugin plugin;
    private boolean isPaper = false;

    public Scheduler(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.isPaper = isPaper();
    }

    public static void cancelAll() {
        if (instance.isPaper) {
            instance.plugin.getServer().getGlobalRegionScheduler().cancelTasks(instance.plugin);
            instance.plugin.getServer().getGlobalRegionScheduler().cancelTasks(instance.plugin);
        } else {
            instance.plugin.getServer().getScheduler().cancelTasks(instance.plugin);
        }
    }

    /**
     * Run a task later
     *
     * @param task  The task to run
     * @param delay The delay in ticks (20 ticks = 1 second in Minecraft)
     */
    public static CancellableTask runTaskLater(Runnable task, long delay) {
        if (delay <= 0) {
            runTask(task);
            return null;
        }
        if (instance.isPaper) {
            return new PaperTask(instance.plugin.getServer().getGlobalRegionScheduler().runDelayed(instance.plugin, (plugin) -> task.run(), delay));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTaskLater(instance.plugin, task, delay));
        }
    }

    /**
     * Run a task
     *
     * @param task The task to run
     */
    public static CancellableTask runTask(Runnable task) {
        if (instance.isPaper) {
            return new PaperTask(instance.plugin.getServer().getGlobalRegionScheduler().run(instance.plugin, (plugin) -> task.run()));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTask(instance.plugin, task));
        }
    }

    /**
     * Run a task repeatedly
     *
     * @param task   The task to run
     * @param delay  The delay in ticks (20 ticks = 1 second in Minecraft)
     * @param period The period in ticks (20 ticks = 1 second in Minecraft)
     */
    public static CancellableTask runTaskRepeat(Runnable task, long delay, long period) {
        if (instance.isPaper) {
            return new PaperTask(instance.plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(instance.plugin, (plugin) -> task.run(), delay, period));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTaskTimer(instance.plugin, task, delay, period));
        }
    }

    /**
     * Run a task later asynchronously
     *
     * @param task  The task to run
     * @param delay The delay in ticks (20 ticks = 1 second in Minecraft)
     */
    public static CancellableTask runTaskLaterAsync(Runnable task, long delay) {
        if (delay <= 0) {
            runTaskAsync(task);
            return null;
        }
        if (instance.isPaper) {
            return new PaperTask(instance.plugin.getServer().getAsyncScheduler().runDelayed(instance.plugin, (plugin) -> task.run(), delay * 50, TimeUnit.MILLISECONDS));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTaskLaterAsynchronously(instance.plugin, task, delay));
        }
    }

    /**
     * Run a task asynchronously
     *
     * @param task The task to run
     */
    public static CancellableTask runTaskAsync(Runnable task) {
        if (instance.isPaper) {
            return new PaperTask(instance.plugin.getServer().getAsyncScheduler().runNow(instance.plugin, (plugin) -> task.run()));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTaskAsynchronously(instance.plugin, task));
        }
    }

    /**
     * Run a task repeatedly asynchronously
     *
     * @param task   The task to run
     * @param delay  The delay in ticks (20 ticks = 1 second in Minecraft)
     * @param period The period in ticks (20 ticks = 1 second in Minecraft)
     */
    public static CancellableTask runTaskRepeatAsync(Runnable task, long delay, long period) {
        if (instance.isPaper) {
            return new PaperTask(instance.plugin.getServer().getAsyncScheduler().runAtFixedRate(instance.plugin, (plugin) -> task.run(), delay * 50, period * 50, TimeUnit.MILLISECONDS));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTaskTimerAsynchronously(instance.plugin, task, delay, period));
        }
    }

    public static CancellableTask runEntityTask(Runnable task, Entity entity) {
        if (instance.isPaper) {
            return new PaperTask(entity.getScheduler().run(instance.plugin, (plugin) -> task.run(), null));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTaskAsynchronously(instance.plugin, task));
        }
    }

    /**
     * Run a task at a specific location
     *
     * @param task     The task to run
     * @param location The location to run the task at
     */
    public static CancellableTask runTaskAtLocation(Runnable task, Location location) {
        if (instance.isPaper) {
            return new PaperTask(instance.plugin.getServer().getRegionScheduler().run(instance.plugin, location, (plugin) -> task.run()));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTask(instance.plugin, task));
        }
    }

    /**
     * Run a task at a specific location later
     *
     * @param task     The task to run
     * @param location The location to run the task at
     * @param delay    The delay in ticks (20 ticks = 1 second in Minecraft)
     */
    public static CancellableTask runTaskAtLocationLater(Runnable task, Location location, long delay) {
        if (delay <= 0) {
            return runTaskAtLocation(task, location);
        }
        if (instance.isPaper) {
            return new PaperTask(instance.plugin.getServer().getRegionScheduler().runDelayed(instance.plugin, location, (plugin) -> task.run(), delay));
        } else {
            return new SpigotTask(instance.plugin.getServer().getScheduler().runTaskLater(instance.plugin, task, delay));
        }
    }
}
