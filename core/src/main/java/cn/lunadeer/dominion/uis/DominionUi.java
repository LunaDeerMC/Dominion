package cn.lunadeer.dominion.uis;

import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.storage.repository.PlayerRepository;
import cn.lunadeer.dominion.uis.chest.DominionChestUi;
import cn.lunadeer.dominion.uis.dialog.DominionDialogUi;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.UiMode;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Selects a UI backend without exposing version-specific details to commands or
 * business menus.
 */
public final class DominionUi {
    private static DominionDialogUi dialog;

    private DominionUi() {
    }

    public static void initialize(JavaPlugin plugin) throws Exception {
        DominionChestUi.initialize(plugin);
        dialog = new DominionDialogUi(plugin);
    }

    public static void openMain(Player player) {
        Scheduler.runTaskAsync(() -> {
            UiMode preference;
            try {
                preference = PlayerRepository.selectUiPreference(player.getUniqueId());
            } catch (Exception exception) {
                XLogger.debug("Unable to load UI preference for {0}: {1}", player.getName(), exception.getMessage());
                preference = UiMode.DEFAULT;
            }
            UiMode selected = resolve(preference);
            Scheduler.runEntityTask(() -> open(player, selected), player);
        });
    }

    public static void setPreferenceAndOpen(Player player, UiMode preference) {
        if (preference == UiMode.DIALOG && !isDialogAvailable(player)) {
            Notification.warn(player, "Dialog UI is not available on this server; your preference was not changed.");
            openChest(player);
            return;
        }
        Scheduler.runTaskAsync(() -> {
            try {
                PlayerRepository.updateUiPreference(player.getUniqueId(), preference);
                Scheduler.runEntityTask(() -> {
                    Notification.info(player, "UI preference set to " + preference.name().toLowerCase() + ".");
                    open(player, resolve(preference));
                }, player);
            } catch (Exception exception) {
                Scheduler.runEntityTask(() -> Notification.error(player, exception), player);
            }
        });
    }

    public static boolean isDialogAvailable(Player player) {
        return dialog != null && dialog.available(player);
    }

    public static void openChest(Player player) {
        DominionChestUi.openMain(player);
    }

    public static void reload() {
        DominionChestUi.reload();
        if (dialog != null) dialog.reload();
    }

    private static UiMode resolve(UiMode preference) {
        if (preference == UiMode.DEFAULT) {
            return UiMode.parse(Configuration.ui.defaultUi, UiMode.CHEST);
        }
        return preference;
    }

    private static void open(Player player, UiMode selected) {
        if (!player.isOnline()) return;
        if (selected == UiMode.DIALOG && isDialogAvailable(player)) {
            dialog.openMain(player);
        } else {
            if (dialog != null && dialog.hasSession(player)) dialog.close(player);
            openChest(player);
        }
    }
}
