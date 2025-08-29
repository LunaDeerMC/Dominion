package cn.lunadeer.dominion.events;

import cn.lunadeer.dominion.handler.*;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static cn.lunadeer.dominion.utils.Misc.isPaper;
import static cn.lunadeer.dominion.utils.Misc.listClassOfPackage;

public class EventsRegister {

    private final JavaPlugin plugin;
    public XVersionManager.ImplementationVersion version;
    public Implementation implementation;

    public enum Implementation {
        SPIGOT,
        PAPER
    }

    public EventsRegister(JavaPlugin plugin) {
        this.plugin = plugin;
        this.version = XVersionManager.VERSION;
        this.implementation = isPaper() ? Implementation.PAPER : Implementation.SPIGOT;
        try {
            XLogger.debug("Load API version: {0}:{1}", implementation.name().toLowerCase(), version.name());
            registerVersion();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException e) {
            XLogger.error("Failed to register events: {0}", e.getMessage());
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }

        new DominionProviderHandler(plugin);
        new MemberProviderHandler(plugin);
        new GroupProviderHandler(plugin);
        new SelectPointEventsHandler(plugin);
        new PlayerStatusHandler(plugin);
    }

    public void registerEvents(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Class<?> clazz = Class.forName(className);
        if (!Listener.class.isAssignableFrom(clazz)) {
            return;
        }
        if (implementation == Implementation.PAPER && clazz.isAnnotationPresent(SpigotOnly.class)) {
            XLogger.debug("Skipping {0} because it is Spigot only", className);
            return;
        }
        if (implementation == Implementation.SPIGOT && clazz.isAnnotationPresent(PaperOnly.class)) {
            XLogger.debug("Skipping {0} because it is Paper only", className);
            return;
        }
        if (clazz.isAnnotationPresent(HighestVersion.class)) {
            HighestVersion highestVersion = clazz.getAnnotation(HighestVersion.class);
            if (highestVersion.value().compareWith(version) < 0) {
                XLogger.debug("Skipping {0} because it is for a lower version", className);
                return;
            }
        }
        if (clazz.isAnnotationPresent(LowestVersion.class)) {
            LowestVersion lowestVersion = clazz.getAnnotation(LowestVersion.class);
            if (lowestVersion.value().compareWith(version) > 0) {
                XLogger.debug("Skipping {0} because it is for a higher version", className);
                return;
            }
        }
        Listener listener = (Listener) clazz.getDeclaredConstructor().newInstance();
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public void registerPackageEvents(String packageName) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        List<String> classesInPackage = listClassOfPackage(plugin, packageName);

        for (String className : classesInPackage) {
            XLogger.debug("Registering event: {0}", className);
            registerEvents(className);
        }
    }

    private void registerVersion() throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        for (XVersionManager.ImplementationVersion v : XVersionManager.ImplementationVersion.values()) {
            String packageName = "cn.lunadeer.dominion." + v.name() + ".";
            registerPackageEvents(packageName + "events.player");
            registerPackageEvents(packageName + "events.environment");
        }
    }

}
