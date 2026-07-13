package cn.lunadeer.dominion.utils.inputter;

import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/** Routes a player's next chat message to an active form input request. */
public class Inputter implements Listener {

    private static Inputter instance;

    public static Inputter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Inputter has not been initialized.");
        }
        return instance;
    }

    public Inputter(JavaPlugin plugin) {
        instance = this;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        cachedInputters = new HashMap<>();
    }

    private final Map<Player, InputterRunner> cachedInputters;

    public void register(InputterRunner inputterRunner) {
        cachedInputters.put(inputterRunner.getSender(), inputterRunner);
    }

    public void unregister(InputterRunner inputterRunner) {
        cachedInputters.remove(inputterRunner.getSender());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInput(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        if (!cachedInputters.containsKey(sender)) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Scheduler.runTask(() -> cachedInputters.get(sender).runner(message));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogout(PlayerQuitEvent event) {
        cachedInputters.remove(event.getPlayer());
    }
}
