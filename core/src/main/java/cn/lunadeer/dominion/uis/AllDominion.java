package cn.lunadeer.dominion.uis;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
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
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static cn.lunadeer.dominion.managers.TeleportManager.teleportToDominion;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
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

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class AllDominionCui extends ConfigurationPart {
        public String title = "§6✦ §c§lAll Dominions §6✦";
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

        view.open();
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
