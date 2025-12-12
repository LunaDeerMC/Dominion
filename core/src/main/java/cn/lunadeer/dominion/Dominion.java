package cn.lunadeer.dominion;

import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.events.EventsRegister;
import cn.lunadeer.dominion.managers.HooksManager;
import cn.lunadeer.dominion.managers.MultiServerManager;
import cn.lunadeer.dominion.managers.TeleportManager;
import cn.lunadeer.dominion.misc.InitCommands;
import cn.lunadeer.dominion.misc.Others;
import cn.lunadeer.dominion.uis.MainMenu;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.VaultConnect.VaultConnect;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.XVersionManager;
import cn.lunadeer.dominion.utils.bStatsMetrics;
import cn.lunadeer.dominion.utils.command.CommandManager;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.databse.DatabaseManager;
import cn.lunadeer.dominion.utils.highlighter.HighlighterManager;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.stui.TextUserInterfaceManager;
import cn.lunadeer.dominion.utils.stui.inputter.Inputter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Dominion extends JavaPlugin {

    public static class DominionText extends ConfigurationPart {
        public String loadingConfig = "Loading Configurations...";
        public String pluginEnabled = "Plugin Enabled!";
        public String pluginVersion = "Plugin Version: {0}";
        public String notificationPrefix = "&6[&eDominion&6]&f";
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        new Notification(this);
        new XLogger(this);
        new Scheduler(this);

        // http://patorjk.com/software/taag/#p=display&f=Big&t=Dominion
        XLogger.info("  _____                  _       _");
        XLogger.info(" |  __ \\                (_)     (_)");
        XLogger.info(" | |  | | ___  _ __ ___  _ _ __  _  ___  _ __");
        XLogger.info(" | |  | |/ _ \\| '_ ` _ \\| | '_ \\| |/ _ \\| '_ \\");
        XLogger.info(" | |__| | (_) | | | | | | | | | | | (_) | | | |");
        XLogger.info(" |_____/ \\___/|_| |_| |_|_|_| |_|_|\\___/|_| |_|");
        XLogger.info(" ");
        XLogger.info(Language.dominionText.pluginVersion, this.getDescription().getVersion());

        try {
            XLogger.info(Language.dominionText.loadingConfig);
            Configuration.loadConfigurationAndDatabase(instance.getServer().getConsoleSender());
        } catch (Exception e) {
            XLogger.error(e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Notification.instance.setPrefix(Language.dominionText.notificationPrefix);
        XVersionManager.VERSION = XVersionManager.GetVersion(this);

        new VaultConnect(this);
        new MultiServerManager(this);
        new TeleportManager(this);
        new CacheManager();
        new HighlighterManager(this);
        new Inputter(this);
        new TextUserInterfaceManager(this);
        new ChestUserInterfaceManager(this);
        new DominionInterface();
        new HooksManager(this);

        new EventsRegister(this);
        new InitCommands();
        CommandManager commandManager = new CommandManager(this, "dominion", (sender) -> {
            MainMenu.show(sender, "1");
        });

        bStatsMetrics metrics = new bStatsMetrics(this, 21445);
        metrics.addCustomChart(new bStatsMetrics.SimplePie("database", () -> Configuration.database.type));
        metrics.addCustomChart(new bStatsMetrics.SingleLineChart("dominion_count", () -> CacheManager.instance.dominionCount()));
        metrics.addCustomChart(new bStatsMetrics.SingleLineChart("group_count", () -> CacheManager.instance.groupCount()));
        metrics.addCustomChart(new bStatsMetrics.SingleLineChart("member_count", () -> CacheManager.instance.memberCount()));

        XLogger.info(Language.dominionText.pluginEnabled);

        Others.autoClean();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (DatabaseManager.instance != null)
            DatabaseManager.instance.close();
        Scheduler.cancelAll();
    }

    public static Dominion instance;
    public static Map<UUID, Map<Integer, Location>> pointsSelect = new HashMap<>();

    public static String defaultPermission = "dominion.default";
    public static String adminPermission = "dominion.admin";
}
