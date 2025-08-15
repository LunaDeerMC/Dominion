package cn.lunadeer.dominion.utils.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.List;


public abstract class SecondaryCommand {
    private final String command;
    private final List<Argument> arguments;
    private final List<String> permissions = new ArrayList<>();
    private boolean dynamic = false;
    private String description;

    // ThreadLocal to store arguments for the current thread
    // This allows us to have a separate set of arguments for each command execution
    // without affecting other threads or command executions.
    private final ThreadLocal<List<Argument>> threadLocalArguments = new ThreadLocal<>();

    /**
     * Create a new SecondaryCommand with command and arguments.
     * <blockquote><pre>
     * /rootCommand SecondaryCommand arg1 arg2 arg3 ...
     * </pre></blockquote>
     *
     * @param command   The secondary command name.
     * @param arguments The arguments.
     */
    public SecondaryCommand(String command, List<Argument> arguments, String description) {
        this.command = command;
        this.arguments = arguments;
        this.description = description;
        boolean startOptional = false;
        for (Argument argument : arguments) {
            if (startOptional && argument.isRequired()) {
                throw new IllegalArgumentException("Optional argument should always be at the end.");
            }
            if (!argument.isRequired()) {
                startOptional = true;
            }
        }
    }

    /**
     * Constructs a SecondaryCommand with the specified command and arguments.
     * The description will be set to an empty string.
     *
     * @param command   The secondary command name.
     * @param arguments The list of arguments for the command.
     */
    public SecondaryCommand(String command, List<Argument> arguments) {
        this(command, arguments, "");
    }

    /**
     * Constructs a SecondaryCommand with the specified command and description.
     * The arguments list will be empty.
     *
     * @param command     The secondary command name.
     * @param description The description of the command.
     */
    public SecondaryCommand(String command, String description) {
        this(command, new ArrayList<>(), description);
    }

    /**
     * Constructs a SecondaryCommand with the specified command.
     * The arguments list and description will be empty.
     *
     * @param command The secondary command name.
     */
    public SecondaryCommand(String command) {
        this(command, new ArrayList<>(), "");
    }

    /**
     * Adds a required permission for this command.
     *
     * @param permission The permission string to be added.
     * @return The current instance of SecondaryCommand.
     */
    public SecondaryCommand needPermission(String permission) {
        permissions.add(permission);
        return this;
    }

    /**
     * Adds a child permission for this command based on a root permission.
     * If the child permission does not exist, it will be created and registered.
     * The child permission is derived from the root permission by appending the command name.
     * <p>
     * e.g. if the root permission is "myplugin.command" and the command is "subcommand",
     * the child permission will be "myplugin.command.subcommand".
     *
     * @param rootPermission The root permission string.
     * @param defaultValue   The default value for the child permission.
     * @return The current instance of SecondaryCommand.
     */
    public SecondaryCommand needChildPermission(String rootPermission, PermissionDefault defaultValue) {
        String childPermission = rootPermission + "." + command;
        permissions.add(childPermission);
        if (Bukkit.getPluginManager().getPermission(childPermission) == null) {
            Bukkit.getPluginManager().addPermission(
                    new Permission(childPermission, defaultValue)
            );
        }
        return this;
    }

    public String getCommand() {
        return command;
    }

    /**
     * Gets the list of arguments for this command.
     * <p>
     * This method returns the thread-local arguments if they exist,
     * otherwise it returns a copy of the original arguments.
     * </p>
     *
     * @return The list of arguments for this command.
     */
    public List<Argument> getArguments() {
        // get the thread-local arguments if they exist
        List<Argument> threadArgs = threadLocalArguments.get();
        if (threadArgs != null) {
            return threadArgs;
        }

        // otherwise, return a copy of the original arguments
        List<Argument> arguments = new ArrayList<>();
        for (Argument argument : this.arguments) {
            arguments.add(argument.copy());
        }
        return arguments;
    }

    public String getArgumentValue(int index) {
        List<Argument> currentArgs = threadLocalArguments.get();
        if (currentArgs == null) {
            currentArgs = this.arguments;
        }

        if (index >= currentArgs.size()) {
            throw new IllegalArgumentException("Index out of range.");
        }
        return currentArgs.get(index).getValue();
    }

    public String getUsage() {
        StringBuilder usage = new StringBuilder();
        usage.append(CommandManager.getRootCommand()).append(" ").append(command);
        for (Argument argument : arguments) {
            usage.append(" ");
            usage.append(argument.toString());
        }
        return usage.toString();
    }

    public void assertArguments(String[] args) throws InvalidArgumentException {
        if (arguments.isEmpty()) return;
        int pos = 1;
        for (Argument argument : arguments) {
            if (!argument.isRequired()) {
                break;
            }
            if (pos >= args.length) {
                throw new InvalidArgumentException(getUsage());
            }
            if (argument instanceof Option option) {
                if (option.getOptions().contains(args[pos])) {
                    pos++;
                    continue;
                } else {
                    throw new InvalidArgumentException(getUsage());
                }
            }
            pos++;
        }
    }

    public void assertPermission(CommandSender sender) throws NoPermissionException {
        if (permissions.isEmpty()) return;
        for (String permission : permissions) {
            if (!sender.hasPermission(permission)) {
                throw new NoPermissionException(permission);
            }
        }
    }

    public void run(CommandSender sender, String[] args) throws NoPermissionException, InvalidArgumentException {
        if (args.length < 1) return;
        if (!args[0].equals(command)) {
            return;
        }
        assertPermission(sender);
        assertArguments(args);

        // create a thread-local copy of the arguments
        List<Argument> threadArgs = new ArrayList<>();
        for (Argument arg : this.arguments) {
            threadArgs.add(arg.copy());
        }
        threadLocalArguments.set(threadArgs);

        try {
            // set the values for the arguments in the thread-local copy
            for (int i = 0; i < threadArgs.size(); i++) {
                if (i + 1 >= args.length) {
                    break;
                }
                threadArgs.get(i).setValue(args[i + 1]);
            }
            executeHandler(sender);
        } finally {
            // clear the thread-local arguments to avoid memory leaks
            threadLocalArguments.remove();
        }
    }

    public SecondaryCommand register() {
        CommandManager.registerCommand(this);
        return this;
    }

    /**
     * Sets the command to be dynamic, meaning its usage message will be hidden.
     * <p>
     * This kind of command is usually generated by the system and does not need to be displayed to the user.
     * When there is no players online, command with hidden usage will be unregistered automatically.
     *
     * @return The current instance of SecondaryCommand with usage hidden.
     */
    public SecondaryCommand dynamic() {
        dynamic = true;
        return this;
    }

    /**
     * Checks if the usage message for this secondary command is hidden.
     *
     * @return true if the usage message is hidden, false otherwise.
     */
    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * Executes the handler for the secondary command.
     * <p>
     * DO NOT CALL THIS METHOD DIRECTLY. Use {@link #run(CommandSender, String[])} instead.
     *
     * @param sender The command sender who executes the command.
     */
    public abstract void executeHandler(CommandSender sender);

    /**
     * Gets the description of this secondary command.
     *
     * @return the description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description for this secondary command.
     *
     * @param description the new description string
     * @return the current instance of SecondaryCommand
     */
    public SecondaryCommand setDescription(String description) {
        this.description = description;
        return this;
    }
}
