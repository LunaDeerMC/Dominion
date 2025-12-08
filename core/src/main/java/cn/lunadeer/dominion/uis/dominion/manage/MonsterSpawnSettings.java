package cn.lunadeer.dominion.uis.dominion.manage;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
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
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.ClickType;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Asserts.assertDominionAdmin;
import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.utils.Misc.*;

/**
 * UI for detailed monster spawn settings.
 * Allows users to control which SpawnReasons should be blocked.
 */
public class MonsterSpawnSettings extends AbstractUI {

    /**
     * SpawnReason items that can be configured by users.
     * Each item contains: SpawnReason, Material for icon, default blocked status
     */
    public static class SpawnReasonItem {
        public final CreatureSpawnEvent.SpawnReason reason;
        public final Material material;
        public final boolean defaultBlocked;

        public SpawnReasonItem(CreatureSpawnEvent.SpawnReason reason, Material material, boolean defaultBlocked) {
            this.reason = reason;
            this.material = material;
            this.defaultBlocked = defaultBlocked;
        }

        public String getDisplayName() {
            return Language.spawnReasonText.getDisplayName(reason);
        }

        public String getDescription() {
            return Language.spawnReasonText.getDescription(reason);
        }
    }

    /**
     * All configurable spawn reasons with their display info.
     * Default blocked reasons are marked with defaultBlocked=true.
     */
    public static final List<SpawnReasonItem> SPAWN_REASON_ITEMS = List.of(
            // Natural spawns (default blocked)
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.NATURAL, Material.COAL, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.SPAWNER, Material.SPAWNER, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.CHUNK_GEN, Material.GRASS_BLOCK, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.JOCKEY, Material.SPIDER_EYE, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.PATROL, Material.CROSSBOW, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.RAID, Material.BELL, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.REINFORCEMENTS, Material.ROTTEN_FLESH, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION, Material.ZOMBIE_HEAD, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.NETHER_PORTAL, Material.OBSIDIAN, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.SILVERFISH_BLOCK, Material.INFESTED_STONE, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.TRAP, Material.SKELETON_SKULL, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.ENDER_PEARL, Material.ENDER_PEARL, true),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.SPELL, Material.TOTEM_OF_UNDYING, true),

            // Player-initiated spawns (default allowed)
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG, Material.ZOMBIE_SPAWN_EGG, false),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.DISPENSE_EGG, Material.DISPENSER, false),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.COMMAND, Material.COMMAND_BLOCK, false),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.CUSTOM, Material.PAPER, false),

            // Conversion spawns (default allowed)
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.INFECTION, Material.GOLDEN_APPLE, false),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.DROWNED, Material.TRIDENT, false),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.LIGHTNING, Material.LIGHTNING_ROD, false),
            new SpawnReasonItem(CreatureSpawnEvent.SpawnReason.CURED, Material.SPLASH_POTION, false)
    );

    /**
     * Get the set of blocked spawn reasons for a dominion.
     * If settings is empty, returns the default blocked reasons.
     */
    public static Set<CreatureSpawnEvent.SpawnReason> getBlockedReasons(DominionDTO dominion) {
        String settings = dominion.getMonsterSpawnSettings();
        if (settings == null || settings.isEmpty()) {
            // Return default blocked reasons
            Set<CreatureSpawnEvent.SpawnReason> defaults = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
            for (SpawnReasonItem item : SPAWN_REASON_ITEMS) {
                if (item.defaultBlocked) {
                    defaults.add(item.reason);
                }
            }
            return defaults;
        }

        // Parse the settings string
        Set<CreatureSpawnEvent.SpawnReason> blocked = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
        for (String reasonName : settings.split(",")) {
            reasonName = reasonName.trim();
            if (!reasonName.isEmpty()) {
                try {
                    blocked.add(CreatureSpawnEvent.SpawnReason.valueOf(reasonName));
                } catch (IllegalArgumentException ignored) {
                    // Invalid reason name, skip
                }
            }
        }
        return blocked;
    }

    /**
     * Check if a specific spawn reason is blocked for a dominion.
     */
    public static boolean isReasonBlocked(DominionDTO dominion, CreatureSpawnEvent.SpawnReason reason) {
        return getBlockedReasons(dominion).contains(reason);
    }

    /**
     * Toggle a spawn reason's blocked status for a dominion.
     */
    public static void toggleReason(Player player, String dominionName, String reasonName, String pageStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            assertDominionAdmin(player, dominion);

            CreatureSpawnEvent.SpawnReason reason = CreatureSpawnEvent.SpawnReason.valueOf(reasonName);
            Set<CreatureSpawnEvent.SpawnReason> blocked = getBlockedReasons(dominion);

            if (blocked.contains(reason)) {
                blocked.remove(reason);
            } else {
                blocked.add(reason);
            }

            // Convert to settings string
            StringBuilder sb = new StringBuilder();
            for (CreatureSpawnEvent.SpawnReason r : blocked) {
                if (sb.length() > 0) sb.append(",");
                sb.append(r.name());
            }

            dominion.setMonsterSpawnSettings(sb.toString());
            show(player, dominionName, pageStr);
        } catch (SQLException e) {
            Notification.error(player, "Failed to update monster spawn settings: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            Notification.error(player, "Invalid spawn reason: " + reasonName);
        }
    }

    public static void show(CommandSender sender, String dominionName, String pageStr) {
        new MonsterSpawnSettings().displayByPreference(sender, dominionName, pageStr);
    }

    public static SecondaryCommand command = new SecondaryCommand("monster_spawn_settings", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.OptionalPageArgument()
    ), Language.uiCommandsDescription.monsterSpawnSettings) {
        @Override
        public void executeHandler(CommandSender sender) {
            show(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ TUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class MonsterSpawnSettingsTuiText extends ConfigurationPart {
        public String title = "{0} Monster Spawn Settings";
        public String button = "SPAWN SETTINGS";
        public String description = "Configure which monster spawn reasons to block.";
    }

    public static ListViewButton button(CommandSender sender, String dominionName) {
        return (ListViewButton) new ListViewButton(TextUserInterface.monsterSpawnSettingsTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(sender, dominionName, pageStr);
            }
        }.needPermission(defaultPermission);
    }

    @Override
    protected void showTUI(Player player, String... args) {
        String dominionName = args[0];
        DominionDTO dominion = toDominionDTO(dominionName);
        assertDominionAdmin(player, dominion);
        int page = toIntegrity(args[1]);

        Set<CreatureSpawnEvent.SpawnReason> blocked = getBlockedReasons(dominion);

        ListView view = ListView.create(10, button(player, dominionName));
        view.title(formatString(TextUserInterface.monsterSpawnSettingsTuiText.title, dominion.getName()))
                .navigator(Line.create()
                        .append(MainMenu.button(player).build())
                        .append(DominionList.button(player).build())
                        .append(DominionManage.button(player, dominionName).build())
                        .append(EnvFlags.button(player, dominionName).build())
                        .append(TextUserInterface.monsterSpawnSettingsTuiText.button));

        for (SpawnReasonItem item : SPAWN_REASON_ITEMS) {
            boolean isBlocked = blocked.contains(item.reason);
            if (isBlocked) {
                view.add(Line.create()
                        .append(new FunctionalButton("☑") {
                            @Override
                            public void function() {
                                toggleReason(player, dominionName, item.reason.name(), String.valueOf(page));
                            }
                        }.needPermission(defaultPermission).green().build())
                        .append(Component.text(item.getDisplayName() + " §c[BLOCKED]").hoverEvent(Component.text(item.getDescription())))
                );
            } else {
                view.add(Line.create()
                        .append(new FunctionalButton("☐") {
                            @Override
                            public void function() {
                                toggleReason(player, dominionName, item.reason.name(), String.valueOf(page));
                            }
                        }.needPermission(defaultPermission).red().build())
                        .append(Component.text(item.getDisplayName() + " §a[ALLOWED]").hoverEvent(Component.text(item.getDescription())))
                );
            }
        }
        view.showOn(player, page);
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ TUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class MonsterSpawnSettingsCui extends ConfigurationPart {
        public String title = "§6✦ §4§lMonster Spawn Settings §6✦";
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
                "§c« Back to Environment Settings",
                List.of(
                        "§7Return to the environment",
                        "§7settings menu.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public String reasonItemName = "§6⚙️ §e{0}";
        public String reasonItemStateBlocked = "§c§l✗ BLOCKED";
        public String reasonItemStateAllowed = "§a§l✓ ALLOWED";
        public List<String> reasonItemLore = List.of(
                "§7Status: {0}",
                "",
                "§7Description:",
                "§f{1}",
                "",
                "§e▶ Click to toggle",
                "§8Blocked = monsters won't spawn this way"
        );
    }

    @Override
    protected void showCUI(Player player, String... args) {
        DominionDTO dominion = toDominionDTO(args[0]);
        assertDominionAdmin(player, dominion);

        Set<CreatureSpawnEvent.SpawnReason> blocked = getBlockedReasons(dominion);

        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(formatString(ChestUserInterface.monsterSpawnSettingsCui.title, dominion.getName()));
        view.applyListConfiguration(ChestUserInterface.monsterSpawnSettingsCui.listConfiguration, toIntegrity(args[1]));

        view.setButton(ChestUserInterface.monsterSpawnSettingsCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.monsterSpawnSettingsCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        EnvFlags.show(player, dominion.getName(), "1");
                    }
                }
        );

        for (int i = 0; i < SPAWN_REASON_ITEMS.size(); i++) {
            SpawnReasonItem item = SPAWN_REASON_ITEMS.get(i);
            Integer page = (int) Math.ceil((double) (i + 1) / view.getPageSize());
            boolean isBlocked = blocked.contains(item.reason);
            String stateText = isBlocked ? ChestUserInterface.monsterSpawnSettingsCui.reasonItemStateBlocked : ChestUserInterface.monsterSpawnSettingsCui.reasonItemStateAllowed;
            String itemName = formatString(ChestUserInterface.monsterSpawnSettingsCui.reasonItemName, item.getDisplayName());
            List<String> itemLore = formatStringList(ChestUserInterface.monsterSpawnSettingsCui.reasonItemLore, stateText, item.getDescription());

            ButtonConfiguration btnConfig = ButtonConfiguration.createMaterial(
                    ChestUserInterface.monsterSpawnSettingsCui.listConfiguration.itemSymbol.charAt(0),
                    item.material,
                    itemName,
                    itemLore
            );
            view.addItem(new ChestButton(btnConfig) {
                @Override
                public void onClick(ClickType type) {
                    toggleReason(player, dominion.getName(), item.reason.name(), page.toString());
                }
            });
        }

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
        Notification.info(sender, ChestUserInterface.monsterSpawnSettingsCui.title, args[0]);

        DominionDTO dominion = toDominionDTO(args[0]);
        Set<CreatureSpawnEvent.SpawnReason> blocked = getBlockedReasons(dominion);

        int page = toIntegrity(args[1], 1);
        Triple<Integer, Integer, Integer> pageInfo = pageUtil(page, 8, SPAWN_REASON_ITEMS.size());
        for (int i = pageInfo.getLeft(); i < pageInfo.getMiddle(); i++) {
            SpawnReasonItem item = SPAWN_REASON_ITEMS.get(i);
            boolean isBlocked = blocked.contains(item.reason);
            String stateText = isBlocked ? ChestUserInterface.monsterSpawnSettingsCui.reasonItemStateBlocked : ChestUserInterface.monsterSpawnSettingsCui.reasonItemStateAllowed;
            String itemName = formatString(ChestUserInterface.monsterSpawnSettingsCui.reasonItemName, item.getDisplayName());
            Notification.info(sender, "§6▶ {0} §7(§b{1}§7)", itemName, item.reason.name());
            Notification.info(sender, "§6  \t{0}\t&7{1}", stateText, item.getDescription());
        }

        Notification.info(sender, Language.consoleText.pageInfo, page, pageInfo.getRight(), SPAWN_REASON_ITEMS.size());
    }
}