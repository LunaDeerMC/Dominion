package cn.lunadeer.dominion.utils.chestui;

import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.scheduler.CancellableTask;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class ChatInputService implements Listener {
    static final long TIMEOUT_TICKS = 20L * 60L;
    private record Request(Consumer<String> success, Runnable cancel, CancellableTask timeout) {}
    private final Map<UUID, Request> requests = new ConcurrentHashMap<>();
    private final ChestUiConfig config;

    ChatInputService(ChestUiConfig config) { this.config = config; }

    void request(Player player, String hint, Consumer<String> success, Runnable cancel) {
        cancel(player.getUniqueId(), false);
        Notification.info(player, hint + " " + config.text("input.cancel-hint"));
        CancellableTask timeout = Scheduler.runTaskLater(() -> {
            Request removed = requests.remove(player.getUniqueId());
            if (removed != null) {
                Scheduler.runEntityTask(() -> {
                    Notification.warn(player, config.text("input.timeout"));
                    removed.cancel().run();
                }, player);
            }
        }, TIMEOUT_TICKS);
        requests.put(player.getUniqueId(), new Request(success, cancel, timeout));
    }

    void cancel(UUID player, boolean invoke) {
        Request request = requests.remove(player);
        if (request == null) return;
        if (request.timeout() != null) request.timeout().cancel();
        if (invoke) request.cancel().run();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Request request = requests.remove(event.getPlayer().getUniqueId());
        if (request == null) return;
        event.setCancelled(true);
        if (request.timeout() != null) request.timeout().cancel();
        Set<String> cancelWords = Set.copyOf(config.textList("input.cancel-keywords"));
        Scheduler.runEntityTask(() -> {
            if (cancelWords.stream().anyMatch(word -> word.equalsIgnoreCase(event.getMessage()))) {
                Notification.warn(event.getPlayer(), config.text("input.cancelled"));
                request.cancel().run();
            } else {
                request.success().accept(event.getMessage());
            }
        }, event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { cancel(event.getPlayer().getUniqueId(), false); }
}
