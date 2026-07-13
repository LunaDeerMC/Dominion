package cn.lunadeer.dominion.uis.dominion.copy;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.commands.CopyCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestListView;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Asserts.assertDominionAdmin;
import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.utils.Misc.formatString;

/**
 * Unified copy functionality for all dominion copy operations.
 * This class eliminates code duplication by providing a single implementation
 * that handles Environment, Guest, Member, and Group copying through an enum-based approach.
 */
public class DominionCopy extends AbstractUI {

    public enum CopyType {
        ENVIRONMENT,
        GUEST,
        MEMBER,
        GROUP
    }

    public static void show(CommandSender sender, String toDominionName, CopyType copyType, String pageStr) {
        new DominionCopy().displayByPreference(sender, toDominionName, copyType.name(), pageStr);
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class DominionCopyCui extends ConfigurationPart {
        public String title = "§6✦ §a§lSelect Dominion to Copy §6✦";

        public ListViewConfiguration listConfiguration = new ListViewConfiguration(
                'i',
                List.of(
                        "<########",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "#p#####n#"
                )
        );

        public ButtonConfiguration backButton = ButtonConfiguration.createMaterial(
                '<', Material.RED_STAINED_GLASS_PANE,
                "§c« Back to Copy Menu",
                List.of(
                        "§7Return to the copy menu",
                        "§7to select a different copy type.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public String itemName = "§a{0}";
        public List<String> itemLore = List.of(
                "§7Copy from this dominion",
                "§7to apply its settings.",
                "",
                "§a▶ Click to select"
        );
    }

    @Override
    protected void showCUI(Player player, String... args) throws Exception {
        String toDominionName = args[0];
        CopyType copyType = CopyType.valueOf(args[1]);
        int page = toIntegrity(args[2]);

        DominionDTO dominion = toDominionDTO(toDominionName);
        assertDominionAdmin(player, dominion);

        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(ChestUserInterface.dominionCopyCui.title);
        view.applyListConfiguration(ChestUserInterface.dominionCopyCui.listConfiguration, page);

        List<DominionDTO> dominions = CacheManager.instance.getPlayerOwnDominionDTOs(player.getUniqueId());
        for (DominionDTO fromDominion : dominions) {
            if (fromDominion.getId().equals(dominion.getId())) continue;

            ButtonConfiguration itemConfig = ButtonConfiguration.createMaterial(
                    ChestUserInterface.dominionCopyCui.listConfiguration.itemSymbol.charAt(0),
                    Material.GRASS_BLOCK,
                    formatString(ChestUserInterface.dominionCopyCui.itemName, fromDominion.getName()),
                    ChestUserInterface.dominionCopyCui.itemLore
            );

            view.addItem(new ChestButton(itemConfig) {
                @Override
                public void onClick(ClickType type) {
                    switch (copyType) {
                        case ENVIRONMENT -> CopyCommand.copyEnvironment(player, fromDominion.getName(), toDominionName);
                        case GUEST -> CopyCommand.copyGuest(player, fromDominion.getName(), toDominionName);
                        case MEMBER -> CopyCommand.copyMember(player, fromDominion.getName(), toDominionName);
                        case GROUP -> CopyCommand.copyGroup(player, fromDominion.getName(), toDominionName);
                    }
                    // Close the inventory after copying
                    player.closeInventory();
                }
            });
        }

        view.setButton(ChestUserInterface.dominionCopyCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.dominionCopyCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        CopyMenu.show(player, toDominionName, "1");
                    }
                }
        );

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        Notification.warn(sender, Language.consoleText.inGameOnly);
    }
}
