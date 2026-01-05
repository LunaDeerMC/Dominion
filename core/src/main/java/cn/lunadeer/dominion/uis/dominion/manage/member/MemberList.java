package cn.lunadeer.dominion.uis.dominion.manage.member;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.commands.MemberCommand;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.uis.MainMenu;
import cn.lunadeer.dominion.uis.dominion.DominionList;
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
import cn.lunadeer.dominion.utils.stui.components.buttons.Button;
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.doos.MemberDOO.selectByDominionId;
import static cn.lunadeer.dominion.misc.Asserts.assertDominionAdmin;
import static cn.lunadeer.dominion.misc.Asserts.assertDominionOwner;
import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.utils.Misc.formatString;
import static cn.lunadeer.dominion.utils.Misc.pageUtil;

public class MemberList extends AbstractUI {

    public static void show(CommandSender sender, String dominionName, String pageStr) {
        new MemberList().displayByPreference(sender, dominionName, pageStr);
    }

    public static SecondaryCommand list = new SecondaryCommand("member_list", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.memberList) {
        @Override
        public void executeHandler(CommandSender sender) {
            show(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class MemberListTuiText extends ConfigurationPart {
        public String title = "{0} Member List";
        public String description = "List of members of this dominion.";
        public String button = "MEMBERS";
        public String remove = "REMOVE";
        public String removeDescription = "Remove this member from this dominion.";

        public String ownerOnly = "Only owner can manage admin member.";
        public String groupOnly = "This member belong to group {0} so you can't manage it separately.";

        public String tagAdmin = "Admin can manage members and groups of this dominion.";
        public String tagNormal = "Normal members.";
        public String tagBan = "Who don't have MOVE privilege.";
        public String tagGroup = "This player belong to a group, you can't manage it separately.";
    }

    public static ListViewButton button(CommandSender sender, String dominionName) {
        return (ListViewButton) new ListViewButton(TextUserInterface.memberListTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(sender, dominionName, pageStr);
            }
        }.needPermission(defaultPermission);
    }

    @Override
    protected void showTUI(Player player, String... args) throws Exception {
        DominionDTO dominion = toDominionDTO(args[0]);
        assertDominionAdmin(player, dominion);
        int page = toIntegrity(args[1], 1);

        ListView view = ListView.create(10, button(player, dominion.getName()));
        view.title(formatString(TextUserInterface.memberListTuiText.title, dominion.getName()));
        view.navigator(
                Line.create()
                        .append(MainMenu.button(player).build())
                        .append(DominionList.button(player).build())
                        .append(DominionManage.button(player, dominion.getName()).build())
                        .append(TextUserInterface.memberListTuiText.button)
        );
        view.add(Line.create()
                .append(SelectPlayer.button(player, dominion.getName()).build())
        );

        // get data from database directly because cache update may not be in time
        List<MemberDTO> members = dominion.getMembers();
        for (MemberDTO member : members) {
            PlayerDTO p_player = member.getPlayer();
            GroupDTO group = CacheManager.instance.getGroup(member.getGroupId());
            Line line = Line.create();
            // Tag
            if (group != null) {
                line.append(groupTag);
            } else if (member.getFlagValue(Flags.ADMIN)) {
                line.append(adminTag);
            } else {
                if (!member.getFlagValue(Flags.MOVE)) {
                    line.append(banTag);
                } else {
                    line.append(normalTag);
                }
            }

            Button prev = MemberFlags.button(player, dominion.getName(), p_player.getLastKnownName()).green();
            Button remove = new FunctionalButton(TextUserInterface.memberListTuiText.remove) {
                @Override
                public void function() {
                    MemberCommand.removeMember(player, dominion.getName(), p_player.getLastKnownName(), String.valueOf(page));
                }
            }.setHoverText(TextUserInterface.memberListTuiText.removeDescription).red();

            boolean disable = false;
            try {
                assertDominionOwner(player, dominion);
            } catch (Exception e) {
                // not owner then the player is admin so he should not remove other admin
                disable = member.getFlagValue(Flags.ADMIN);
            }
            if (disable) {
                prev.setDisabled(TextUserInterface.memberListTuiText.ownerOnly);
                remove.setDisabled(TextUserInterface.memberListTuiText.ownerOnly);
            }
            if (group != null) {
                prev.setDisabled(formatString(TextUserInterface.memberListTuiText.groupOnly, group.getNamePlain()));
            }

            line.append(remove.build());
            line.append(prev.build());
            line.append(p_player.getLastKnownName());
            view.add(line);
        }
        view.showOn(player, page);
    }

    private static final TextComponent adminTag = Component.text("[A]", Style.style(TextColor.color(97, 97, 210)))
            .hoverEvent(Component.text(TextUserInterface.memberListTuiText.tagAdmin));
    private static final TextComponent normalTag = Component.text("[N]", Style.style(TextColor.color(255, 255, 255)))
            .hoverEvent(Component.text(TextUserInterface.memberListTuiText.tagNormal));
    private static final TextComponent banTag = Component.text("[B]", Style.style(TextColor.color(255, 67, 0)))
            .hoverEvent(Component.text(TextUserInterface.memberListTuiText.tagBan));
    private static final TextComponent groupTag = Component.text("[G]", Style.style(TextColor.color(0, 185, 153)))
            .hoverEvent(Component.text(TextUserInterface.memberListTuiText.tagGroup));

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class MemberListCui extends ConfigurationPart {
        public String title = "§6✦ §3§lMember List of {0} §6✦";
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
                "§c« Back to Dominion Management",
                List.of(
                        "§7Return to the dominion",
                        "§7management menu.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public ButtonConfiguration addPlayerButton = ButtonConfiguration.createMaterial(
                'i', Material.LIME_DYE,
                "§a➕ §2Add New Member",
                List.of(
                        "§7Invite a player to join",
                        "§7this dominion as a member.",
                        "",
                        "§2▶ Click to select player",
                        "",
                        "§8Grant access to your dominion!"
                )
        );

        public String normalMember = "§aNormal Member";
        public String adminMember = "§bAdmin Member";
        public String banMember = "§cBanned Member";

        public List<String> playerHeadItemLore = List.of(
                "§e▶ Left Click to manage permissions",
                "§8  Set custom privileges for this player",
                "",
                "§c▶ Right Click to remove",
                "§8  Remove this player from this dominion",
                "",
                "§7Status: {0}"
        );

        public List<String> playerHeadItemLoreDisable = List.of(
                "&cThis player belongs",
                "&cto a group, you can't",
                "&cmanage it separately."
        );
    }

    @Override
    protected void showCUI(Player player, String... args) throws Exception {
        DominionDTO dominion = toDominionDTO(args[0]);
        assertDominionAdmin(player, dominion);
        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(formatString(ChestUserInterface.memberListCui.title, dominion.getName()));
        view.applyListConfiguration(ChestUserInterface.memberListCui.listConfiguration, toIntegrity(args[1]));

        view.setButton(ChestUserInterface.memberListCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.memberListCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionManage.show(player, dominion.getName(), "1");
                    }
                }
        );

        view.addItem(
                new ChestButton(ChestUserInterface.memberListCui.addPlayerButton) {
                    @Override
                    public void onClick(ClickType type) {
                        SelectPlayer.show(player, dominion.getName(), "1");
                    }
                }
        );

        // get data from database directly because cache update may not be in time
        List<MemberDTO> members = new ArrayList<>(selectByDominionId(dominion.getId()));
        for (MemberDTO m : members) {
            ButtonConfiguration item = ButtonConfiguration.createHeadByName(
                    ChestUserInterface.memberListCui.listConfiguration.itemSymbol.charAt(0),
                    m.getPlayer().getLastKnownName(),
                    m.getPlayer().getLastKnownName(),
                    m.getGroupId() == -1 ?
                            ChestUserInterface.memberListCui.playerHeadItemLore : ChestUserInterface.memberListCui.playerHeadItemLoreDisable
            );
            String status = ChestUserInterface.memberListCui.normalMember;
            if (m.getFlagValue(Flags.ADMIN)) {
                status = ChestUserInterface.memberListCui.adminMember;
            } else if (!m.getFlagValue(Flags.MOVE)) {
                status = ChestUserInterface.memberListCui.banMember;
            }
            view.addItem(new ChestButton(item) {
                @Override
                public void onClick(ClickType type) {
                    if (type.isLeftClick()) {
                        if (m.getGroupId() == -1) {
                            MemberFlags.show(player, dominion.getName(), m.getPlayer().getLastKnownName(), "1");
                        }
                    } else if (type.isRightClick()) {
                        MemberCommand.removeMember(player, dominion.getName(), m.getPlayer().getLastKnownName(), String.valueOf(view.getCurrentPage()));
                    }
                }
            }.setLoreArgs(status));
        }

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        Notification.info(sender, ChestUserInterface.memberListCui.title, args[0]);
        // command
        Notification.info(sender, MemberCommand.addMember.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, MemberCommand.addMember.getDescription());
        Notification.info(sender, MemberCommand.removeMember.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, MemberCommand.removeMember.getDescription());
        Notification.info(sender, MemberFlags.flags.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, MemberFlags.flags.getDescription());
        // item
        DominionDTO dominion = toDominionDTO(args[0]);
        int page = toIntegrity(args[1], 1);
        List<MemberDTO> members = new ArrayList<>(selectByDominionId(dominion.getId()));
        Triple<Integer, Integer, Integer> pageInfo = pageUtil(page, 10, members.size());
        for (int i = pageInfo.getLeft(); i < pageInfo.getMiddle(); i++) {
            MemberDTO member = members.get(i);
            PlayerDTO p_player = member.getPlayer();
            StringBuilder line = new StringBuilder();
            if (member.getGroupId() == -1) {
                line.append("§6▶ &a[G] &7");
            } else if (member.getFlagValue(Flags.ADMIN)) {
                line.append("§6▶ &9[A] &f");
            } else {
                if (!member.getFlagValue(Flags.MOVE)) {
                    line.append("§6▶ &c[B] &f");
                } else {
                    line.append("§6▶ &f[N] &f");
                }
            }
            line.append(p_player.getLastKnownName());
            Notification.info(sender, line.toString());
        }
        // page info
        Notification.info(sender, Language.consoleText.pageInfo, page, pageInfo.getRight(), members.size());
    }
}
