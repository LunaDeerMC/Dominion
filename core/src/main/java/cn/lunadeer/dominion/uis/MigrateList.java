package cn.lunadeer.dominion.uis;

import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.commands.MigrationCommand;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.ResMigration;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestListView;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.utils.Misc.pageUtil;


public class MigrateList extends AbstractUI {

    public static void show(CommandSender sender, String pageStr) {
        new MigrateList().displayByPreference(sender, pageStr);
    }

    public static SecondaryCommand migrateList = new SecondaryCommand("migrate_list", List.of(
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.migrateList) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                MigrateList.show(sender, getArgumentValue(0));
            } catch (Exception e) {
                Notification.error(sender, e.getMessage());
            }
        }
    }.needPermission(defaultPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class MigrateListCui extends ConfigurationPart {
        public String title = "§6⚡ §c§lMigrate From Residence §6⚡";
        public ListViewConfiguration listConfiguration = new ListViewConfiguration(
                'i',
                List.of(
                        "<######A#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "p#######n"
                )
        );

        public ButtonConfiguration residenceItemButton = ButtonConfiguration.createMaterial(
                'i', Material.PAPER, "§6📋 §f{0}",
                List.of(
                        "§e▶ Click to migrate this residence",
                        "§8  and all its sub-residences"
                )
        );

        public ButtonConfiguration backButton = ButtonConfiguration.createMaterial(
                '<', Material.RED_STAINED_GLASS_PANE,
                "§c« Back to Main Menu",
                List.of(
                        "§7Return to the main menu",
                        "§8to access other features.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public ButtonConfiguration migrateAllButton = ButtonConfiguration.createMaterial(
                'A', Material.DIAMOND, "§6⚡ §c§lMIGRATE ALL",
                List.of(
                        "§e▶ Click to migrate all residences",
                        "§8  and all their sub-residences",
                        "",
                        "§c⚠️ This may take a while,",
                        "§c⚠️ please be patient and do not",
                        "§c⚠️ interrupt the process."
                )
        );
    }

    @Override
    protected void showCUI(Player player, String... args) throws Exception {
        if (!Configuration.residenceMigration) {
            Notification.error(player, Language.migrationCommandText.notEnabled);
            return;
        }

        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(ChestUserInterface.migrateListCui.title);
        view.applyListConfiguration(ChestUserInterface.migrateListCui.listConfiguration, toIntegrity(args[0], 1));

        List<ResMigration.ResidenceNode> res_data;

        if (player.hasPermission(adminPermission)) {
            res_data = CacheManager.instance.getResidenceCache().getResidenceData();   // get all residence data
        } else {
            res_data = CacheManager.instance.getResidenceCache().getResidenceData(player.getUniqueId());   // get player's residence data
        }

        if (res_data != null) {
            for (ResMigration.ResidenceNode node : res_data) {
                ChestButton btn = new ChestButton(ChestUserInterface.migrateListCui.residenceItemButton) {
                    @Override
                    public void onClick(ClickType type) {
                        MigrationCommand.migrate(player, node.name, args[0]);
                    }
                };
                btn = btn.setDisplayNameArgs(node.name);
                view = view.addItem(btn);
            }
        }

        if (player.hasPermission(adminPermission)) {
            view.setButton(ChestUserInterface.migrateListCui.migrateAllButton.getSymbol(),
                    new ChestButton(ChestUserInterface.migrateListCui.migrateAllButton) {
                        @Override
                        public void onClick(ClickType type) {
                            MigrationCommand.migrateAll(player);
                        }
                    }
            );
        }

        view.setButton(ChestUserInterface.migrateListCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.migrateListCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        MainMenu.show(player, "1");
                    }
                }
        );

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        if (!Configuration.residenceMigration) {
            Notification.error(sender, Language.migrationCommandText.notEnabled);
            return;
        }

        Notification.info(sender, ChestUserInterface.migrateListCui.title);

        Notification.info(sender, MigrationCommand.migrateAll.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, MigrationCommand.migrateAll.getDescription());
        Notification.info(sender, MigrationCommand.migrate.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, MigrationCommand.migrate.getDescription());

        List<ResMigration.ResidenceNode> res_data = CacheManager.instance.getResidenceCache().getResidenceData();
        int page = toIntegrity(args[0], 1);
        Triple<Integer, Integer, Integer> pageInfo = pageUtil(page, 15, res_data.size());
        for (int i = pageInfo.getLeft(); i < pageInfo.getMiddle(); i++) {
            ResMigration.ResidenceNode node = res_data.get(i);
            Notification.info(sender, "§6📋 §f{0} §7(§b{1}§7) ", node.name, node.ownerName);
        }

        Notification.info(sender, Language.consoleText.pageInfo, page, pageInfo.getRight(), res_data.size());
    }
}
