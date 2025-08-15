package cn.lunadeer.dominion.utils.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Manages the registration and execution of commands.
 * <p>
 * CommandManager is a class that manages the command registration and execution.
 * This is used to create command system such as:
 * <blockquote><pre>
 * /rootCommand subCommand <arg1> <arg2> [arg3] ...
 * /rootCommand subCommand2 <arg1> [arg3] ...
 * ...
 * </pre></blockquote>
 */
public class CommandManager implements TabExecutor, Listener {

    private static CommandManager instance;
    private static String rootCommand;
    private final JavaPlugin plugin;
    private Consumer<CommandSender> rootCommandConsumer = null;
    public SecondaryCommand helpCommand;

    public static CommandManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CommandManager is not initialized.");
        }
        return instance;
    }

    /**
     * Constructs a CommandManager with the specified root command.
     *
     * @param rootCommand The root command to be managed, should start with a slash.
     */
    public CommandManager(JavaPlugin plugin, String rootCommand) {
        this(plugin, rootCommand, null);
    }

    public CommandManager(JavaPlugin plugin, String rootCommand, Consumer<CommandSender> rootCommandConsumer) {
        if (instance != null) {
            throw new IllegalStateException("CommandManager is already initialized.");
        }
        instance = this;
        CommandManager.rootCommand = "/" + rootCommand;
        Objects.requireNonNull(Bukkit.getPluginCommand(rootCommand)).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.rootCommandConsumer = rootCommandConsumer;

        helpCommand = new SecondaryCommand("help", List.of(
                new Argument("page", "1")
        ), "Displays help information for commands.") {
            @Override
            public void executeHandler(CommandSender sender) {
                printHelp(sender, getArguments().get(0).getValue());
            }
        }.register();

        if (this.rootCommandConsumer == null) {
            this.rootCommandConsumer = (sender) -> {
                printHelp(sender, "1");
            };
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            for (String cmd : commands.keySet()) {
                // Unregister all commands that are hidden
                if (commands.get(cmd).isDynamic()) {
                    unregisterCommand(cmd);
                }
            }
        }
    }

    /**
     * Retrieves the root command.
     *
     * @return The root command.
     */
    public static String getRootCommand() {
        return rootCommand;
    }

    private static final Map<String, SecondaryCommand> commands = new HashMap<>();
    private static final Map<String, SecondaryCommand> commandsUsable = new HashMap<>();

    /**
     * Registers a secondary command.
     *
     * @param command The secondary command to be registered.
     */
    public static void registerCommand(SecondaryCommand command) {
        commands.put(command.getCommand(), command);
        if (!command.isDynamic()) {
            commandsUsable.put(command.getCommand(), command);
        }
    }

    /**
     * Unregisters a secondary command.
     *
     * @param command The secondary command to be unregistered.
     */
    public static void unregisterCommand(SecondaryCommand command) {
        commands.remove(command.getCommand());
        commandsUsable.remove(command.getCommand());
    }

    /**
     * Unregisters a secondary command by its name.
     *
     * @param command The name of the secondary command to be unregistered.
     */
    public static void unregisterCommand(String command) {
        commands.remove(command);
        commandsUsable.remove(command);
    }

    /**
     * Retrieves a secondary command by its name.
     *
     * @param command The name of the secondary command.
     * @return The corresponding SecondaryCommand object, or null if not found.
     */
    public static SecondaryCommand getCommand(String command) {
        return commands.get(command);
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 0) {
            if (rootCommandConsumer != null) {
                try {
                    rootCommandConsumer.accept(commandSender);
                } catch (Exception e) {
                    commandSender.sendMessage(e.getMessage());
                }
            }
            return true;
        }
        SecondaryCommand cmd = getCommand(strings[0]);
        if (cmd == null) {
            return true;
        }
        try {
            cmd.run(commandSender, strings);
        } catch (Exception e) {
            commandSender.sendMessage(e.getMessage());
            plugin.getLogger().severe(e.getMessage());
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            return new ArrayList<>(commandsUsable.keySet().stream()
                    .filter(cmd -> cmd.startsWith(strings[0]))
                    .toList());
        }
        SecondaryCommand cmd = getCommand(strings[0]);
        if (cmd == null) {
            return null;
        }
        List<Argument> args = cmd.getArguments();
        if (strings.length - 1 > args.size()) {
            return null;
        }
        // Set values for all arguments up to the current position
        String[] suggestionPreArg = new String[strings.length - 1];
        for (int i = 1; i < strings.length - 1; i++) {
            args.get(i - 1).setValue(strings[i]);
            suggestionPreArg[i - 1] = strings[i];
        }
        return args.get(strings.length - 2).getSuggestion().get(commandSender, suggestionPreArg);
    }

    private void printHelp(CommandSender sender, String pageStr) {
        if (commands.isEmpty()) {
            return;
        }
        int pageSize = sender instanceof Player ? 10 : 25; // Number of commands per page
        int page;
        try {
            page = Integer.parseInt(pageStr);
        } catch (NumberFormatException e) {
            // If pageStr is not a valid number, default to page 1
            page = 1;
        }
        int totalPages = (int) Math.ceil((double) commandsUsable.size() / pageSize);
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        String header = "&a------------ [" + pageStr + "/" + totalPages + "] ------------";
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', header));

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, commandsUsable.size());
        int index = 0;
        for (String cmd : commandsUsable.keySet()) {
            if (index >= start && index < end) {
                String line = commandsUsable.get(cmd).getUsage();
                if (!commandsUsable.get(cmd).getDescription().isEmpty()) {
                    line += "&8 - &b" + commandsUsable.get(cmd).getDescription();
                }
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            }
            index++;
        }
    }
}
