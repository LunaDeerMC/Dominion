package cn.lunadeer.dominion.uis;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.misc.Converts;
import cn.lunadeer.dominion.uis.dominion.DominionManage;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestListView;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import cn.lunadeer.dominion.utils.stui.ListView;
import cn.lunadeer.dominion.utils.stui.components.Line;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static cn.lunadeer.dominion.managers.TeleportManager.teleportToDominion;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.uis.dominion.DominionList.BuildTreeLines;
import static cn.lunadeer.dominion.utils.Misc.pageUtil;

public class AllDominionOfPlayer extends AbstractUI {

    public static void show(CommandSender sender, String playerNameStr, String pageStr) {
        new AllDominionOfPlayer().displayByPreference(sender, playerNameStr, pageStr);
    }

    public static SecondaryCommand listAll = new SecondaryCommand("list_all_of", List.of(
            new CommandArguments.RequiredPlayerArgument(),
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.listAllOfDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            show(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(adminPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class AllDominionOfPlayerTuiText extends ConfigurationPart {
        public String title = "All Dominions of {0}";
        public String button = "LIST ALL OF";
    }

    public static ListViewButton button(CommandSender sender, String playerNameStr) {
        return (ListViewButton) new ListViewButton(TextUserInterface.allDominionOfPlayerTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(sender, playerNameStr, pageStr);
            }
        }.needPermission(adminPermission);
    }

    @Override
    protected void showTUI(Player sender, String... args) {
        PlayerDTO playerDTO = Converts.toPlayerDTO(args[0]);
        int page = toIntegrity(args[1], 1);
        ListView view = ListView.create(10, button(sender, playerDTO.getLastKnownName()));

        view.title(TextUserInterface.allDominionOfPlayerTuiText.title);
        view.navigator(Line.create()
                .append(MainMenu.button(sender).build()));
        view.addLines(BuildTreeLines(sender, CacheManager.instance.getCache().getDominionCache().getPlayerDominionNodes(playerDTO.getUuid()), 0));
        view.showOn(sender, page);
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class AllDominionOfPlayerCui extends ConfigurationPart {
        public String title = "§6✦ §c§lAll Dominions Of {0} §6✦";
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
    }

    @Override
    protected void showCUI(Player player, String... args) {
        PlayerDTO playerDTO = Converts.toPlayerDTO(args[0]);
        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(ChestUserInterface.allDominionOfPlayerCui.title);
        view.applyListConfiguration(ChestUserInterface.allDominionOfPlayerCui.listConfiguration, toIntegrity(args[1]));

        List<DominionDTO> dominions = DominionAPI.getInstance().getAllDominionsOfPlayer(playerDTO.getUuid());
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

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        PlayerDTO playerDTO = Converts.toPlayerDTO(args[0]);
        Notification.info(sender, ChestUserInterface.allDominionOfPlayerCui.title, playerDTO.getLastKnownName());
        // command
        Notification.info(sender, DominionManage.manage.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, DominionManage.manage.getDescription());
        // item
        List<DominionDTO> dominions = DominionAPI.getInstance().getAllDominionsOfPlayer(playerDTO.getUuid());
        int page = toIntegrity(args[1], 1);
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
