package cn.lunadeer.dominion.uis;

import cn.lunadeer.dominion.commands.AdministratorCommand;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.inputters.CreateDominionInputter;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.uis.dominion.DominionList;
import cn.lunadeer.dominion.uis.template.TemplateList;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.CommandManager;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.ChestView;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;


public class MainMenu extends AbstractUI {

    public static void show(CommandSender sender, String pageStr) {
        new MainMenu().displayByPreference(sender, pageStr);
    }

    public static SecondaryCommand menu = new SecondaryCommand("menu", List.of(
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.mainMenu) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                MainMenu.show(sender, getArgumentValue(0));
            } catch (Exception e) {
                Notification.error(sender, e);
            }
        }
    }.needPermission(defaultPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class MainMenuCui extends ConfigurationPart {
        public String title = "§b✦ §6§lDominion Main Menu §b✦";
        public List<String> adminLayout = List.of(
                "#########",
                "##A#B#C##",
                "##D#E#F##",
                "#########",
                "#########"
        );
        public List<String> userLayout = List.of(
                "#########",
                "##A#B#C##",
                "###D#E###",
                "#########",
                "#########"
        );
        public List<String> statusDisabledLore = List.of(
                "§c✘ §4This feature is currently disabled.",
                "§7Please check back later or contact",
                "§7§oServer Administrators §rfor more info.",
                "",
                "§8§lStatus: §c✘ Disabled by Operator"
        );
        public ButtonConfiguration createButton = ButtonConfiguration.createMaterial(
                'A', Material.NETHER_STAR, "§6✨ §eCreate Dominion §6✨",
                List.of(
                        "§7Start your empire by creating",
                        "§7a new dominion at your location.",
                        "",
                        "§e▶ Click to begin creation",
                        "",
                        "§8Tip: Make sure you're in the",
                        "§8area you want to claim!"
                )
        );
        public ButtonConfiguration listButton = ButtonConfiguration.createMaterial(
                'B', Material.BOOKSHELF, "§b📋 §fManage My Dominions",
                List.of(
                        "§7View and manage all dominions",
                        "§7that you have access to.",
                        "",
                        "§b▶ Click to view list",
                        "",
                        "§8Includes: Your dominions &",
                        "§8dominions you're admin of!"
                )
        );
        public ButtonConfiguration titleButton = ButtonConfiguration.createMaterial(
                'C', Material.NAME_TAG, "§6👑 §eGroup Titles",
                List.of(
                        "§7Browse and equip titles from",
                        "§7groups you're member of.",
                        "",
                        "§e▶ Click to browse titles",
                        "",
                        "§8Show off your rank and",
                        "§8membership status!"
                )
        );
        public ButtonConfiguration templateButton = ButtonConfiguration.createMaterial(
                'D', Material.WRITABLE_BOOK, "§a📝 §fTemplate Manager",
                List.of(
                        "§7Create and manage permission",
                        "§7templates for quick setup.",
                        "",
                        "§a▶ Click to manage templates",
                        "",
                        "§8Save time when setting up",
                        "§8new dominions!"
                )
        );
        public ButtonConfiguration migrateButton = ButtonConfiguration.createMaterial(
                'E', Material.ENDER_PEARL, "§d🔄 §fMigrate from Residence",
                List.of(
                        "§7Convert your existing Residence",
                        "§7plots to Dominion format.",
                        "",
                        "§d▶ Click to start migration",
                        "",
                        "§c⚠ Make sure to backup first!",
                        "§8This process is irreversible."
                )
        );
        public ButtonConfiguration allButton = ButtonConfiguration.createMaterial(
                'F', Material.DIAMOND, "§c💎 §fAll Server Dominions",
                List.of(
                        "§7§lADMIN ONLY§r",
                        "§7View all dominions across",
                        "§7the entire server.",
                        "",
                        "§c▶ Click to view all dominions",
                        "",
                        "§8Perfect for server management",
                        "§8and moderation purposes."
                )
        );

    }

    @Override
    protected void showCUI(Player player, String... args) {
        ChestView view = ChestUserInterfaceManager.getInstance().getViewOf(player).setTitle(ChestUserInterface.mainMenuCui.title);

        if (player.hasPermission(adminPermission)) {
            view.setLayout(ChestUserInterface.mainMenuCui.adminLayout);
        } else {
            view.setLayout(ChestUserInterface.mainMenuCui.userLayout);
        }

        view.setButton(ChestUserInterface.mainMenuCui.createButton.getSymbol(),
                new ChestButton(ChestUserInterface.mainMenuCui.createButton) {
                    @Override
                    public void onClick(ClickType type) {
                        CreateDominionInputter.createOn(player);
                        view.close();
                    }
                }
        );

        view.setButton(ChestUserInterface.mainMenuCui.listButton.getSymbol(),
                new ChestButton(ChestUserInterface.mainMenuCui.listButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionList.show(player, "1");
                    }
                }
        );

        if (!Configuration.groupTitle.enable) {
            ChestUserInterface.mainMenuCui.titleButton.lore = ChestUserInterface.mainMenuCui.statusDisabledLore;
        }

        view.setButton(ChestUserInterface.mainMenuCui.titleButton.getSymbol(),
                new ChestButton(ChestUserInterface.mainMenuCui.titleButton) {
                    @Override
                    public void onClick(ClickType type) {
                        if (Configuration.groupTitle.enable) {
                            TitleList.show(player, "1");
                        }
                    }
                }
        );

        view.setButton(ChestUserInterface.mainMenuCui.templateButton.getSymbol(),
                new ChestButton(ChestUserInterface.mainMenuCui.templateButton) {
                    @Override
                    public void onClick(ClickType type) {
                        TemplateList.show(player, "1");
                    }
                }
        );

        if (!Configuration.residenceMigration) {
            ChestUserInterface.mainMenuCui.migrateButton.lore = ChestUserInterface.mainMenuCui.statusDisabledLore;
        }

        view.setButton(ChestUserInterface.mainMenuCui.migrateButton.getSymbol(),
                new ChestButton(ChestUserInterface.mainMenuCui.migrateButton) {
                    @Override
                    public void onClick(ClickType type) {
                        if (Configuration.residenceMigration) {
                            MigrateList.show(player, "1");
                        }
                    }
                }
        );

        if (player.hasPermission(adminPermission)) {
            view.setButton(ChestUserInterface.mainMenuCui.allButton.getSymbol(),
                    new ChestButton(ChestUserInterface.mainMenuCui.allButton) {
                        @Override
                        public void onClick(ClickType type) {
                            AllDominion.show(player, "1");
                        }
                    }
            );
        }

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        Notification.info(sender, ChestUserInterface.mainMenuCui.title);
        Notification.info(sender, AllDominion.listAll.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, AllDominion.listAll.getDescription());
        Notification.info(sender, MigrateList.migrateList.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, MigrateList.migrateList.getDescription());
        Notification.info(sender, AdministratorCommand.reloadCache.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, AdministratorCommand.reloadCache.getDescription());
        Notification.info(sender, AdministratorCommand.updateLanguage.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, AdministratorCommand.updateLanguage.getDescription());
        Notification.info(sender, CommandManager.getInstance().helpCommand.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, CommandManager.getInstance().helpCommand.getDescription());
    }
}
