package cn.lunadeer.dominion.utils;

import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import static cn.lunadeer.dominion.utils.Misc.formatString;
import static cn.lunadeer.dominion.utils.XLogger.isDebug;
import static net.kyori.adventure.title.Title.DEFAULT_TIMES;

/**
 * Utility class for sending various types of notifications to players and all online users.
 * Supports chat messages, action bars, titles, subtitles, and boss bars.
 */
public class Notification {
    public static Notification instance;
    private static BukkitAudiences adventure = null;

    public Notification(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.prefix = "&6[&e" + plugin.getName() + "&6]&f";
        if (!Misc.isPaper()) {
            adventure = BukkitAudiences.create(plugin);
        }
    }

    private String prefix;
    private JavaPlugin plugin;

    /**
     * Sets the prefix for all messages sent by this Notification instance.
     *
     * @param prefix The prefix to set, which will be prepended to all messages.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Sends an info message to a command sender.
     *
     * @param sender The command sender to send the message to.
     * @param msg    The message to send.
     */
    public static void info(CommandSender sender, String msg) {
        Component adventureMessage = LegacyToMiniMessage.parse(instance.prefix + " &2" + msg);
        if (adventure != null) {
            adventure.sender(sender).sendMessage(adventureMessage);
        } else {
            sender.sendMessage(adventureMessage);
        }
    }

    /**
     * Sends a formatted info message to a command sender.
     *
     * @param player The command sender to send the message to.
     * @param msg    The message template.
     * @param args   The arguments to format the message.
     */
    public static void info(CommandSender player, String msg, Object... args) {
        info(player, formatString(msg, args));
    }

    /**
     * Sends a warning message to a command sender.
     *
     * @param sender The command sender to send the message to.
     * @param msg    The message to send.
     */
    public static void warn(CommandSender sender, String msg) {
        Component adventureMessage = LegacyToMiniMessage.parse(instance.prefix + " &e" + msg);
        if (adventure != null) {
            adventure.sender(sender).sendMessage(adventureMessage);
        } else {
            sender.sendMessage(adventureMessage);
        }
    }

    /**
     * Sends a formatted warning message to a command sender.
     *
     * @param sender The command sender to send the message to.
     * @param msg    The message template.
     * @param args   The arguments to format the message.
     */
    public static void warn(CommandSender sender, String msg, Object... args) {
        warn(sender, formatString(msg, args));
    }

    /**
     * Sends an error message to a command sender.
     *
     * @param sender The command sender to send the message to.
     * @param msg    The message to send.
     */
    public static void error(CommandSender sender, String msg) {
        Component adventureMessage = LegacyToMiniMessage.parse(instance.prefix + " &c" + msg);
        if (adventure != null) {
            adventure.sender(sender).sendMessage(adventureMessage);
        } else {
            sender.sendMessage(adventureMessage);
        }
    }

    /**
     * Sends a formatted error message to a command sender.
     *
     * @param player The command sender to send the message to.
     * @param msg    The message template.
     * @param args   The arguments to format the message.
     */
    public static void error(CommandSender player, String msg, Object... args) {
        error(player, formatString(msg, args));
    }

    /**
     * Sends an error message to a command sender with an associated throwable.
     *
     * @param player The command sender to send the message to.
     * @param e      The throwable associated with the error.
     */
    public static void error(CommandSender player, Throwable e) {
        error(player, e.getMessage());
        if (isDebug()) {
            XLogger.error(e);
        }
    }

    /**
     * Broadcasts a message to all online players.
     *
     * @param msg The message to broadcast.
     */
    public static void all(String msg) {
        Component adventureMessage = LegacyToMiniMessage.parse(instance.prefix + " &2" + msg);
        if (adventure != null) {
            adventure.all().sendMessage(adventureMessage);
        } else {
            instance.plugin.getServer().broadcast(adventureMessage);
        }
    }

    /**
     * Broadcasts a formatted message to all online players.
     *
     * @param msg  The message template to broadcast.
     * @param args Arguments to format the message.
     */
    public static void all(String msg, Object... args) {
        all(formatString(msg, args));
    }

    /**
     * Sends an action bar message to a specific player.
     *
     * @param player The player to send the message to.
     * @param msg    The message to display.
     */
    public static void actionBar(Player player, String msg) {
        Component adventureMessage = LegacyToMiniMessage.parse(msg);
        if (adventure != null) {
            adventure.player(player).sendActionBar(adventureMessage);
        } else {
            player.sendActionBar(adventureMessage);
        }
    }

    /**
     * Sends a formatted action bar message to a specific player.
     *
     * @param player The player to send the message to.
     * @param msg    The message template to display.
     * @param args   Arguments to format the message.
     */
    public static void actionBar(Player player, String msg, Object... args) {
        actionBar(player, formatString(msg, args));
    }

    /**
     * Sends a title to a specific player.
     *
     * @param player The player to send the title to.
     * @param title  The title text.
     */
    public static void title(Player player, String title) {
        title(player, title, "");
    }

    /**
     * Sends a subtitle to a specific player.
     *
     * @param player   The player to send the subtitle to.
     * @param subtitle The subtitle text.
     */
    public static void subTitle(Player player, String subtitle) {
        title(player, "", subtitle);
    }

    /**
     * Sends a title and subtitle to a specific player.
     *
     * @param player   The player to send the title and subtitle to.
     * @param title    The title text.
     * @param subtitle The subtitle text.
     */
    public static void title(Player player, String title, String subtitle) {
        Component titleAdventureMessage = LegacyToMiniMessage.parse(title);
        Component subtitleAdventureMessage = LegacyToMiniMessage.parse(subtitle);
        Title adventureTitle = Title.title(titleAdventureMessage, subtitleAdventureMessage, DEFAULT_TIMES);
        if (adventure != null) {
            adventure.player(player).showTitle(adventureTitle);
        } else {
            player.showTitle(adventureTitle);
        }
    }

    /**
     * Sends a boss bar message to a specific player.
     *
     * @param player  The player to send the boss bar to.
     * @param message The message to display on the boss bar.
     */
    public static void bossBar(Player player, String message) {
        NamespacedKey key = new NamespacedKey(instance.plugin, "dominion_bossbar_" + player.getUniqueId());
        BossBar bossBar = player.getServer().createBossBar(
                key,
                ChatColor.translateAlternateColorCodes('&', message),
                BarColor.GREEN,
                BarStyle.SOLID
        );
        bossBar.setProgress(1);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        Scheduler.runTaskLater(() -> {
            if (player.isOnline()) {
                bossBar.removePlayer(player);
            }
            bossBar.setVisible(false);
            player.getServer().removeBossBar(key);
        }, 60);
    }

}
