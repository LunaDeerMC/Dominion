package cn.lunadeer.dominion.utils;

import org.bukkit.entity.Player;

import static cn.lunadeer.dominion.managers.HooksManager.setPlaceholder;

public class MessageDisplay {
    public enum Place {
        BOSS_BAR,
        ACTION_BAR,
        TITLE,
        SUBTITLE,
        CHAT
    }

    public static void show(Player player, Place place, String message) {
        message = setPlaceholder(player, message);
        message = ColorParser.getBukkitType(message);
        // BOSS_BAR, ACTION_BAR, TITLE, SUBTITLE, CHAT
        if (place == Place.BOSS_BAR) {
            Notification.bossBar(player, message);
        } else if (place == Place.CHAT) {
            player.sendMessage(LegacyToMiniMessage.parse(message));
        } else if (place == Place.TITLE) {
            Notification.title(player, message);
        } else if (place == Place.SUBTITLE) {
            Notification.subTitle(player, message);
        } else {
            Notification.actionBar(player, message);
        }
    }

}
