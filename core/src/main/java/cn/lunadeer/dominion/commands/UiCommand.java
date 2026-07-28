package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.handler.UiPreferenceHandler;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.Option;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class UiCommand {
    public UiCommand() {
        new SecondaryCommand("ui", List.of(new Option(List.of("chest", "dialog", "default"))),
                "Select the graphical user interface.") {
            @Override
            public void executeHandler(CommandSender sender) {
                if (!(sender instanceof Player player)) {
                    Notification.error(sender, "This command can only be used by a player.");
                    return;
                }
                UiPreferenceHandler.select(player, getArgumentValue(0));
            }
        }.register();
    }
}
