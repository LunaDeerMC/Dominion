package cn.lunadeer.dominion.uis.dominion.manage.member;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.commands.MemberCommand;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.inputters.SearchPlayerInputter;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestListView;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import cn.lunadeer.dominion.utils.stui.ListView;
import cn.lunadeer.dominion.utils.stui.components.Line;
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Asserts.assertDominionAdmin;
import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.utils.Misc.formatString;

public class SelectPlayer extends AbstractUI {

    public static void show(CommandSender sender, String dominionName, String pageStr) {
        new SelectPlayer().displayByPreference(sender, dominionName, pageStr);
    }

    /**
     * Show search results for fuzzy player search
     */
    public static void showSearchResults(CommandSender sender, String dominionName, String searchTerm, String pageStr) {
        new SelectPlayer().displaySearchResultsByPreference(sender, dominionName, searchTerm, pageStr);
    }

    private void displaySearchResultsByPreference(CommandSender sender, String dominionName, String searchTerm, String pageStr) {
        if (sender instanceof Player player) {
            try {
                PlayerDTO playerDTO = CacheManager.instance.getPlayer(player.getUniqueId());
                if (playerDTO == null) {
                    showSearchResultsTUI(player, dominionName, searchTerm, pageStr);
                    return;
                }
                if (player.getUniqueId().toString().startsWith("00000000")) {
                    showSearchResultsCUI(player, dominionName, searchTerm, pageStr);
                    return;
                }
                if (PlayerDTO.UI_TYPE.valueOf(Configuration.defaultUiType).equals(PlayerDTO.UI_TYPE.BY_PLAYER)) {
                    if (playerDTO.getUiPreference().equals(PlayerDTO.UI_TYPE.CUI)) {
                        showSearchResultsCUI(player, dominionName, searchTerm, pageStr);
                    } else {
                        showSearchResultsTUI(player, dominionName, searchTerm, pageStr);
                    }
                } else {
                    PlayerDTO.UI_TYPE type = PlayerDTO.UI_TYPE.valueOf(Configuration.defaultUiType);
                    if (type.equals(PlayerDTO.UI_TYPE.CUI)) {
                        showSearchResultsCUI(player, dominionName, searchTerm, pageStr);
                    } else {
                        showSearchResultsTUI(player, dominionName, searchTerm, pageStr);
                    }
                }
            } catch (Exception e) {
                Notification.error(player, e.getMessage());
            }
        }
    }

