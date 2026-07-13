package cn.lunadeer.dominion.utils.inputter;

import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class InputterRunner {

    public static String ONLY_PLAYER = "Chat input can only be used by a player.";
    public static String CANCEL = " [Send 'C' to cancel the input.]";
    public static String INPUTTER_CANCELLED = "Input cancelled.";

    private Player sender;

    public InputterRunner(CommandSender sender, String hint) {
        if (!(sender instanceof Player player)) {
            Notification.error(sender, ONLY_PLAYER);
            return;
        }
        this.sender = player;
        Inputter.getInstance().register(this);
        Notification.info(sender, hint + CANCEL);
    }

    public void runner(String input) {
        Inputter.getInstance().unregister(this);
        try {
            if (input.equalsIgnoreCase("C")) {
                Notification.warn(sender, INPUTTER_CANCELLED);
                cancelRun();
            } else {
                run(input);
            }
        } catch (Exception e) {
            Notification.error(sender, e.getMessage());
            XLogger.error(e);
        }
    }

    public abstract void run(String input);

    public void cancelRun() {}

    public Player getSender() {
        return sender;
    }
}
