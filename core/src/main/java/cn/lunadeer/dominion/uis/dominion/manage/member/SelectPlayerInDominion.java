package cn.lunadeer.dominion.uis.dominion.manage.member;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.commands.MemberCommand;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Asserts.assertDominionAdmin;
import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.utils.Misc.formatString;

/**
 * GUI for selecting players who are currently inside the dominion.
 * This provides a convenient way to add members who are physically present in the dominion.
 */
public class SelectPlayerInDominion extends AbstractUI {

    public static void show(CommandSender sender, String dominionName, String pageStr) {
        new SelectPlayerInDominion().displayByPreference(sender, dominionName, pageStr);
    }

    /**
     * Gets a list of online players who are currently inside the specified dominion.
     *
     * @param dominion The dominion to check
     * @return List of players inside the dominion
     */
    private static List<Player> getPlayersInDominion(DominionDTO dominion) {
        List<Player> playersInDominion = new ArrayList<>();
        World world = dominion.getWorld();
        if (world == null) {
            return playersInDominion;
        }

        CuboidDTO cuboid = dominion.getCuboid();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            Location loc = player.getLocation();
            if (cuboid.contain(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                playersInDominion.add(player);
            }
        }
        return playersInDominion;
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class SelectPlayerInDominionTuiText extends ConfigurationPart {
        public String title = "Players In Dominion";
        public String button = "NEARBY PLAYERS";
        public String description = "Select from players currently inside this dominion.";
        public String back = "BACK";
        public String noPlayers = "No players are currently inside this dominion.";
    }

    public static ListViewButton button(Player player, String dominionName) {
        return (ListViewButton) new ListViewButton(TextUserInterface.selectPlayerInDominionTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(player, dominionName, pageStr);
            }
        }.needPermission(defaultPermission).setHoverText(TextUserInterface.selectPlayerInDominionTuiText.description);
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
                .append(SelectPlayer.button(player, dominionName).setText(TextUserInterface.selectPlayerInDominionTuiText.back).build());
        view.title(TextUserInterface.selectPlayerInDominionTuiText.title).subtitle(sub);

        List<Player> playersInDominion = getPlayersInDominion(dominion);
        List<UUID> members = dominion.getMembers().stream()
                .map(MemberDTO::getPlayerUUID)
                .toList();

        if (playersInDominion.isEmpty()) {
            view.add(Line.create().append(TextUserInterface.selectPlayerInDominionTuiText.noPlayers));
        } else {
            for (Player p : playersInDominion) {
                if (members.contains(p.getUniqueId())) continue; // Skip if player is already a member
                if (p.getUniqueId().equals(dominion.getOwner())) continue; // Skip the owner
                view.add(Line.create().
                        append(new FunctionalButton(p.getName()) {
                            @Override
                            public void function() {
                                MemberCommand.addMember(player, dominionName, p.getName());
                            }
                        }.needPermission(defaultPermission).build()));
            }
        }
        view.showOn(player, page);
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class SelectPlayerInDominionCui extends ConfigurationPart {
        public String title = "§6✦ §5§lPlayers In {0} §6✦";
        public ListViewConfiguration listConfiguration = new ListViewConfiguration(
                'i',
                List.of(
                        "<########",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "p#######n"
                )
        );

        public ButtonConfiguration backButton = ButtonConfiguration.createMaterial(
                '<', Material.RED_STAINED_GLASS_PANE,
                "§c« Back to Player Selection",
                List.of(
                        "§7Return to the full player",
                        "§7selection menu.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public String playerButtonName = "§a➕ §2{0}";
        public List<String> playerButtonLore = List.of(
                "§7This player is currently",
                "§7inside your dominion.",
                "",
                "§7Player: §e{0}",
                "",
                "§2▶ Click to add as member"
        );

        public String noPlayersMessage = "§7No players are currently inside this dominion.";
    }

    @Override
    protected void showCUI(Player player, String... args) throws Exception {
        String dominionName = args[0];

        DominionDTO dominion = toDominionDTO(dominionName);
        assertDominionAdmin(player, dominion);

        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(formatString(ChestUserInterface.selectPlayerInDominionCui.title, dominion.getName()));
        view.applyListConfiguration(ChestUserInterface.selectPlayerInDominionCui.listConfiguration, toIntegrity(args[1]));

        view.setButton(ChestUserInterface.selectPlayerInDominionCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.selectPlayerInDominionCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        SelectPlayer.show(player, dominionName, "1");
                    }
                }
        );

        List<Player> playersInDominion = getPlayersInDominion(dominion);
        List<UUID> members = dominion.getMembers().stream()
                .map(MemberDTO::getPlayerUUID)
                .toList();

        for (Player p : playersInDominion) {
            if (members.contains(p.getUniqueId())) continue;
            if (p.getUniqueId().equals(dominion.getOwner())) continue;
            
            ButtonConfiguration playerButtonConfig = ButtonConfiguration.createHeadByName(
                    ChestUserInterface.selectPlayerInDominionCui.listConfiguration.itemSymbol.charAt(0),
                    p.getName(),
                    ChestUserInterface.selectPlayerInDominionCui.playerButtonName,
                    ChestUserInterface.selectPlayerInDominionCui.playerButtonLore
            );

            ChestButton playerChest = new ChestButton(playerButtonConfig) {
                @Override
                public void onClick(ClickType type) {
                    MemberCommand.addMember(player, dominionName, p.getName());
                }
            }.setDisplayNameArgs(p.getName()).setLoreArgs(p.getName());
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