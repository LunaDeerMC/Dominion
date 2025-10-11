package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.events.FlagRegisterEvent;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class FlagRegisterHandler implements Listener {

    public FlagRegisterHandler(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFlagRegister(FlagRegisterEvent event) {
        XLogger.debug("External flag registered: " +
                event.getFlag().getDisplayName() +
                " (" + event.getFlag().getFlagName() + ")"
                + " - From: " + event.getPlugin().getName());
    }

}
