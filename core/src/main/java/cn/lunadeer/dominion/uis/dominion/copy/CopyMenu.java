package cn.lunadeer.dominion.uis.dominion.copy;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.uis.MainMenu;
import cn.lunadeer.dominion.uis.dominion.DominionList;
import cn.lunadeer.dominion.uis.dominion.DominionManage;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.ChestView;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
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

public class CopyMenu extends AbstractUI {

    public static void show(CommandSender sender, String toDominionName, String pageStr) {
        new CopyMenu().displayByPreference(sender, toDominionName, pageStr);
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class CopyMenuCui extends ConfigurationPart {
        public String title = "§6✦ §a§lCopy Settings §6✦";
        public List<String> layout = List.of(
                "<########",
                "#E#G#M#P#",
                "#########"
        );

        public ButtonConfiguration backButton = ButtonConfiguration.createMaterial(
                '<', Material.RED_STAINED_GLASS_PANE,
                "§c« Back to Dominion Management",
                List.of(
                        "§7Return to dominion management",
                        "§7to access other settings.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public ButtonConfiguration envCopyButton = ButtonConfiguration.createMaterial(
                'E', Material.GRASS_BLOCK,
                "§2🌍 §aEnvironment Copy",
                List.of(
                        "§7Copy environment settings",
                        "§7from another dominion.",
                        "",
                        "§a▶ Click to copy environment",
                        "",
                        "§8Weather, time, mob spawning..."
                )
        );

        public ButtonConfiguration guestCopyButton = ButtonConfiguration.createMaterial(
                'G', Material.OAK_DOOR,
                "§e🚪 §6Guest Copy",
                List.of(
                        "§7Copy guest permissions",
                        "§7from another dominion.",
                        "",
                        "§6▶ Click to copy guest settings",
                        "",
                        "§8Build, interact, use items..."
                )
        );

        public ButtonConfiguration memberCopyButton = ButtonConfiguration.createMaterial(
                'M', Material.PLAYER_HEAD,
                "§b👥 §3Member Copy",
                List.of(
                        "§7Copy member permissions",
                        "§7from another dominion.",
                        "",
                        "§3▶ Click to copy member settings",
                        "",
                        "§8Individual player permissions..."
                )
        );

        public ButtonConfiguration groupCopyButton = ButtonConfiguration.createMaterial(
                'P', Material.CHEST,
                "§d📦 §5Group Copy",
                List.of(
                        "§7Copy group settings",
                        "§7from another dominion.",
                        "",
                        "§5▶ Click to copy group settings",
                        "",
                        "§8Permission groups and roles..."
                )
        );
    }

    @Override
    protected void showCUI(Player player, String... args) throws Exception {
        String toDominionName = args[0];

        DominionDTO dominion = toDominionDTO(toDominionName);
        assertDominionAdmin(player, dominion);

        ChestView view = ChestUserInterfaceManager.getInstance().getViewOf(player);
        view.setTitle(ChestUserInterface.copyMenuCui.title);
        view.setLayout(ChestUserInterface.copyMenuCui.layout);

        view.setButton(ChestUserInterface.copyMenuCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.copyMenuCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionManage.show(player, toDominionName, "1");
                    }
                }
        );

        view.setButton(ChestUserInterface.copyMenuCui.envCopyButton.getSymbol(),
                new ChestButton(ChestUserInterface.copyMenuCui.envCopyButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionCopy.show(player, toDominionName, DominionCopy.CopyType.ENVIRONMENT, "1");
                    }
                }
        );

        view.setButton(ChestUserInterface.copyMenuCui.guestCopyButton.getSymbol(),
                new ChestButton(ChestUserInterface.copyMenuCui.guestCopyButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionCopy.show(player, toDominionName, DominionCopy.CopyType.GUEST, "1");
                    }
                }
        );

        view.setButton(ChestUserInterface.copyMenuCui.memberCopyButton.getSymbol(),
                new ChestButton(ChestUserInterface.copyMenuCui.memberCopyButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionCopy.show(player, toDominionName, DominionCopy.CopyType.MEMBER, "1");
                    }
                }
        );

        view.setButton(ChestUserInterface.copyMenuCui.groupCopyButton.getSymbol(),
                new ChestButton(ChestUserInterface.copyMenuCui.groupCopyButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionCopy.show(player, toDominionName, DominionCopy.CopyType.GROUP, "1");
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
