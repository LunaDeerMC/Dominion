package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.Argument;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.*;
import static cn.lunadeer.dominion.misc.Others.autoPoints;

public class DominionCreateCommand {

    public static class DominionCreateCommandText extends ConfigurationPart {
        public String createDescription = "Create a new dominion using selected points.";
        public String createSubDescription = "Create a sub-dominion within an existing dominion.";
        public String autoCreateDescription = "Automatically create a dominion around your current position.";
        public String autoCreateSubDescription = "Automatically create a sub-dominion within an existing dominion.";
    }

    public static SecondaryCommand create = new SecondaryCommand("create", List.of(
            new Argument("name", true)
    ), Language.dominionCreateCommandText.createDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                Player player = toPlayer(sender);
                World world = player.getWorld();
                Location[] points = getSelectedPoints(player);
                CuboidDTO cuboidDTO = new CuboidDTO(points[0], points[1]);
                DominionProvider.getInstance().createDominion(
                        sender,
                        getArgumentValue(0),
                        player.getUniqueId(),
                        world, cuboidDTO,
                        null, false
                );
            } catch (Exception e) {
                Notification.error(sender, e);
            }
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand createSub = new SecondaryCommand("create_sub", List.of(
            new Argument("name", true),
            new CommandArguments.RequiredDominionArgument()
    ), Language.dominionCreateCommandText.createSubDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                Player player = toPlayer(sender);
                World world = player.getWorld();
                Location[] points = getSelectedPoints(player);
                CuboidDTO cuboidDTO = new CuboidDTO(points[0], points[1]);
                DominionProvider.getInstance().createDominion(
                        sender,
                        getArgumentValue(0),
                        player.getUniqueId(),
                        world, cuboidDTO,
                        toDominionDTO(getArgumentValue(1)),
                        false
                );
            } catch (Exception e) {
                Notification.error(sender, e);
            }
        }
    }.needPermission(defaultPermission).register();

    public static SecondaryCommand autoCreate = new SecondaryCommand("auto_create", List.of(
            new Argument("name", true)
    ), Language.dominionCreateCommandText.autoCreateDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            autoCreate(sender, getArgumentValue(0));
        }
    }.needPermission(defaultPermission).register();

    public static void autoCreate(CommandSender sender, String dominionName) {
        try {
            Player player = toPlayer(sender);
            World world = player.getWorld();
            Location[] points = autoPoints(player);
            CuboidDTO cuboidDTO = new CuboidDTO(points[0], points[1]);
            DominionProvider.getInstance().createDominion(
                    sender,
                    dominionName,
                    player.getUniqueId(),
                    world, cuboidDTO,
                    null, false
            );
        } catch (Exception e) {
            Notification.error(sender, e);
        }
    }

    public static SecondaryCommand autoCreateSub = new SecondaryCommand("auto_create_sub", List.of(
            new Argument("name", true),
            new CommandArguments.RequiredDominionArgument()
    ), Language.dominionCreateCommandText.autoCreateSubDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            try {
                Player player = toPlayer(sender);
                World world = player.getWorld();
                Location[] points = autoPoints(player);
                CuboidDTO cuboidDTO = new CuboidDTO(points[0], points[1]);
                DominionProvider.getInstance().createDominion(
                        sender,
                        getArgumentValue(0),
                        player.getUniqueId(),
                        world, cuboidDTO,
                        toDominionDTO(getArgumentValue(1)),
                        false
                );
            } catch (Exception e) {
                Notification.error(sender, e);
            }
        }
    }.needPermission(defaultPermission).register();

}
