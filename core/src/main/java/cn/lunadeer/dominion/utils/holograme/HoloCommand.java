package cn.lunadeer.dominion.utils.holograme;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.Argument;
import cn.lunadeer.dominion.utils.command.CommandManager;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Debug commands for the HoloItem system.
 * <p>
 * These commands are registered under the existing {@code /dominion} command system
 * and provide full control over HoloItem creation, configuration, and management
 * directly from the game chat for testing purposes.
 * <p>
 * <b>All commands require {@code dominion.admin} permission (op).</b>
 * <p>
 * <b>To remove this debug module:</b> Delete this file and remove the
 * {@code new HoloCommand()} line from {@code InitCommands.java}.
 *
 * <h3>Available Commands:</h3>
 * <ul>
 *   <li>{@code /dom holo_create <name>} - Create a HoloItem at your position</li>
 *   <li>{@code /dom holo_remove <name>} - Remove a HoloItem</li>
 *   <li>{@code /dom holo_list} - List all HoloItems</li>
 *   <li>{@code /dom holo_info <name>} - Show HoloItem details</li>
 *   <li>{@code /dom holo_tp <name>} - Move HoloItem to your position</li>
 *   <li>{@code /dom holo_rotate <name> <yaw> <pitch> [roll]} - Rotate HoloItem</li>
 *   <li>{@code /dom holo_show <name>} - Show a hidden HoloItem</li>
 *   <li>{@code /dom holo_hide <name>} - Hide a HoloItem</li>
 *   <li>{@code /dom holo_add_block <name> <element> <material>} - Add a BlockDisplay element</li>
 *   <li>{@code /dom holo_add_item <name> <element> <material>} - Add an ItemDisplay element</li>
 *   <li>{@code /dom holo_rm_el <name> <element>} - Remove an element</li>
 *   <li>{@code /dom holo_el_pos <name> <element> <x> <y> <z>} - Set element offset</li>
 *   <li>{@code /dom holo_el_scale <name> <element> <sx> <sy> <sz>} - Scale an element</li>
 *   <li>{@code /dom holo_el_rotate <name> <element> <yaw> <pitch> [roll]} - Rotate an element</li>
 *   <li>{@code /dom holo_el_glow <name> <element> <true|false>} - Toggle element glow</li>
 *   <li>{@code /dom holo_el_bright <name> <element> <block_light> <sky_light>} - Set brightness</li>
 *   <li>{@code /dom holo_el_trans <name> <element> <tx> <ty> <tz>} - Set translation offset</li>
 *   <li>{@code /dom holo_demo} - Create a demo coordinate axes display</li>
 * </ul>
 */
public class HoloCommand {

    private static final List<SecondaryCommand> registeredCommands = new ArrayList<>();

    public HoloCommand() {
        registerCommands();
    }

    /**
     * Unregister all debug commands and clean up all HoloItems.
     */
    public static void cleanup() {
        for (SecondaryCommand cmd : registeredCommands) {
            CommandManager.unregisterCommand(cmd);
        }
        registeredCommands.clear();
        HoloManager.instance().removeAll();
    }

    // ==================== Helper Methods ====================

    private static SecondaryCommand reg(SecondaryCommand cmd) {
        cmd.needPermission(Dominion.adminPermission).register();
        registeredCommands.add(cmd);
        return cmd;
    }

