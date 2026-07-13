package cn.lunadeer.dominion.utils.chestui;

import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ChestUiManager implements Listener {
    private final JavaPlugin plugin;
    private final ChestUiConfig config;
    private final ChatInputService input;
    private final MenuController controller;
    private final MenuNavigator navigator;
    private final MenuRoute homeRoute;
    private final MenuRoute confirmationRoute;
    private final Map<UUID, MenuSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, MenuView> views = new ConcurrentHashMap<>();

    public ChestUiManager(JavaPlugin plugin, ChestUiConfig config, MenuRoute homeRoute, MenuRoute confirmationRoute,
                          String language,
                          Function<ChestUiManager, MenuController> controllerFactory) throws Exception {
        this.plugin = plugin;
        this.config = config;
        this.homeRoute = homeRoute;
        this.confirmationRoute = confirmationRoute;
        config.load(language);
        input = new ChatInputService(config);
        navigator = new MenuNavigator(this);
        controller = controllerFactory.apply(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(input, plugin);
    }

    public MenuNavigator navigator() { return navigator; }

    public void openMain(Player player) {
        Scheduler.runEntityTask(() -> {
            closeSession(player, false);
            MenuSession session = new MenuSession(player.getUniqueId(), homeRoute);
            sessions.put(player.getUniqueId(), session);
            render(player, session);
        }, player);
    }

    public void reload(String language) {
        try {
            config.load(language);
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (sessions.containsKey(player.getUniqueId())) navigator.refresh(player);
            }
        } catch (Exception e) {
            XLogger.error(e);
        }
    }

    void navigate(Player player, Consumer<MenuSession> mutation) {
        Scheduler.runEntityTask(() -> {
            MenuSession session = sessions.get(player.getUniqueId());
            if (session == null) return;
            mutation.accept(session);
            render(player, session);
        }, player);
    }

    public void close(Player player) {
        closeSession(player, true);
    }

    public void requestInput(Player player, String hint, Consumer<String> success) {
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        session.inputPending(true);
        session.transitioning(true);
        player.closeInventory();
        session.transitioning(false);
        input.request(player, hint, value -> {
            MenuSession current = sessions.get(player.getUniqueId());
            if (current == null) return;
            current.inputPending(false);
            success.accept(value);
        }, () -> {
            MenuSession current = sessions.get(player.getUniqueId());
            if (current == null) return;
            current.inputPending(false);
            render(player, current);
        });
    }

    public <T> void submit(Player player, CompletableFuture<T> future, Consumer<T> success) {
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null || session.busy()) return;
        long generation = session.beginAsync();
        future.whenComplete((value, throwable) -> Scheduler.runEntityTask(() -> {
            MenuSession current = sessions.get(player.getUniqueId());
            if (current == null || !current.isCurrentAsync(generation)) return;
            current.busy(false);
            if (throwable != null) Notification.error(player, throwable);
            else if (value != null) success.accept(value);
            long revisionAfterCallback = current.revision();
            Scheduler.runEntityTask(() -> {
                MenuSession latest = sessions.get(player.getUniqueId());
                if (latest == null || latest.revision() != revisionAfterCallback) return;
                latest.touch();
                render(player, latest);
            }, player);
        }, player));
    }

    public void confirm(Player player, String summary, Consumer<Player> action) {
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        session.confirmation(action);
        session.push(confirmationRoute.with("summary", summary));
        render(player, session);
    }

    public MenuSession session(Player player) { return sessions.get(player.getUniqueId()); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        if (!holder.playerId().equals(player.getUniqueId())) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() < topSize || event.isShiftClick()
                || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topSize) return;
        MenuSession session = sessions.get(player.getUniqueId());
        MenuView view = views.get(player.getUniqueId());
        if (session == null || view == null || session.busy() || holder.revision() != session.revision()) return;
        ClickHandler handler = view.handlers().get(event.getRawSlot());
        if (handler != null) handler.handle(event.getClick());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) return;
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null || session.transitioning() || session.inputPending()) return;
        closeSession(player, false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { closeSession(event.getPlayer(), false); }

    private void render(Player player, MenuSession session) {
        if (!player.isOnline()) return;
        try {
            MenuView view = controller.render(player, session);
            views.put(player.getUniqueId(), view);
            session.transitioning(true);
            player.openInventory(view.inventory());
            session.transitioning(false);
        } catch (Exception e) {
            session.transitioning(false);
            Notification.error(player, config.text("errors.unavailable"));
            XLogger.error(e);
            if (!session.current().id().equals(homeRoute.id())) {
                if (!session.back()) session.home();
                render(player, session);
            } else {
                closeSession(player, true);
            }
        }
    }

    private void closeSession(Player player, boolean closeInventory) {
        input.cancel(player.getUniqueId(), false);
        sessions.remove(player.getUniqueId());
        views.remove(player.getUniqueId());
        if (closeInventory) player.closeInventory();
    }
}
