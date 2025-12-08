package cn.lunadeer.dominion.uis.dominion.manage;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.commands.DominionFlagCommand;
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
import cn.lunadeer.dominion.utils.stui.components.buttons.FunctionalButton;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Asserts.assertDominionAdmin;
import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.utils.Misc.*;

public class EnvFlags extends AbstractUI {

    public static void show(CommandSender sender, String dominionName, String pageStr) {
        new EnvFlags().displayByPreference(sender, dominionName, pageStr);
    }

    public static SecondaryCommand flags = new SecondaryCommand("env_flags", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.environmentFlags) {
        @Override
        public void executeHandler(CommandSender sender) {
            show(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class EnvSettingTuiText extends ConfigurationPart {
        public String title = "{0} Env Setting";
        public String button = "ENV SET";
        public String description = "Set environment of dominion.";
    }

    public static ListViewButton button(CommandSender sender, String dominionName) {
        return (ListViewButton) new ListViewButton(TextUserInterface.envSettingTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(sender, dominionName, pageStr);
            }
        }.needPermission(defaultPermission);
    }

    @Override
    protected void showTUI(Player player, String... args) {
        String dominionName = args[0];
        String pageStr = args.length > 1 ? args[1] : "1";
        DominionDTO dominion = toDominionDTO(dominionName);
        assertDominionAdmin(player, dominion);
        int page = toIntegrity(pageStr);

        ListView view = ListView.create(10, button(player, dominionName));
        view.title(formatString(TextUserInterface.envSettingTuiText.title, dominion.getName()))
                .navigator(Line.create()
                        .append(MainMenu.button(player).build())
                        .append(DominionList.button(player).build())
                        .append(DominionManage.button(player, dominionName).build())
                        .append(TextUserInterface.envSettingTuiText.button));
        for (EnvFlag flag : Flags.getAllEnvFlagsEnable()) {
            if (dominion.getEnvFlagValue(flag)) {
                view.add(Line.create()
                        .append(new FunctionalButton("☑") {
                            @Override
                            public void function() {
                                DominionFlagCommand.setEnv(player, dominionName, flag.getFlagName(), "false", pageStr);
                            }
                        }.needPermission(defaultPermission).green().build())
                        .append(Component.text(flag.getDisplayName()).hoverEvent(Component.text(flag.getDescription())))
                );
            } else {
                view.add(Line.create()
                        .append(new FunctionalButton("☐") {
                            @Override
                            public void function() {
                                DominionFlagCommand.setEnv(player, dominionName, flag.getFlagName(), "true", pageStr);
                            }
                        }.needPermission(defaultPermission).red().build())
                        .append(Component.text(flag.getDisplayName()).hoverEvent(Component.text(flag.getDescription())))
                );
            }
        }
        view.showOn(player, page);

    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class EnvSettingCui extends ConfigurationPart {
        public String title = "§6✦ §2§lEnvironment of {0} §6✦";
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

        public String flagItemName = "§6⚙️ §e{0}";
        public String flagItemStateTrue = "§a§l✓ ENABLED";
        public String flagItemStateFalse = "§c§l✗ DISABLED";
        public List<String> flagItemLore = List.of(
                "§7Status: {0}",
                "",
                "§7Description:",
                "§f{1}",
                "§f{2}",
                "",
                "§e▶ Click to toggle this setting",
                "§8Changes take effect immediately"
        );

        public String rightClickForDetailedSettings = "§b▶ Right-click for detailed settings";
    }

    @Override
    protected void showCUI(Player player, String... args) {
        DominionDTO dominion = toDominionDTO(args[0]);
        assertDominionAdmin(player, dominion);

        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(formatString(ChestUserInterface.envSettingCui.title, dominion.getName()));
        view.applyListConfiguration(ChestUserInterface.envSettingCui.listConfiguration, toIntegrity(args[1]));

        view.setButton(ChestUserInterface.envSettingCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.envSettingCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        DominionManage.show(player, dominion.getName(), "1");
                    }
                }
        );

        for (int i = 0; i < Flags.getAllEnvFlagsEnable().size(); i++) {
            EnvFlag flag = Flags.getAllEnvFlagsEnable().get(i);
            Integer page = (int) Math.ceil((double) (i + 1) / view.getPageSize());
            String flagState = dominion.getEnvFlagValue(flag) ? ChestUserInterface.envSettingCui.flagItemStateTrue : ChestUserInterface.envSettingCui.flagItemStateFalse;
            String flagName = formatString(ChestUserInterface.envSettingCui.flagItemName, flag.getDisplayName());
            List<String> descriptions = foldLore2Line(flag.getDescription(), 30);
            List<String> flagLore;
            if (flag.equals(Flags.MONSTER_SPAWN)) {
                flagLore = formatStringList(ChestUserInterface.envSettingCui.flagItemLore, flagState, descriptions.get(0), descriptions.get(1));
                flagLore.add("");
                flagLore.add(ChestUserInterface.envSettingCui.rightClickForDetailedSettings);
            } else {
                flagLore = formatStringList(ChestUserInterface.envSettingCui.flagItemLore, flagState, descriptions.get(0), descriptions.get(1));
            }
            ButtonConfiguration btnConfig = ButtonConfiguration.createMaterial(
                    ChestUserInterface.envSettingCui.listConfiguration.itemSymbol.charAt(0),
                    flag.getMaterial(),
                    flagName,
                    flagLore
            );
            view.addItem(new ChestButton(btnConfig) {
                @Override
                public void onClick(ClickType type) {
                    if (flag.equals(Flags.MONSTER_SPAWN) && type == ClickType.RIGHT) {
                        MonsterSpawnSettings.show(player, dominion.getName(), "1");
                        return;
                    }
                    DominionFlagCommand.setEnv(player, dominion.getName(), flag.getFlagName(), String.valueOf(!dominion.getEnvFlagValue(flag)), page.toString());
                }
            });
        }

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        Notification.info(sender, ChestUserInterface.envSettingCui.title, args[0]);

        Notification.info(sender, DominionFlagCommand.SetEnvFlag.getUsage());
        Notification.info(sender, Language.consoleText.descPrefix, DominionFlagCommand.SetEnvFlag.getDescription());

        DominionDTO dominion = toDominionDTO(args[0]);
        int page = toIntegrity(args[1], 1);
        Triple<Integer, Integer, Integer> pageInfo = pageUtil(page, 8, Flags.getAllEnvFlagsEnable().size());
        for (int i = pageInfo.getLeft(); i < pageInfo.getMiddle(); i++) {
            EnvFlag flag = Flags.getAllEnvFlagsEnable().get(i);
            String flagState = dominion.getEnvFlagValue(flag) ? ChestUserInterface.envSettingCui.flagItemStateTrue : ChestUserInterface.envSettingCui.flagItemStateFalse;
            String flagName = formatString(ChestUserInterface.envSettingCui.flagItemName, flag.getDisplayName());
            Notification.info(sender, "§6▶ {0} §7(§b{1}§7)", flagName, flag.getFlagName());
            Notification.info(sender, "§6  \t{0}\t&7{1}", flagState, flag.getDescription());
        }

        Notification.info(sender, Language.consoleText.pageInfo, page, pageInfo.getRight(), Flags.getAllEnvFlagsEnable().size());
    }

}
