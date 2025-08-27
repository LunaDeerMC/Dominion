package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.events.dominion.modify.DominionReSizeEvent;
import cn.lunadeer.dominion.events.dominion.modify.DominionSetMessageEvent;
import cn.lunadeer.dominion.handler.DominionProviderHandler;
import cn.lunadeer.dominion.managers.TeleportManager;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.uis.MainMenu;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.Argument;
import cn.lunadeer.dominion.utils.command.Option;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.*;

public class DominionOperateCommand {

    public static class DominionOperateCommandText extends ConfigurationPart {
        public String resizeDescription = "Resize a dominion with a specific size and direction.";
        public String easyExpandDescription = "Expand the dominion size easily based on the player's current location.";
        public String easyContractDescription = "Contract the dominion size easily based on the player's current location.";
        public String deleteDescription = "Delete a dominion. Use 'force' to confirm deletion.";
        public String setMessageDescription = "Set a message for a dominion, either for entering or leaving.";
        public String setTeleportDescription = "Set the teleport location for a dominion.";
        public String renameDescription = "Rename a dominion.";
        public String setMapColorDescription = "Set the map color for a dominion.";
        public String giveDescription = "Give a dominion to a player.";
        public String tpDescription = "Teleport to a dominion.";
        public String switchUiDescription = "Switch the UI type for the dominion commands.";
    }

