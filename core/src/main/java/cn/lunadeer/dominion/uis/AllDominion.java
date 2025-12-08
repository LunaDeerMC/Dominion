package cn.lunadeer.dominion.uis;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.inputters.SearchPlayerDominionInputter;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.uis.dominion.DominionManage;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestListView;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import cn.lunadeer.dominion.utils.stui.ListView;
import cn.lunadeer.dominion.utils.stui.components.Line;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static cn.lunadeer.dominion.managers.TeleportManager.teleportToDominion;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.uis.dominion.DominionList.BuildTreeLines;
import static cn.lunadeer.dominion.utils.Misc.pageUtil;

public class AllDominion extends AbstractUI {

    public static void show(CommandSender sender, String pageStr) {
        new AllDominion().displayByPreference(sender, pageStr);
    }

    public static SecondaryCommand listAll = new SecondaryCommand("list_all", List.of(
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.listAll) {
        @Override
        public void executeHandler(CommandSender sender) {
            show(sender, getArgumentValue(0));
        }
    }.needPermission(adminPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class AllDominionTuiText extends ConfigurationPart {
        public String title = "All Dominions";
        public String description = "List all dominions.";
        public String button = "LIST ALL";
    }

    public static ListViewButton button(CommandSender sender) {
        return (ListViewButton) new ListViewButton(TextUserInterface.allDominionTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(sender, pageStr);
            }
        }.needPermission(adminPermission);
    }

    @Override
    protected void showTUI(Player sender, String... args) {
        int page = toIntegrity(args[0], 1);
        ListView view = ListView.create(10, button(sender));

        view.title(TextUserInterface.allDominionTuiText.title);
        view.navigator(Line.create()
                .append(MainMenu.button(sender).build())
                .append(TextUserInterface.allDominionTuiText.button));
        view.addLines(BuildTreeLines(sender, CacheManager.instance.getCache().getDominionCache().getAllDominionNodes(), 0));
        view.showOn(sender, page);
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class AllDominionCui extends ConfigurationPart {
        public String title = "§6✦ §c§lAll Dominions §6✦";
        public String searchResultsTitle = "§6✦ §c§lDominions of {0} §6✦";
        public ListViewConfiguration listConfiguration = new ListViewConfiguration(
                'i',
                List.of(
                        "<#####s##",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "p#######n"
                )
        );

        public ButtonConfiguration dominionItemButton = ButtonConfiguration.createMaterial(
                'i', Material.FILLED_MAP, "§6🏰 §e{0}",
                List.of(
                        "§7Owner: §b{0}",
                        "",
                        "§a▶ Left Click: manage",
                        "§8  View settings, members & permissions",
                        "§b▶ Right Click: teleport",
                        "",
                        "§7Status: §aActive",
                        "§8Admin access granted"
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

        public ButtonConfiguration searchButton = ButtonConfiguration.createMaterial(
                's', Material.COMPASS,
                "§e🔍 §6Search Player Dominions",
                List.of(
                        "§7Search for dominions owned by",
                        "§7a specific player.",
                        "",
                        "§6▶ Click to search",
                        "",
                        "§8Enter player name to find their dominions!"
                )
        );
    }

    @Override
    protected void showCUI(Player player, String... args) {
        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(ChestUserInterface.allDominionCui.title);
        view.applyListConfiguration(ChestUserInterface.allDominionCui.listConfiguration, toIntegrity(args[0]));

        List<DominionDTO> dominions = CacheManager.instance.getCache().getDominionCache().getAllDominions();
        for (DominionDTO dominion : dominions) {
            ChestButton btn = new ChestButton(ChestUserInterface.allDominionCui.dominionItemButton) {
                @Override
                public void onClick(ClickType type) {
                    if (type.isLeftClick()) {
                        DominionManage.show(player, dominion.getName(), "1");
                    } else if (type.isRightClick()) {
                        teleportToDominion(player, dominion);
                    }
                }
            };
            btn = btn.setDisplayNameArgs(dominion.getName());
            btn = btn.setLoreArgs(List.of(dominion.getOwnerDTO().getLastKnownName()));
            view = view.addItem(btn);
        }

        view.setButton(ChestUserInterface.allDominionCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.allDominionCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        MainMenu.show(player, "1");
                    }
                }
        );

        view.setButton(ChestUserInterface.allDominionCui.searchButton.getSymbol(),
                new ChestButton(ChestUserInterface.allDominionCui.searchButton) {
                    @Override
                    public void onClick(ClickType type) {
                        player.closeInventory();
                        SearchPlayerDominionInputter.createOn(player);
                    }
                }
        );

        view.open();
    }

    /**
     * Show search results for dominions owned by a specific player.
     *
     * @param sender     the command sender
     * @param playerName the name of the player to search for
     * @param pageStr    the page number
     */
    public static void showSearchResults(CommandSender sender, String playerName, String pageStr) {
        if (sender instanceof Player player) {
            showSearchResultsCUI(player, playerName, pageStr);
        } else {
            showSearchResultsConsole(sender, playerName, pageStr);
        }
    }

    private static void showSearchResultsCUI(Player player, String playerName, String pageStr) {
        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(ChestUserInterface.allDominionCui.searchResultsTitle.replace("{0}", playerName));
        view.applyListConfiguration(ChestUserInterface.allDominionCui.listConfiguration, toIntegrity(pageStr));

        // Find player by name
        PlayerDTO targetPlayer = CacheManager.instance.getPlayer(playerName);
        List<DominionDTO> dominions;
        if (targetPlayer != null) {
            dominions = CacheManager.instance.getPlayerOwnDominionDTOs(targetPlayer.getUuid());
        } else {
            dominions = new ArrayList<>();
        }

        if (dominions.isEmpty()) {
            Notification.warn(player, Language.searchPlayerDominionInputterText.noResults);
        }

        for (DominionDTO dominion : dominions) {
            ChestButton btn = new ChestButton(ChestUserInterface.allDominionCui.dominionItemButton) {
                @Override
                public void onClick(ClickType type) {
                    if (type.isLeftClick()) {
                        DominionManage.show(player, dominion.getName(), "1");
                    } else if (type.isRightClick()) {
                        teleportToDominion(player, dominion);
                    }
                }
            };
            btn = btn.setDisplayNameArgs(dominion.getName());
            btn = btn.setLoreArgs(List.of(dominion.getOwnerDTO().getLastKnownName()));
            view = view.addItem(btn);
        }

        view.setButton(ChestUserInterface.allDominionCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.allDominionCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        show(player, "1");
                    }
                }
        );

        view.setButton(ChestUserInterface.allDominionCui.searchButton.getSymbol(),
                new ChestButton(ChestUserInterface.allDominionCui.searchButton) {
                    @Override
                    public void onClick(ClickType type) {
                        player.closeInventory();
                        SearchPlayerDominionInputter.createOn(player);
                    }
                }
        );

        view.open();
    }

