package cn.lunadeer.dominion.uis;

import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class AbstractUI {

    public static class ConsoleText extends ConfigurationPart {
        public String inGameOnly = "This ui is not available in console mode. Please use it in-game.";
        public String pageInfo = "§7 Page {0}/{1} - Total {2}";
        public String descPrefix = "§7      - §f{0}";
    }

    public static class UiCommandsDescription extends ConfigurationPart {
        public String mainMenu = "Open the Dominion main menu.";
        public String listAll = "List all dominions of this server.";
        public String migrateList = "List all residences can migrate to Dominion.";
        public String dominionList = "List all dominions you can manage.";
        public String dominionManage = "Manage menu of a dominion.";
        public String groupList = "List groups of a dominion.";
        public String groupFlags = "Manage flags of a group.";
        public String memberList = "List members of a dominion.";
        public String memberFlags = "Manage flags of a member.";
        public String environmentFlags = "Manage environment flags of a dominion.";
        public String guestFlags = "Manage guest privilege flags of a dominion.";
        public String monsterSpawnSettings = "Manage detailed monster spawn settings.";
    }

    protected abstract void showTUI(Player player, String... args) throws Exception;

    protected abstract void showCUI(Player player, String... args) throws Exception;

    protected abstract void showConsole(CommandSender sender, String... args) throws Exception;

    protected void displayByPreference(CommandSender sender, String... args) {
        try {
            if (sender instanceof Player player) {
                PlayerDTO playerDTO = CacheManager.instance.getPlayer(player.getUniqueId());
                if (playerDTO == null) {
                    showTUI(player, args);
                    XLogger.warn("PlayerDTO not found for player: " + player.getName() + ". Showing TUI instead.");
                    return;
                }
                // If UUID starts with "00000000", it is a bedrock player so we show CUI,
                // otherwise we show TUI.
                // This is a workaround for bedrock players who cannot use TUI.
                if (player.getUniqueId().toString().startsWith("00000000")) {
                    showCUI(player, args);
                    return;
                }
                if (PlayerDTO.UI_TYPE.valueOf(Configuration.defaultUiType).equals(PlayerDTO.UI_TYPE.BY_PLAYER)) {
                    if (playerDTO.getUiPreference().equals(PlayerDTO.UI_TYPE.CUI)) {
                        showCUI(player, args);
                    } else if (playerDTO.getUiPreference().equals(PlayerDTO.UI_TYPE.TUI)) {
                        showTUI(player, args);
                    }
                } else {
                    PlayerDTO.UI_TYPE type = PlayerDTO.UI_TYPE.valueOf(Configuration.defaultUiType);
                    if (type.equals(PlayerDTO.UI_TYPE.CUI)) {
                        showCUI(player, args);
                    } else if (type.equals(PlayerDTO.UI_TYPE.TUI)) {
                        showTUI(player, args);
                    }
                }
            } else {
                Notification.info(sender, "--------------------------------------------------");
                showConsole(sender, args);
                Notification.info(sender, "--------------------------------------------------");
            }
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }
}