    public static SecondaryCommand resize = new SecondaryCommand("resize", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new Option(Arrays.stream(DominionReSizeEvent.TYPE.values()).map(Enum::name).map(String::toLowerCase).toList()),
            new Argument("size", true),
            new Option(Arrays.stream(DominionReSizeEvent.DIRECTION.values()).map(Enum::name).map(String::toLowerCase).toList(), "")
    ), Language.dominionOperateCommandText.resizeDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            resize(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2), getArgumentValue(3));
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand easyExpand = new SecondaryCommand("expand", List.of(
            new Argument("size", true),
            new Option(Arrays.stream(DominionReSizeEvent.DIRECTION.values()).map(Enum::name).map(String::toLowerCase).toList(), "")
    ), Language.dominionOperateCommandText.easyExpandDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            easyResize(sender, DominionReSizeEvent.TYPE.EXPAND.name(), getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand easyContract = new SecondaryCommand("contract", List.of(
            new Argument("size", true),
            new Option(Arrays.stream(DominionReSizeEvent.DIRECTION.values()).map(Enum::name).map(String::toLowerCase).toList(), "")
    ), Language.dominionOperateCommandText.easyContractDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            easyResize(sender, DominionReSizeEvent.TYPE.CONTRACT.name(), getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand delete = new SecondaryCommand("delete", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new Option(List.of("force"), "")
    ), Language.dominionOperateCommandText.deleteDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            delete(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand setMessage = new SecondaryCommand("set_msg", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new Option(Arrays.stream(DominionSetMessageEvent.TYPE.values()).map(Enum::name).map(String::toLowerCase).toList()),
            new Argument("message", true)
    ), Language.dominionOperateCommandText.setMessageDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            setMessage(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2));
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand setTeleport = new SecondaryCommand("set_tp", List.of(
            new CommandArguments.RequiredDominionArgument()
    ), Language.dominionOperateCommandText.setTeleportDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            setTp(sender, getArgumentValue(0));
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand rename = new SecondaryCommand("rename", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new Argument("newName", true)
    ), Language.dominionOperateCommandText.renameDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            rename(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand setMapColor = new SecondaryCommand("set_map_color", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new Argument("color", true)
    ), Language.dominionOperateCommandText.setMapColorDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            setMapColor(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand give = new SecondaryCommand("give", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredPlayerArgument(),
            new Option(List.of("force"), "")
    ), Language.dominionOperateCommandText.giveDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                DominionDTO dominion = toDominionDTO(getArgumentValue(0));
                PlayerDTO player = toPlayerDTO(getArgumentValue(1));
                boolean force = getArgumentValue(2).equals("force");
                DominionProviderHandler.getInstance().transferDominion(sender, dominion, player, force);
            } catch (Exception e) {
                Notification.error(sender, e);
            }
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand tp = new SecondaryCommand("tp", List.of(
            new CommandArguments.RequiredDominionArgument()
    ), Language.dominionOperateCommandText.tpDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                Player player = toPlayer(sender);
                DominionDTO dominion = toDominionDTO(getArgumentValue(0));
                TeleportManager.teleportToDominion(player, dominion);
            } catch (Exception e) {
                Notification.error(sender, e);
            }
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand switchUi = new SecondaryCommand("switch_ui", List.of(
            new Option(List.of(PlayerDTO.UI_TYPE.TUI.name(), PlayerDTO.UI_TYPE.CUI.name()), "")
    ),
            Language.dominionOperateCommandText.switchUiDescription
    ) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                Player player = toPlayer(sender);
                PlayerDTO playerDTO = CacheManager.instance.getPlayer(player.getUniqueId());
                if (playerDTO == null) {
                    throw new DominionException("Player data not found.");
                }
                PlayerDTO.UI_TYPE uiType;
                String uiTypeStr = getArgumentValue(0);
                if (uiTypeStr.isEmpty()) {
                    // Toggle UI type
                    uiType = playerDTO.getUiPreference() == PlayerDTO.UI_TYPE.TUI ? PlayerDTO.UI_TYPE.CUI : PlayerDTO.UI_TYPE.TUI;
                } else if (!Arrays.stream(PlayerDTO.UI_TYPE.values()).map(Enum::name).toList().contains(uiTypeStr)) {
                    throw new DominionException("Invalid UI type: " + uiTypeStr + ". Valid types are: " +
                            Arrays.stream(PlayerDTO.UI_TYPE.values()).map(Enum::name).toList());
                } else {
                    // Set UI type directly
                    uiType = PlayerDTO.UI_TYPE.valueOf(uiTypeStr);
                }
                playerDTO.setUiPreference(uiType);
                MainMenu.show(sender, "1");
            } catch (Exception e) {
                Notification.error(sender, e);
            }
        }
    }.needPermission(defaultPermission).register();


    public static SecondaryCommand easyInfo = new SecondaryCommand("info", List.of(
    ), TextUserInterface.sizeInfoTuiText.description) {
        @Override
        public void executeHandler(CommandSender sender) {
            if (!(sender instanceof Player player)) {
                Notification.error(sender, "This command can only be executed by a player.");
                return;
            }
            Location location = player.getLocation();
            DominionDTO dominion = CacheManager.instance.getDominion(location);
            if (dominion == null) {
                Notification.error(player, Language.selectPointEventsHandlerText.noDominion, location.getBlockX(), location.getBlockY(), location.getBlockZ());
                return;
            }
            Notification.info(player, ">--------------------<");
            Notification.info(player, dominion.getName());
            PlayerDTO owner = CacheManager.instance.getPlayer(dominion.getOwner());
            if (owner == null) {
                Notification.info(player, ">--------------------<");
                return;
            }
            Notification.info(player,"");
            Notification.info(player, TextUserInterface.sizeInfoTuiText.ownerName, owner.getLastKnownName());
            CuboidDTO cuboid = dominion.getCuboid();
            Notification.info(player, TextUserInterface.sizeInfoTuiText.infoLWH, cuboid.xLength(), cuboid.yLength(), cuboid.zLength());
            Notification.info(player, TextUserInterface.sizeInfoTuiText.infoHeight, cuboid.y1(), cuboid.y2());
            Notification.info(player, ">--------------------<");
        }
    }.needPermission(defaultPermission).register();

    /**
     * Adjusts the size of a specified dominion.
     *
     * @param sender       The command sender who initiates the size adjustment.
     * @param dominionName The name of the dominion to be resized.
     * @param operation    The operation to perform, either "expand" or "contract".
     * @param sizeStr      The size value to adjust by.
     * @param faceStr      The direction to adjust the size in (e.g., "N", "S", "E", "W", "U", "D").
     */
    public static void resize(CommandSender sender, String dominionName, String operation, String sizeStr, String faceStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            DominionReSizeEvent.TYPE type = toResizeType(operation);
            int size = toIntegrity(sizeStr);
            DominionReSizeEvent.DIRECTION dir = faceStr.isEmpty() ? toDirection(toPlayer(sender)) : toDirection(faceStr);
            DominionProviderHandler.getInstance().resizeDominion(
                    sender,
                    dominion,
                    type,
                    dir,
                    size
            );
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    /**
     * Adjusts the size of a dominion based on the player's current location.
     *
     * @param sender    The command sender who initiates the resize operation.
     * @param operation The type of resize operation to perform (e.g., "expand" or "contract").
     * @param sizeStr   The size value to adjust by.
     */
    public static void easyResize(CommandSender sender, String operation, String sizeStr, String faceStr) {
        try {
            Player player = toPlayer(sender);
            Location location = player.getLocation();
            DominionDTO dominion = CacheManager.instance.getDominion(location);
            if (dominion == null) {
                throw new DominionException(Language.selectPointEventsHandlerText.noDominion, location.getBlockX(), location.getBlockY(), location.getBlockZ());
            }
            DominionReSizeEvent.TYPE type = toResizeType(operation);
            int size = toIntegrity(sizeStr);
            DominionReSizeEvent.DIRECTION dir = faceStr.isEmpty() ? toDirection(player) : toDirection(faceStr);
            DominionProviderHandler.getInstance().resizeDominion(
                    sender,
                    dominion,
                    type,
                    dir,
                    size
            );
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    /**
     * Sets a message for a specified dominion.
     *
     * @param sender       The command sender who initiates the message setting.
     * @param dominionName The name of the dominion for which the message is being set.
     * @param typeStr      The type of message being set, either "enter" or "leave".
     * @param msg          The message content to be set.
     */
    public static void setMessage(CommandSender sender, String dominionName, String typeStr, String msg) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            DominionSetMessageEvent.TYPE type = toMessageType(typeStr);
            DominionProvider.getInstance().setDominionMessage(
                    sender,
                    dominion,
                    type,
                    msg
            );
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    /**
     * Deletes a specified dominion.
     *
     * @param sender       The command sender who initiates the deletion.
     * @param dominionName The name of the dominion to be deleted.
     * @param forceStr     A string indicating whether the deletion should be forced ("force").
     */
    public static void delete(CommandSender sender, String dominionName, String forceStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            boolean force = forceStr.equals("force");
            DominionProviderHandler.getInstance().deleteDominion(sender, dominion, false, force);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    /**
     * Renames a specified dominion.
     *
     * @param sender       The command sender who initiates the renaming.
     * @param dominionName The name of the dominion to be renamed.
     * @param newName      The new name for the dominion.
     */
    public static void rename(CommandSender sender, String dominionName, String newName) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            DominionProviderHandler.getInstance().renameDominion(sender, dominion, newName);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static void setMapColor(CommandSender sender, String dominionName, String colorStr) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            Color color = toColor(colorStr);
            DominionProvider.getInstance().setDominionMapColor(sender, dominion, color);
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static void setTp(CommandSender sender, String dominionName) {
        try {
            Player player = toPlayer(sender);
            DominionDTO dominion = toDominionDTO(dominionName);
            DominionProvider.getInstance().setDominionTpLocation(sender, dominion, player.getLocation());
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

}