    private static void showSearchResultsConsole(CommandSender sender, String playerName, String pageStr) {
        Notification.info(sender, ChestUserInterface.allDominionCui.searchResultsTitle.replace("{0}", playerName));

        // Find player by name
        PlayerDTO targetPlayer = CacheManager.instance.getPlayer(playerName);
        List<DominionDTO> dominions;
        if (targetPlayer != null) {
            dominions = CacheManager.instance.getPlayerOwnDominionDTOs(targetPlayer.getUuid());
        } else {
            dominions = new ArrayList<>();
        }

        if (dominions.isEmpty()) {
            Notification.warn(sender, Language.searchPlayerDominionInputterText.noResults);
            return;
        }

        int page = toIntegrity(pageStr, 1);
        Triple<Integer, Integer, Integer> pageInfo = pageUtil(page, 15, dominions.size());
        for (int i = pageInfo.getLeft(); i < pageInfo.getMiddle(); i++) {
            DominionDTO dominion = dominions.get(i);
            String ownerName = dominion.getOwnerDTO().getLastKnownName();
            Notification.info(sender, "§6▶ §e{0} §7(§b{1}§7) ", dominion.getName(), ownerName);
        }
        Notification.info(sender, Language.consoleText.pageInfo, page, pageInfo.getRight(), dominions.size());
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        Notification.info(sender, ChestUserInterface.allDominionCui.title);
        // command
        Notification.info(sender, DominionManage.manage.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, DominionManage.manage.getDescription());
        // item
        List<DominionDTO> dominions = CacheManager.instance.getCache().getDominionCache().getAllDominions();
        int page = toIntegrity(args[0], 1);
        Triple<Integer, Integer, Integer> pageInfo = pageUtil(page, 15, dominions.size());
        for (int i = pageInfo.getLeft(); i < pageInfo.getMiddle(); i++) {
            DominionDTO dominion = dominions.get(i);
            String ownerName = dominion.getOwnerDTO().getLastKnownName();
            Notification.info(sender, "§6▶ §e{0} §7(§b{1}§7) ", dominion.getName(), ownerName);
        }
        // page info
        Notification.info(sender, Language.consoleText.pageInfo, page, pageInfo.getRight(), dominions.size());
    }
}
