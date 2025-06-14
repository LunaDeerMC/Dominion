package cn.lunadeer.dominion.utils.command;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;


public abstract class SecondaryCommand {
    private final String command;
    private final List<Argument> arguments;
    private final List<String> permissions = new ArrayList<>();
    private boolean dynamic = false;
    private String description;

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

    public SecondaryCommand(String command, List<Argument> arguments) {
        this(command, arguments, "");
    }

    public SecondaryCommand(String command, String description) {
        this(command, new ArrayList<>(), description);
    }

    public SecondaryCommand(String command) {
        this(command, new ArrayList<>(), "");
    }

    public SecondaryCommand needPermission(String permission) {
        permissions.add(permission);
        return this;
    }

    public String getCommand() {
        return command;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public String getArgumentValue(int index) {
        if (index >= arguments.size()) {
            throw new IllegalArgumentException("Index out of range.");
        }
        return arguments.get(index).getValue();
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
        for (int i = 0; i < arguments.size(); i++) {
            if (i + 1 >= args.length) {
                break;
            }
            arguments.get(i).setValue(args[i + 1]);
        }
        executeHandler(sender);
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

    public String getDescription() {
        return description;
    }
}
