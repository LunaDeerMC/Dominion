package cn.lunadeer.dominion.uis.dominion;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.cache.DominionNode;
import cn.lunadeer.dominion.commands.DominionOperateCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.uis.AllDominion;
import cn.lunadeer.dominion.uis.MainMenu;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestListView;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import cn.lunadeer.dominion.utils.stui.ListView;
import cn.lunadeer.dominion.utils.stui.ViewStyles;
import cn.lunadeer.dominion.utils.stui.components.Line;
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.managers.TeleportManager.teleportToDominion;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;

public class DominionList extends AbstractUI {

    public static void show(CommandSender sender, String pageStr) {
        new DominionList().displayByPreference(sender, pageStr);
    }

    public static SecondaryCommand list = new SecondaryCommand("list", List.of(
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.dominionList) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                show(sender, getArgumentValue(0));
            } catch (Exception e) {
                Notification.error(sender, e.getMessage());
            }
        }
    }.needPermission(defaultPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class DominionListTuiText extends ConfigurationPart {
        public String title = "Your Dominions";
        public String button = "DOMINIONS";
        public String description = "List all of your dominions.";
        public String deleteButton = "DELETE";
        public String adminSection = "Your admin dominions section.";
        public String serverSection = "Server {0} dominions section.";
    }

    public static ListViewButton button(CommandSender sender) {
        return (ListViewButton) new ListViewButton(TextUserInterface.dominionListTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(sender, pageStr);
            }
        }.needPermission(defaultPermission);
    }

    public static List<Line> BuildTreeLines(CommandSender sender, List<DominionNode> dominionTree, Integer depth) {
        List<Line> lines = new ArrayList<>();
        StringBuilder prefix = new StringBuilder();
        prefix.append(" | ".repeat(Math.max(0, depth)));
        for (DominionNode node : dominionTree) {
            TextComponent manage = DominionManage.button(sender, node.getDominion().getName()).green().build();
            TextComponent delete = new FunctionalButton(TextUserInterface.dominionListTuiText.deleteButton) {
                @Override
                public void function() {
                    DominionOperateCommand.delete(sender, node.getDominion().getName(), "");
                }
            }.red().build();
            TextComponent tp = new FunctionalButton("TP") {
                @Override
                public void function() {
                    if (sender instanceof Player player)
                        teleportToDominion(player, node.getDominion());
                }
            }.build();
            Line line = Line.create().append(delete).append(manage).append(tp).append(prefix + node.getDominion().getName());
            lines.add(line);
            lines.addAll(BuildTreeLines(sender, node.getChildren(), depth + 1));
        }
        return lines;
    }

    @Override
    protected void showTUI(Player player, String... args) throws Exception {
        int page = toIntegrity(args[0], 1);
        ListView view = ListView.create(10, button(player));

        view.title(TextUserInterface.dominionListTuiText.title);
        view.navigator(Line.create()
                .append(MainMenu.button(player).build())
                .append(TextUserInterface.dominionListTuiText.button));
        List<DominionNode> dominionNodes = CacheManager.instance.getCache().getDominionCache().getPlayerDominionNodes(player.getUniqueId());
        // Show dominions on current server
        view.addLines(BuildTreeLines(player, dominionNodes, 0));
        // Show admin dominions on this server
        List<DominionDTO> admin_dominions = CacheManager.instance.getCache().getDominionCache().getPlayerAdminDominionDTOs(player.getUniqueId());
        if (!admin_dominions.isEmpty()) {
            view.add(Line.create().append(""));
            view.add(Line.create().append(Component.text(TextUserInterface.dominionListTuiText.adminSection, ViewStyles.PRIMARY)));
            for (DominionDTO dominion : admin_dominions) {
                TextComponent manage = DominionManage.button(player, dominion.getName()).build();
                view.add(Line.create().append(manage).append(dominion.getName()));
            }
        }
        view.showOn(player, page);
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class DominionListCui extends ConfigurationPart {
        public String title = "§6✦ §b§lDominions List §6✦";
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

        public ButtonConfiguration ownDominionButton = ButtonConfiguration.createMaterial(
                'i', Material.GRASS_BLOCK, "§2👑 §a{0}",
                List.of(
                        "§7This is §ayour dominion§7.",
                        "§8You have full control here!",
                        "",
                        "§a▶ Left Click: manage",
                        "§8  Settings, members, permissions",
                        "§b▶ Right Click: teleport",
                        "",
                        "§7Status: §2Owner"
                )
        );

        public ButtonConfiguration adminDominionButton = ButtonConfiguration.createMaterial(
                'i', Material.DIRT_PATH, "§9⚡ §b{0}",
                List.of(
                        "§7Owner: §e{0}",
                        "§8You have admin access here.",
                        "",
                        "§b▶ Left Click: manage",
                        "§8  Help the owner manage this dominion",
                        "§b▶ Right Click: teleport",
                        "",
                        "§7Status: §9Administrator"
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
        view.setTitle(ChestUserInterface.dominionListCui.title);
        view.applyListConfiguration(ChestUserInterface.dominionListCui.listConfiguration, toIntegrity(args[0]));

        List<DominionDTO> own = CacheManager.instance.getCache().getDominionCache().getPlayerOwnDominionDTOs(player.getUniqueId());
        for (DominionDTO dominion : own) {
            ChestButton btn = new ChestButton(ChestUserInterface.dominionListCui.ownDominionButton) {
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
            view = view.addItem(btn);
        }

        List<DominionDTO> admin = CacheManager.instance.getCache().getDominionCache().getPlayerAdminDominionDTOs(player.getUniqueId());
        for (DominionDTO dominion : admin) {
            ChestButton btn = new ChestButton(ChestUserInterface.dominionListCui.adminDominionButton) {
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

        view.setButton(ChestUserInterface.dominionListCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.dominionListCui.backButton) {
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
        AllDominion.show(sender, args[0]);
    }

}
