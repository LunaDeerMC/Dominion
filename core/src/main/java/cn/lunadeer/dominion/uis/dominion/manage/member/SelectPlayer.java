package cn.lunadeer.dominion.uis.dominion.manage.member;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.commands.MemberCommand;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.inputters.SearchPlayerInputter;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestListView;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

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