    private static Player toPlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Notification.error(sender, "[Holo] This command can only be used by a player.");
            return null;
        }
        return player;
    }

    private static HoloItem getItem(CommandSender sender, String name) {
        HoloItem item = HoloManager.instance().get(name);
        if (item == null) {
            Notification.error(sender, "[Holo] HoloItem ''{0}'' not found.", name);
        }
        return item;
    }

    private static HoloElement getElement(CommandSender sender, HoloItem item, String elementName) {
        HoloElement element = item.getElement(elementName);
        if (element == null) {
            Notification.error(sender, "[Holo] Element ''{0}'' not found in HoloItem ''{1}''.", elementName, item.getName());
        }
        return element;
    }

    private static float parseFloat(CommandSender sender, String value, String fieldName) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            Notification.error(sender, "[Holo] Invalid {0}: ''{1}'' is not a number.", fieldName, value);
            return Float.NaN;
        }
    }

    private static int parseInt(CommandSender sender, String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Notification.error(sender, "[Holo] Invalid {0}: ''{1}'' is not an integer.", fieldName, value);
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Suggestion that provides existing HoloItem names.
     */
    private static final Argument HOLO_NAME_ARG = new Argument("name", true,
            (sender, preArgs) -> new ArrayList<>(HoloManager.instance().getNames()));

    /**
     * Create a new Argument for HoloItem name with tab completion.
     */
    private static Argument holoNameArg() {
        return new Argument("name", true,
                (sender, preArgs) -> new ArrayList<>(HoloManager.instance().getNames()));
    }

    /**
     * Create a new Argument for element name with tab completion.
     */
    private static Argument elementNameArg() {
        return new Argument("element", true,
                (sender, preArgs) -> {
                    // preArgs[0] should be the holo item name
                    if (preArgs.length >= 1) {
                        HoloItem item = HoloManager.instance().get(preArgs[0]);
                        if (item != null) {
                            return new ArrayList<>(item.getElementNames());
                        }
                    }
                    return List.of("<element>");
                });
    }

    /**
     * Create a new Argument for material name with tab completion.
     */
    private static Argument materialArg() {
        return new Argument("material", true,
                (sender, preArgs) -> {
                    List<String> suggestions = new ArrayList<>();
                    // Return some common materials
                    suggestions.add("STONE");
                    suggestions.add("RED_CONCRETE");
                    suggestions.add("GREEN_CONCRETE");
                    suggestions.add("BLUE_CONCRETE");
                    suggestions.add("WHITE_CONCRETE");
                    suggestions.add("YELLOW_CONCRETE");
                    suggestions.add("GLASS");
                    suggestions.add("GLOWSTONE");
                    suggestions.add("DIAMOND_BLOCK");
                    suggestions.add("GOLD_BLOCK");
                    suggestions.add("IRON_BLOCK");
                    suggestions.add("ARROW");
                    suggestions.add("DIAMOND_SWORD");
                    suggestions.add("COMPASS");
                    return suggestions;
                });
    }

    // ==================== Command Registration ====================

    private void registerCommands() {
        // /dom holo_create <name>
        reg(new SecondaryCommand("holo_create", List.of(
                new Argument("name", true)
        ), "[Debug] Create a HoloItem at your position") {
            @Override
            public void executeHandler(CommandSender sender) {
                Player player = toPlayer(sender);
                if (player == null) return;
                String name = getArgumentValue(0);
                try {
                    Location loc = player.getLocation();
                    HoloManager.instance().create(name, loc);
                    Notification.info(sender, "[Holo] Created HoloItem ''{0}'' at ({1}, {2}, {3})",
                            name,
                            String.format("%.1f", loc.getX()),
                            String.format("%.1f", loc.getY()),
                            String.format("%.1f", loc.getZ()));
                } catch (IllegalArgumentException e) {
                    Notification.error(sender, "[Holo] " + e.getMessage());
                }
            }
        });

        // /dom holo_remove <name>
        reg(new SecondaryCommand("holo_remove", List.of(
                holoNameArg()
        ), "[Debug] Remove a HoloItem") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                if (HoloManager.instance().get(name) == null) {
                    Notification.error(sender, "[Holo] HoloItem ''{0}'' not found.", name);
                    return;
                }
                HoloManager.instance().remove(name);
                Notification.info(sender, "[Holo] Removed HoloItem ''{0}''.", name);
            }
        });

        // /dom holo_list
        reg(new SecondaryCommand("holo_list",
                "[Debug] List all HoloItems") {
            @Override
            public void executeHandler(CommandSender sender) {
                Set<String> names = HoloManager.instance().getNames();
                if (names.isEmpty()) {
                    Notification.info(sender, "[Holo] No HoloItems exist.");
                    return;
                }
                Notification.info(sender, "[Holo] HoloItems ({0}):", String.valueOf(names.size()));
                for (String name : names) {
                    HoloItem item = HoloManager.instance().get(name);
                    if (item != null) {
                        Notification.info(sender, "  - {0}", item.toString());
                    }
                }
            }
        });

        // /dom holo_info <name>
        reg(new SecondaryCommand("holo_info", List.of(
                holoNameArg()
        ), "[Debug] Show HoloItem details") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                Notification.info(sender, "[Holo] === HoloItem: {0} ===", name);
                Location anchor = item.getAnchor();
                Notification.info(sender, "[Holo] Anchor: ({0}, {1}, {2}) in {3}",
                        String.format("%.2f", anchor.getX()),
                        String.format("%.2f", anchor.getY()),
                        String.format("%.2f", anchor.getZ()),
                        anchor.getWorld() != null ? anchor.getWorld().getName() : "null");
                Notification.info(sender, "[Holo] Visible: {0}, Elements: {1}",
                        String.valueOf(item.isVisible()), String.valueOf(item.getElementCount()));
                for (HoloElement element : item.getElements()) {
                    Notification.info(sender, "[Holo]   {0}", element.toString());
                }
            }
        });

        // /dom holo_tp <name>
        reg(new SecondaryCommand("holo_tp", List.of(
                holoNameArg()
        ), "[Debug] Move HoloItem to your position") {
            @Override
            public void executeHandler(CommandSender sender) {
                Player player = toPlayer(sender);
                if (player == null) return;
                String name = getArgumentValue(0);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                Location loc = player.getLocation();
                item.setPosition(loc);
                Notification.info(sender, "[Holo] Moved ''{0}'' to ({1}, {2}, {3})",
                        name,
                        String.format("%.1f", loc.getX()),
                        String.format("%.1f", loc.getY()),
                        String.format("%.1f", loc.getZ()));
            }
        });

        // /dom holo_rotate <name> <yaw> <pitch> [roll]
        reg(new SecondaryCommand("holo_rotate", List.of(
                holoNameArg(),
                new Argument("yaw", true),
                new Argument("pitch", true),
                new Argument("roll", "0")
        ), "[Debug] Set HoloItem rotation (degrees)") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                float yaw = parseFloat(sender, getArgumentValue(1), "yaw");
                float pitch = parseFloat(sender, getArgumentValue(2), "pitch");
                float roll = parseFloat(sender, getArgumentValue(3), "roll");
                if (Float.isNaN(yaw) || Float.isNaN(pitch) || Float.isNaN(roll)) return;
                item.setRotation(yaw, pitch, roll);
                Notification.info(sender, "[Holo] Rotated ''{0}'' to yaw={1} pitch={2} roll={3}",
                        name,
                        String.format("%.1f", yaw),
                        String.format("%.1f", pitch),
                        String.format("%.1f", roll));
            }
        });

        // /dom holo_show <name>
        reg(new SecondaryCommand("holo_show", List.of(
                holoNameArg()
        ), "[Debug] Show a hidden HoloItem") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                item.show();
                Notification.info(sender, "[Holo] HoloItem ''{0}'' is now visible.", name);
            }
        });

        // /dom holo_hide <name>
        reg(new SecondaryCommand("holo_hide", List.of(
                holoNameArg()
        ), "[Debug] Hide a HoloItem") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                item.hide();
                Notification.info(sender, "[Holo] HoloItem ''{0}'' is now hidden.", name);
            }
        });

        // /dom holo_add_block <name> <element> <material>
        reg(new SecondaryCommand("holo_add_block", List.of(
                holoNameArg(),
                new Argument("element", true),
                materialArg()
        ), "[Debug] Add a BlockDisplay element") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                String materialName = getArgumentValue(2);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isBlock()) {
                    Notification.error(sender, "[Holo] Unknown or non-block material: ''{0}''", materialName);
                    return;
                }
                try {
                    item.addBlockDisplay(elementName, material);
                    Notification.info(sender, "[Holo] Added BlockDisplay ''{0}'' ({1}) to ''{2}''.",
                            elementName, material.name(), name);
                } catch (IllegalArgumentException e) {
                    Notification.error(sender, "[Holo] " + e.getMessage());
                }
            }
        });

        // /dom holo_add_item <name> <element> <material>
        reg(new SecondaryCommand("holo_add_item", List.of(
                holoNameArg(),
                new Argument("element", true),
                materialArg()
        ), "[Debug] Add an ItemDisplay element") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                String materialName = getArgumentValue(2);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    Notification.error(sender, "[Holo] Unknown material: ''{0}''", materialName);
                    return;
                }
                try {
                    item.addItemDisplay(elementName, material);
                    Notification.info(sender, "[Holo] Added ItemDisplay ''{0}'' ({1}) to ''{2}''.",
                            elementName, material.name(), name);
                } catch (IllegalArgumentException e) {
                    Notification.error(sender, "[Holo] " + e.getMessage());
                }
            }
        });

        // /dom holo_rm_el <name> <element>
        reg(new SecondaryCommand("holo_rm_el", List.of(
                holoNameArg(),
                elementNameArg()
        ), "[Debug] Remove an element from a HoloItem") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                if (!item.hasElement(elementName)) {
                    Notification.error(sender, "[Holo] Element ''{0}'' not found in ''{1}''.", elementName, name);
                    return;
                }
                item.removeElement(elementName);
                Notification.info(sender, "[Holo] Removed element ''{0}'' from ''{1}''.", elementName, name);
            }
        });

        // /dom holo_el_pos <name> <element> <x> <y> <z>
        reg(new SecondaryCommand("holo_el_pos", List.of(
                holoNameArg(),
                elementNameArg(),
                new Argument("x", true),
                new Argument("y", true),
                new Argument("z", true)
        ), "[Debug] Set element relative offset") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                HoloElement element = getElement(sender, item, elementName);
                if (element == null) return;
                float x = parseFloat(sender, getArgumentValue(2), "x");
                float y = parseFloat(sender, getArgumentValue(3), "y");
                float z = parseFloat(sender, getArgumentValue(4), "z");
                if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) return;
                element.offset(x, y, z);
                Notification.info(sender, "[Holo] Set ''{0}'' offset to ({1}, {2}, {3})",
                        elementName,
                        String.format("%.2f", x),
                        String.format("%.2f", y),
                        String.format("%.2f", z));
            }
        });

        // /dom holo_el_scale <name> <element> <sx> <sy> <sz>
        reg(new SecondaryCommand("holo_el_scale", List.of(
                holoNameArg(),
                elementNameArg(),
                new Argument("sx", true),
                new Argument("sy", true),
                new Argument("sz", true)
        ), "[Debug] Set element scale") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                HoloElement element = getElement(sender, item, elementName);
                if (element == null) return;
                float sx = parseFloat(sender, getArgumentValue(2), "sx");
                float sy = parseFloat(sender, getArgumentValue(3), "sy");
                float sz = parseFloat(sender, getArgumentValue(4), "sz");
                if (Float.isNaN(sx) || Float.isNaN(sy) || Float.isNaN(sz)) return;
                element.scale(sx, sy, sz);
                Notification.info(sender, "[Holo] Set ''{0}'' scale to ({1}, {2}, {3})",
                        elementName,
                        String.format("%.2f", sx),
                        String.format("%.2f", sy),
                        String.format("%.2f", sz));
            }
        });

        // /dom holo_el_rotate <name> <element> <yaw> <pitch> [roll]
        reg(new SecondaryCommand("holo_el_rotate", List.of(
                holoNameArg(),
                elementNameArg(),
                new Argument("yaw", true),
                new Argument("pitch", true),
                new Argument("roll", "0")
        ), "[Debug] Set element local rotation (degrees)") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                HoloElement element = getElement(sender, item, elementName);
                if (element == null) return;
                float yaw = parseFloat(sender, getArgumentValue(2), "yaw");
                float pitch = parseFloat(sender, getArgumentValue(3), "pitch");
                float roll = parseFloat(sender, getArgumentValue(4), "roll");
                if (Float.isNaN(yaw) || Float.isNaN(pitch) || Float.isNaN(roll)) return;
                element.rotate(yaw, pitch, roll);
                Notification.info(sender, "[Holo] Rotated ''{0}'' to yaw={1} pitch={2} roll={3}",
                        elementName,
                        String.format("%.1f", yaw),
                        String.format("%.1f", pitch),
                        String.format("%.1f", roll));
            }
        });

        // /dom holo_el_glow <name> <element> <true/false>
        reg(new SecondaryCommand("holo_el_glow", List.of(
                holoNameArg(),
                elementNameArg(),
                new Argument("enabled", true,
                        (sender, preArgs) -> List.of("true", "false"))
        ), "[Debug] Toggle element glow") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                String enabledStr = getArgumentValue(2);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                HoloElement element = getElement(sender, item, elementName);
                if (element == null) return;
                boolean enabled = Boolean.parseBoolean(enabledStr);
                element.glow(enabled);
                Notification.info(sender, "[Holo] Element ''{0}'' glow: {1}", elementName, String.valueOf(enabled));
            }
        });

        // /dom holo_el_bright <name> <element> <block_light> <sky_light>
        reg(new SecondaryCommand("holo_el_bright", List.of(
                holoNameArg(),
                elementNameArg(),
                new Argument("block_light", true),
                new Argument("sky_light", true)
        ), "[Debug] Set element brightness (0-15)") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                HoloElement element = getElement(sender, item, elementName);
                if (element == null) return;
                int blockLight = parseInt(sender, getArgumentValue(2), "block_light");
                int skyLight = parseInt(sender, getArgumentValue(3), "sky_light");
                if (blockLight == Integer.MIN_VALUE || skyLight == Integer.MIN_VALUE) return;
                blockLight = Math.max(0, Math.min(15, blockLight));
                skyLight = Math.max(0, Math.min(15, skyLight));
                element.brightness(blockLight, skyLight);
                Notification.info(sender, "[Holo] Element ''{0}'' brightness: block={1} sky={2}",
                        elementName, String.valueOf(blockLight), String.valueOf(skyLight));
            }
        });

        // /dom holo_el_trans <name> <element> <tx> <ty> <tz>
        reg(new SecondaryCommand("holo_el_trans", List.of(
                holoNameArg(),
                elementNameArg(),
                new Argument("tx", true),
                new Argument("ty", true),
                new Argument("tz", true)
        ), "[Debug] Set element translation offset (pivot adjustment)") {
            @Override
            public void executeHandler(CommandSender sender) {
                String name = getArgumentValue(0);
                String elementName = getArgumentValue(1);
                HoloItem item = getItem(sender, name);
                if (item == null) return;
                HoloElement element = getElement(sender, item, elementName);
                if (element == null) return;
                float tx = parseFloat(sender, getArgumentValue(2), "tx");
                float ty = parseFloat(sender, getArgumentValue(3), "ty");
                float tz = parseFloat(sender, getArgumentValue(4), "tz");
                if (Float.isNaN(tx) || Float.isNaN(ty) || Float.isNaN(tz)) return;
                element.translation(tx, ty, tz);
                Notification.info(sender, "[Holo] Set ''{0}'' translation to ({1}, {2}, {3})",
                        elementName,
                        String.format("%.2f", tx),
                        String.format("%.2f", ty),
                        String.format("%.2f", tz));
            }
        });

        // /dom holo_demo
        reg(new SecondaryCommand("holo_demo",
                "[Debug] Create a demo coordinate axes at your position") {
            @Override
            public void executeHandler(CommandSender sender) {
                Player player = toPlayer(sender);
                if (player == null) return;

                String demoName = "holo_demo_" + System.currentTimeMillis() % 10000;
                Location loc = player.getLocation();

                try {
                    HoloItem axes = HoloManager.instance().create(demoName, loc);

                    // Origin marker - small white block at the center
                    axes.addBlockDisplay("origin", Material.WHITE_CONCRETE)
                            .scale(0.15f, 0.15f, 0.15f)
                            .translation(-0.075f, -0.075f, -0.075f)
                            .brightness(15, 15)
                            .glow(true)
                            .glowColor(Color.WHITE);

                    // X axis - red bar extending in +X direction
                    axes.addBlockDisplay("x_axis", Material.RED_CONCRETE)
                            .offset(0.6f, 0, 0)
                            .scale(1.0f, 0.08f, 0.08f)
                            .translation(-0.5f, -0.04f, -0.04f)
                            .brightness(15, 15)
                            .glow(true)
                            .glowColor(Color.RED);

                    // X axis tip - small red block at the end
                    axes.addBlockDisplay("x_tip", Material.RED_CONCRETE)
                            .offset(1.15f, 0, 0)
                            .scale(0.15f, 0.15f, 0.15f)
                            .translation(-0.075f, -0.075f, -0.075f)
                            .brightness(15, 15)
                            .glow(true)
                            .glowColor(Color.RED);

                    // Y axis - green bar extending in +Y direction
                    axes.addBlockDisplay("y_axis", Material.GREEN_CONCRETE)
                            .offset(0, 0.6f, 0)
                            .scale(0.08f, 1.0f, 0.08f)
                            .translation(-0.04f, -0.5f, -0.04f)
                            .brightness(15, 15)
                            .glow(true)
                            .glowColor(Color.GREEN);

                    // Y axis tip
                    axes.addBlockDisplay("y_tip", Material.GREEN_CONCRETE)
                            .offset(0, 1.15f, 0)
                            .scale(0.15f, 0.15f, 0.15f)
                            .translation(-0.075f, -0.075f, -0.075f)
                            .brightness(15, 15)
                            .glow(true)
                            .glowColor(Color.GREEN);

                    // Z axis - blue bar extending in +Z direction
                    axes.addBlockDisplay("z_axis", Material.BLUE_CONCRETE)
                            .offset(0, 0, 0.6f)
                            .scale(0.08f, 0.08f, 1.0f)
                            .translation(-0.04f, -0.04f, -0.5f)
                            .brightness(15, 15)
                            .glow(true)
                            .glowColor(Color.BLUE);

                    // Z axis tip
                    axes.addBlockDisplay("z_tip", Material.BLUE_CONCRETE)
                            .offset(0, 0, 1.15f)
                            .scale(0.15f, 0.15f, 0.15f)
                            .translation(-0.075f, -0.075f, -0.075f)
                            .brightness(15, 15)
                            .glow(true)
                            .glowColor(Color.BLUE);

                    Notification.info(sender, "[Holo] Demo coordinate axes ''{0}'' created at ({1}, {2}, {3})",
                            demoName,
                            String.format("%.1f", loc.getX()),
                            String.format("%.1f", loc.getY()),
                            String.format("%.1f", loc.getZ()));
                    Notification.info(sender, "[Holo] Red=X, Green=Y, Blue=Z. Use /dom holo_rotate {0} <yaw> <pitch> to rotate.",
                            demoName);
                } catch (Exception e) {
                    Notification.error(sender, "[Holo] Failed to create demo: " + e.getMessage());
                }
            }
        });
    }
}
