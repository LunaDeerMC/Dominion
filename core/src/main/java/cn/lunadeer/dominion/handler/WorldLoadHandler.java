package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Consumer;

public class WorldLoadHandler implements Listener {

    private static WorldLoadHandler instance;

    public static WorldLoadHandler getInstance() {
        if (instance == null) {
            new WorldLoadHandler(Dominion.instance);
        }
        return instance;
    }

    private final List<World> loadedWorlds = new java.util.ArrayList<>();
    private final List<Consumer<World>> runners = new java.util.ArrayList<>();

    public WorldLoadHandler(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        instance = this;
        Scheduler.runTaskLaterAsync(()->{
            for (World world : plugin.getServer().getWorlds()) {
                if (loadedWorlds.contains(world))  continue;    // Skip already loaded worlds
                loadedWorlds.add(world);
                for (Consumer<World> runner : runners) {
                    try {
                        runner.accept(world);
                    } catch (Exception e) {
                        XLogger.error(e);
                    }
                }
            }
        }, 90 * 20L);
    }

    public void addRunner(Consumer<World> runner) {
        runners.add(runner);
    }

    @EventHandler
    public void onWorldLoaded(WorldLoadEvent event) {
        World world = event.getWorld();
        if (!loadedWorlds.contains(world)) {
            loadedWorlds.add(world);
        }
        for (Consumer<World> runner : runners) {
            try {
                runner.accept(world);
            } catch (Exception e) {
                XLogger.error(e);
            }
        }
    }
}
