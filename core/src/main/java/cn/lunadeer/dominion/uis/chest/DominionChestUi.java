package cn.lunadeer.dominion.uis.chest;

import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.chestui.ChestUiManager;
import cn.lunadeer.dominion.utils.chestui.MenuRoute;
import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Dominion-specific bootstrap and public entry point for the chest UI. */
public final class DominionChestUi {
    private static ChestUiManager manager;

    private DominionChestUi() {
    }

    public static void initialize(JavaPlugin plugin) throws Exception {
        ChestUiConfig config = new ChestUiConfig(plugin);
        manager = new ChestUiManager(plugin, config, MenuRoute.of(MenuId.MAIN), MenuRoute.of(MenuId.CONFIRM),
                Configuration.language,
                ui -> new BuiltinMenuController(ui, config, ui.navigator()));
    }

    public static void openMain(Player player) {
        if (manager != null) manager.openMain(player);
    }

    public static void reload() {
        if (manager != null) manager.reload(Configuration.language);
    }
}
