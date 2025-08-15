package cn.lunadeer.dominion.utils.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface Suggestion {
    List<String> get(CommandSender sender, String... preArguments);
}