    private void showSearchResultsTUI(Player player, String dominionName, String searchTerm, String pageStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            assertDominionAdmin(player, dominion);
            int page = toIntegrity(pageStr);

            List<PlayerDTO> searchResults = PlayerDOO.fuzzySearch(searchTerm);
            
            if (searchResults.isEmpty()) {
                Notification.warn(player, Language.searchPlayerInputterText.noResults);
                show(player, dominionName, "1");
                return;
            }

            ListView view = ListView.create(10, searchResultButton(player, dominionName, searchTerm));
            Line sub = Line.create()
                    .append(SearchPlayerInputter.createTuiButtonOn(player, dominionName).build())
                    .append(button(player, dominionName).setText(TextUserInterface.selectPlayerTuiText.back).build());
            view.title(TextUserInterface.searchResultsTuiText.title).subtitle(sub);

            List<UUID> members = dominion.getMembers().stream()
                    .map(MemberDTO::getPlayerUUID)
                    .toList();
            
            for (PlayerDTO p : searchResults) {
                if (members.contains(p.getUuid())) continue;
                view.add(Line.create().
                        append(new FunctionalButton(p.getLastKnownName()) {
                            @Override
                            public void function() {
                                MemberCommand.addMember(player, dominionName, p.getLastKnownName());
                            }
                        }.needPermission(defaultPermission).build()));
            }
            view.showOn(player, page);
        } catch (Exception e) {
            Notification.error(player, e.getMessage());
        }
    }

    private void showSearchResultsCUI(Player player, String dominionName, String searchTerm, String pageStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            assertDominionAdmin(player, dominion);

            List<PlayerDTO> searchResults = PlayerDOO.fuzzySearch(searchTerm);
            
            if (searchResults.isEmpty()) {
                Notification.warn(player, Language.searchPlayerInputterText.noResults);
                show(player, dominionName, "1");
                return;
            }

            ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
            view.setTitle(formatString(ChestUserInterface.searchResultsCui.title, searchTerm));
            view.applyListConfiguration(ChestUserInterface.searchResultsCui.listConfiguration, toIntegrity(pageStr));

            view.setButton(ChestUserInterface.searchResultsCui.backButton.getSymbol(),
                    new ChestButton(ChestUserInterface.searchResultsCui.backButton) {
                        @Override
                        public void onClick(ClickType type) {
                            show(player, dominionName, "1");
                        }
                    }
            );

            view.setButton(ChestUserInterface.searchResultsCui.searchAgainButton.getSymbol(),
                    new ChestButton(ChestUserInterface.searchResultsCui.searchAgainButton) {
                        @Override
                        public void onClick(ClickType type) {
                            SearchPlayerInputter.createOn(player, dominionName);
                            view.close();
                        }
                    }
            );

            List<UUID> members = dominion.getMembers().stream()
                    .map(MemberDTO::getPlayerUUID)
                    .toList();
            
            for (PlayerDTO p : searchResults) {
                if (members.contains(p.getUuid())) continue;
                ButtonConfiguration playerButtonConfig = ButtonConfiguration.createHeadByName(
                        ChestUserInterface.searchResultsCui.listConfiguration.itemSymbol.charAt(0),
                        p.getLastKnownName(),
                        ChestUserInterface.searchResultsCui.playerButtonName,
                        ChestUserInterface.searchResultsCui.playerButtonLore
                );

                ChestButton playerChest = new ChestButton(playerButtonConfig) {
                    @Override
                    public void onClick(ClickType type) {
                        MemberCommand.addMember(player, dominionName, p.getLastKnownName());
                    }
                }.setDisplayNameArgs(p.getLastKnownName()).setLoreArgs(p.getLastKnownName());
                view.addItem(playerChest);
            }

            view.open();
        } catch (Exception e) {
            Notification.error(player, e.getMessage());
        }
    }

    private static ListViewButton searchResultButton(Player player, String dominionName, String searchTerm) {
        return (ListViewButton) new ListViewButton(TextUserInterface.searchResultsTuiText.button) {
            @Override
            public void function(String pageStr) {
                showSearchResults(player, dominionName, searchTerm, pageStr);
            }
        }.needPermission(defaultPermission).setHoverText(TextUserInterface.searchResultsTuiText.description);
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class SelectPlayerTuiText extends ConfigurationPart {
        public String title = "Select Player";
        public String button = "ADD PLAYER";
        public String description = "Add a player as a member of this dominion.";
        public String back = "BACK";
    }

    public static class SearchResultsTuiText extends ConfigurationPart {
        public String title = "Search Results";
        public String button = "SEARCH RESULTS";
        public String description = "Players matching your search.";
    }

    public static ListViewButton button(Player player, String dominionName) {
        return (ListViewButton) new ListViewButton(TextUserInterface.selectPlayerTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(player, dominionName, pageStr);
            }
        }.needPermission(defaultPermission).setHoverText(TextUserInterface.selectPlayerTuiText.description);
    }

    @Override
    protected void showTUI(Player player, String... args) throws Exception {
        String dominionName = args[0];
        String pageStr = args[1];

        DominionDTO dominion = toDominionDTO(dominionName);
        assertDominionAdmin(player, dominion);
        int page = toIntegrity(pageStr);

        ListView view = ListView.create(10, button(player, dominionName));
        Line sub = Line.create()
                .append(SearchPlayerInputter.createTuiButtonOn(player, dominionName).build())
                .append(MemberList.button(player, dominionName).setText(TextUserInterface.selectPlayerTuiText.back).build());
        view.title(TextUserInterface.selectPlayerTuiText.title).subtitle(sub);

        List<PlayerDTO> players = PlayerDOO.all();
        for (PlayerDTO p : players) {
            view.add(Line.create().
                    append(new FunctionalButton(p.getLastKnownName()) {
                        @Override
                        public void function() {
                            MemberCommand.addMember(player, dominionName, p.getLastKnownName());
                        }
                    }.needPermission(defaultPermission).build()));
        }
        view.showOn(player, page);
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class SelectPlayerCui extends ConfigurationPart {
        public String title = "§6✦ §5§lSelect Player for {0} §6✦";
        public ListViewConfiguration listConfiguration = new ListViewConfiguration(
                'i',
                List.of(
                        "<######s#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "p#######n"
                )
        );

        public ButtonConfiguration backButton = ButtonConfiguration.createMaterial(
                '<', Material.RED_STAINED_GLASS_PANE,
                "§c« Back to Member List",
                List.of(
                        "§7Return to the member list",
                        "§7without adding a player.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public ButtonConfiguration searchButton = ButtonConfiguration.createMaterial(
                's', Material.COMPASS,
                "§e🔍 §6Search Player",
                List.of(
                        "§7Search for a specific player",
                        "§7by entering their username.",
                        "",
                        "§6▶ Click to search",
                        "",
                        "§8Useful for finding offline players!"
                )
        );

        public String playerButtonName = "§a➕ §2{0}";
        public List<String> playerButtonLore = List.of(
                "§7Add this player as a member",
                "§7of your dominion.",
                "",
                "§7Player: §e{0}",
                "",
                "§2▶ Click to add member"
        );
    }

    public static class SearchResultsCui extends ConfigurationPart {
        public String title = "§6✦ §5§lSearch Results: {0} §6✦";
        public ListViewConfiguration listConfiguration = new ListViewConfiguration(
                'i',
                List.of(
                        "<######r#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "p#######n"
                )
        );

        public ButtonConfiguration backButton = ButtonConfiguration.createMaterial(
                '<', Material.RED_STAINED_GLASS_PANE,
                "§c« Back to Player List",
                List.of(
                        "§7Return to the full player list",
                        "§7without selecting a player.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public ButtonConfiguration searchAgainButton = ButtonConfiguration.createMaterial(
                'r', Material.COMPASS,
                "§e🔍 §6Search Again",
                List.of(
                        "§7Search for a different player",
                        "§7by entering another username.",
                        "",
                        "§6▶ Click to search again"
                )
        );

        public String playerButtonName = "§a➕ §2{0}";
        public List<String> playerButtonLore = List.of(
                "§7Add this player as a member",
                "§7of your dominion.",
                "",
                "§7Player: §e{0}",
                "",
                "§2▶ Click to add member"
        );
    }

    @Override
    protected void showCUI(Player player, String... args) throws Exception {
        String dominionName = args[0];

        DominionDTO dominion = toDominionDTO(dominionName);
        assertDominionAdmin(player, dominion);

        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(formatString(ChestUserInterface.selectPlayerCui.title, dominion.getName()));
        view.applyListConfiguration(ChestUserInterface.selectPlayerCui.listConfiguration, toIntegrity(args[1]));

        view.setButton(ChestUserInterface.selectPlayerCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.selectPlayerCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        MemberList.show(player, dominionName, "1");
                    }
                }
        );

        view.setButton(ChestUserInterface.selectPlayerCui.searchButton.getSymbol(),
                new ChestButton(ChestUserInterface.selectPlayerCui.searchButton) {
                    @Override
                    public void onClick(ClickType type) {
                        SearchPlayerInputter.createOn(player, dominionName);
                        view.close();
                    }
                }
        );

        List<PlayerDTO> players = PlayerDOO.all();
        List<UUID> members = dominion.getMembers().stream()
                .map(MemberDTO::getPlayerUUID)
                .toList();
        for (PlayerDTO p : players) {
            if (members.contains(p.getUuid())) continue; // Skip if player is already a member
            ButtonConfiguration playerButtonConfig = ButtonConfiguration.createHeadByName(
                    ChestUserInterface.selectPlayerCui.listConfiguration.itemSymbol.charAt(0),
                    p.getLastKnownName(),
                    ChestUserInterface.selectPlayerCui.playerButtonName,
                    ChestUserInterface.selectPlayerCui.playerButtonLore
            );

            ChestButton playerChest = new ChestButton(playerButtonConfig) {
                @Override
                public void onClick(ClickType type) {
                    MemberCommand.addMember(player, dominionName, p.getLastKnownName());
                }
            }.setDisplayNameArgs(p.getLastKnownName()).setLoreArgs(p.getLastKnownName());
            view.addItem(playerChest);
        }

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
    }
}
