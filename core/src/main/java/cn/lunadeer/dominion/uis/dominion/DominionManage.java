package cn.lunadeer.dominion.uis.dominion;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.commands.CopyCommand;
import cn.lunadeer.dominion.commands.DominionOperateCommand;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.inputters.EditMessageInputter;
import cn.lunadeer.dominion.inputters.RenameDominionInputter;
import cn.lunadeer.dominion.inputters.SetMapColorInputter;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.uis.MainMenu;
import cn.lunadeer.dominion.uis.dominion.copy.CopyMenu;
import cn.lunadeer.dominion.uis.dominion.manage.EnvFlags;
import cn.lunadeer.dominion.uis.dominion.manage.GuestFlags;
import cn.lunadeer.dominion.uis.dominion.manage.Info;
import cn.lunadeer.dominion.uis.dominion.manage.SetSize;
import cn.lunadeer.dominion.uis.dominion.manage.group.GroupList;
import cn.lunadeer.dominion.uis.dominion.manage.member.MemberList;
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
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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

public class DominionManage extends AbstractUI {

    public static void show(CommandSender sender, String dominionName, String pageStr) {
        new DominionManage().displayByPreference(sender, dominionName, pageStr);
    }

    public static SecondaryCommand manage = new SecondaryCommand("manage", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.dominionManage) {
        @Override
        public void executeHandler(CommandSender sender) {
            show(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class DominionManageTuiText extends ConfigurationPart {
        public String title = "Manage {0}";
        public String button = "MANAGE";
        public String setTpButton = "SET TP";
        public String setTpDescription = "Set your current location as tp location.";
    }

    public static ListViewButton button(CommandSender sender, String dominionName) {
        return (ListViewButton) new ListViewButton(TextUserInterface.dominionManageTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(sender, dominionName, pageStr);
            }
        }.needPermission(defaultPermission);
    }

    @Override
    protected void showTUI(Player player, String... args) {
        DominionDTO dominion = toDominionDTO(args[0]);
        assertDominionAdmin(player, dominion);
        int page = toIntegrity(args[1], 1);

        Line size_info = Line.create()
                .append(Info.button(player, dominion.getName()).build())
                .append(TextUserInterface.sizeInfoTuiText.description);
        Line env_info = Line.create()
                .append(EnvFlags.button(player, dominion.getName()).build())
                .append(TextUserInterface.envSettingTuiText.description);
        Line flag_info = Line.create()
                .append(GuestFlags.button(player, dominion.getName()).build())
                .append(TextUserInterface.guestSettingTuiText.description);
        Line member_list = Line.create()
                .append(MemberList.button(player, dominion.getName()).build())
                .append(TextUserInterface.memberListTuiText.description);
        Line group_list = Line.create()
                .append(GroupList.button(player, dominion.getName()).build())
                .append(TextUserInterface.groupListTuiText.description);
        Line set_tp = Line.create()
                .append(new FunctionalButton(TextUserInterface.dominionManageTuiText.setTpButton) {
                    @Override
                    public void function() {
                        DominionOperateCommand.setTp(player, dominion.getName());
                    }
                }.build())
                .append(TextUserInterface.dominionManageTuiText.setTpDescription);
        Line rename = Line.create()
                .append(RenameDominionInputter.createTuiButtonOn(player, dominion.getName()).needPermission(defaultPermission).build())
                .append(Language.renameDominionInputterText.description);
        Line enter_msg = Line.create()
                .append(EditMessageInputter.createEnterTuiButtonOn(player, dominion.getName()).needPermission(defaultPermission).build())
                .append(Language.editMessageInputterText.enterDescription);
        Line leave_msg = Line.create()
                .append(EditMessageInputter.createLeaveTuiButtonOn(player, dominion.getName()).needPermission(defaultPermission).build())
                .append(Language.editMessageInputterText.leaveDescription);
        Line map_color = Line.create()
                .append(SetMapColorInputter.createTuiButtonOn(player, dominion.getName()).build())
                .append(Component.text(Language.setMapColorInputterText.description)
                        .append(Component.text(dominion.getColor(),
                                TextColor.color(dominion.getColorR(), dominion.getColorG(), dominion.getColorB()))));
        Line copy_menu = Line.create()
                .append(CopyMenu.button(player, dominion.getName()).build())
                .append(TextUserInterface.copyMenuTuiText.description);
        ListView view = ListView.create(10, button(player, dominion.getName()));
        view.title(formatString(TextUserInterface.dominionManageTuiText.title, dominion.getName()))
                .navigator(Line.create()
                        .append(MainMenu.button(player).build())
                        .append(DominionList.button(player).build())
                        .append(dominion.getName()))
                .add(size_info)
                .add(env_info)
                .add(flag_info)
                .add(member_list)
                .add(group_list)
                .add(set_tp)
                .add(rename)
                .add(enter_msg)
                .add(leave_msg);
        if (Configuration.webMapRenderer.blueMap || Configuration.webMapRenderer.dynmap) {
            view.add(map_color);
        }
        view.add(copy_menu);
        view.showOn(player, page);
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class DominionManageCui extends ConfigurationPart {
        public String title = "§6✦ §a§lManage {0} §6✦";
        public ListViewConfiguration listConfiguration = new ListViewConfiguration(
                'i',
                List.of(
                        "<###I####",
                        "#i#i#i#i#",
                        "#i#i#i#i#",
                        "p#######n"
                )
        );

        public ButtonConfiguration backButton = ButtonConfiguration.createMaterial(
                '<', Material.RED_STAINED_GLASS_PANE,
                "§c« Back to Dominion List",
                List.of(
                        "§7Return to your dominion list",
                        "§8to manage other dominions.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public ButtonConfiguration dominionInfoButton = ButtonConfiguration.createMaterial(
                'I', Material.GRASS_BLOCK,
                "§6📊 §e§lDominion Overview",
                List.of(
                        "",
                        "§f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                        "§b▪ §7Size:     §f{0}",
                        "§b▪ §7Height:   §f{1}",
                        "§b▪ §7Square:   §f{2} §7blocks²",
                        "§b▪ §7Volume:   §f{3} §7blocks³",
                        "§b▪ §7Location: §f{4}",
                        "§f━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                        "",
                        "§a▶ §2Click §7- Resize dominion",
                        "",
                        "§8💡 Manage your dominion's size."
                )
        );

        public ButtonConfiguration envSettingButton = ButtonConfiguration.createMaterial(
                'i', Material.COMPASS,
                "§2🌍 §aEnvironment Settings",
                List.of(
                        "§7Configure environment rules",
                        "§7for this dominion.",
                        "",
                        "§a▶ Click to configure",
                        "",
                        "§8Weather, time, mob spawning..."
                )
        );

        public ButtonConfiguration guestSettingButton = ButtonConfiguration.createMaterial(
                'i', Material.OAK_DOOR,
                "§e🚪 §6Guest Permissions",
                List.of(
                        "§7Set permissions for visitors",
                        "§7who aren't members.",
                        "",
                        "§6▶ Click to configure",
                        "",
                        "§8Build, interact, use items..."
                )
        );

        public ButtonConfiguration memberListButton = ButtonConfiguration.createMaterial(
                'i', Material.PLAYER_HEAD,
                "§b👥 §3Member Management",
                List.of(
                        "§7View and manage dominion",
                        "§7members and their roles.",
                        "",
                        "§3▶ Click to manage members",
                        "",
                        "§8Add, remove, set permissions..."
                )
        );

        public ButtonConfiguration groupListButton = ButtonConfiguration.createMaterial(
                'i', Material.CHEST,
                "§d📦 §5Group Management",
                List.of(
                        "§7Create and manage member",
                        "§7groups with custom permissions.",
                        "",
                        "§5▶ Click to manage groups",
                        "",
                        "§8Create ranks, set titles..."
                )
        );

        public ButtonConfiguration setTpButton = ButtonConfiguration.createMaterial(
                'i', Material.ENDER_PEARL,
                "§9🎯 §bSet Teleport Point",
                List.of(
                        "§7Set your current location",
                        "§7as the dominion's TP point.",
                        "",
                        "§b▶ Click to set location",
                        "",
                        "§8Members can teleport here!"
                )
        );

        public ButtonConfiguration renameButton = ButtonConfiguration.createMaterial(
                'i', Material.NAME_TAG,
                "§e✏️ §6Rename Dominion",
                List.of(
                        "§7Change the name of",
                        "§7this dominion.",
                        "",
                        "§6▶ Click to rename",
                        "",
                        "§8Choose a memorable name!"
                )
        );

        public ButtonConfiguration enterMessageButton = ButtonConfiguration.createMaterial(
                'i', Material.WRITABLE_BOOK,
                "§a📝 §2Enter Message",
                List.of(
                        "§7Edit the welcome message",
                        "§7shown when players enter.",
                        "",
                        "§2▶ Click to edit message",
                        "",
                        "§8Greet your visitors!"
                )
        );

        public ButtonConfiguration leaveMessageButton = ButtonConfiguration.createMaterial(
                'i', Material.BOOK,
                "§c📄 §4Leave Message",
                List.of(
                        "§7Edit the farewell message",
                        "§7shown when players leave.",
                        "",
                        "§4▶ Click to edit message",
                        "",
                        "§8Say goodbye in style!"
                )
        );

        public ButtonConfiguration setMapColorButton = ButtonConfiguration.createMaterial(
                'i', Material.PAINTING,
                "§6🎨 §eMap Color",
                List.of(
                        "§7Set the display color for",
                        "§7this dominion on the map.",
                        "",
                        "§e▶ Click to choose color",
                        "",
                        "§8Make it stand out!"
                )
        );

        public ButtonConfiguration copyMenuButton = ButtonConfiguration.createMaterial(
                'i', Material.GLASS,
                "§b📋 §3Copy Settings",
                List.of(
                        "§7Copy permissions and settings",
                        "§7to other dominions.",
                        "",
                        "§3▶ Click to access copy menu",
                        "",
                        "§8Save time with templates!"
                )
        );

        public ButtonConfiguration deleteButton = ButtonConfiguration.createMaterial(
                'i', Material.RED_DYE,
                "§c💀 §4Delete Dominion",
                List.of(
                        "§c§lDANGER ZONE",
                        "§7Permanently delete this",
                        "§7dominion and all its data.",
                        "",
                        "§4▶ Click to delete",
                        "",
                        "§c⚠ This cannot be undone!"
                )
        );
    }

    @Override
    protected void showCUI(Player player, String... args) {
        DominionDTO dominion = toDominionDTO(args[0]);
        assertDominionAdmin(player, dominion);

        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(formatString(ChestUserInterface.dominionManageCui.title, dominion.getName()));
        view.applyListConfiguration(ChestUserInterface.dominionManageCui.listConfiguration, toIntegrity(args[1]));

        view.setButton(ChestUserInterface.dominionManageCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.dominionManageCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionList.show(player, "1");
                    }
                }
        );

        view.setButton(ChestUserInterface.dominionManageCui.dominionInfoButton.getSymbol(),
                new ChestButton(ChestUserInterface.dominionManageCui.dominionInfoButton) {
                    @Override
                    public void onClick(ClickType type) {
                        SetSize.show(player, dominion.getName());
                    }
                }.setLoreArgs(
                        dominion.getCuboid().xLength() + " x " + dominion.getCuboid().yLength() + " x " + dominion.getCuboid().zLength(),
                        dominion.getCuboid().y1() + " ~ " + dominion.getCuboid().y2(),
                        dominion.getCuboid().getSquare(),
                        dominion.getCuboid().getVolume(),
                        dominion.getTpLocation().getWorld().getName() + ":" +
                                dominion.getTpLocation().getBlockX() + "," +
                                dominion.getTpLocation().getBlockY() + "," +
                                dominion.getTpLocation().getBlockZ()
                ));

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.envSettingButton) {
            @Override
            public void onClick(ClickType type) {
                EnvFlags.show(player, dominion.getName(), "1");
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.guestSettingButton) {
            @Override
            public void onClick(ClickType type) {
                GuestFlags.show(player, dominion.getName(), "1");
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.memberListButton) {
            @Override
            public void onClick(ClickType type) {
                MemberList.show(player, dominion.getName(), "1");
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.groupListButton) {
            @Override
            public void onClick(ClickType type) {
                GroupList.show(player, dominion.getName(), "1");
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.setTpButton) {
            @Override
            public void onClick(ClickType type) {
                DominionOperateCommand.setTp(player, dominion.getName());
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.renameButton) {
            @Override
            public void onClick(ClickType type) {
                RenameDominionInputter.createOn(player, dominion.getName());
                view.close();
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.enterMessageButton) {
            @Override
            public void onClick(ClickType type) {
                EditMessageInputter.createEnterOn(player, dominion.getName());
                view.close();
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.leaveMessageButton) {
            @Override
            public void onClick(ClickType type) {
                EditMessageInputter.createLeaveOn(player, dominion.getName());
                view.close();
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.setMapColorButton) {
            @Override
            public void onClick(ClickType type) {
                SetMapColorInputter.createOn(player, dominion.getName());
                view.close();
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.copyMenuButton) {
            @Override
            public void onClick(ClickType type) {
                CopyMenu.show(player, dominion.getName(), "1");
            }
        });

        view.addItem(new ChestButton(ChestUserInterface.dominionManageCui.deleteButton) {
            @Override
            public void onClick(ClickType type) {
                DominionOperateCommand.delete(player, dominion.getName(), "");
                view.close();
            }
        });

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        DominionDTO dominion = toDominionDTO(args[0]);

        Notification.info(sender, ChestUserInterface.dominionManageCui.title, dominion.getName());

        Notification.info(sender, EnvFlags.flags.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, EnvFlags.flags.getDescription());
        Notification.info(sender, GuestFlags.flags.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, GuestFlags.flags.getDescription());
        Notification.info(sender, MemberList.list.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, MemberList.list.getDescription());
        Notification.info(sender, GroupList.list.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, GroupList.list.getDescription());
        Notification.info(sender, DominionOperateCommand.rename.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, DominionOperateCommand.rename.getDescription());
        Notification.info(sender, DominionOperateCommand.resize.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, DominionOperateCommand.resize.getDescription());
        Notification.info(sender, DominionOperateCommand.setMessage.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, DominionOperateCommand.setMessage.getDescription());
        Notification.info(sender, CopyCommand.copyEnvironmentCommand.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, CopyCommand.copyEnvironmentCommand.getDescription());
        Notification.info(sender, CopyCommand.copyGuestCommand.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, CopyCommand.copyGuestCommand.getDescription());
        Notification.info(sender, CopyCommand.copyMemberCommand.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, CopyCommand.copyMemberCommand.getDescription());
        Notification.info(sender, CopyCommand.copyGroupCommand.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, CopyCommand.copyGroupCommand.getDescription());
    }
}
