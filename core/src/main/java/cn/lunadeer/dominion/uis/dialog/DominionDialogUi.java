package cn.lunadeer.dominion.uis.dialog;

import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.nms.NMSDialogCallbackBridge;
import cn.lunadeer.dominion.nms.NMSDialogFactory;
import cn.lunadeer.dominion.nms.NMSManager;
import cn.lunadeer.dominion.uis.DominionUi;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuController;
import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import cn.lunadeer.dominion.utils.dialogui.DialogCallbackRegistry;
import cn.lunadeer.dominion.utils.dialogui.DialogEncodingResult;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuSession;
import cn.lunadeer.dominion.utils.dialogui.DialogMenuUi;
import cn.lunadeer.dominion.utils.dialogui.DialogNavigator;
import cn.lunadeer.dominion.utils.dialogui.DialogRoute;
import cn.lunadeer.dominion.utils.dialogui.DialogSessionContext;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Version-independent Dominion Dialog UI runtime.
 */
public final class DominionDialogUi implements DialogMenuUi, Listener {
    private final JavaPlugin plugin;
    private final DialogUiText text;
    private final DialogMenuController controller;
    private final DialogNavigator navigator;
    private final Map<UUID, DialogMenuSession> sessions = new ConcurrentHashMap<>();
    private final DialogRoute homeRoute = DialogRoute.of(DialogMenuId.MAIN);
    private final DialogRoute confirmationRoute = DialogRoute.of(DialogMenuId.CONFIRM);

    public DominionDialogUi(JavaPlugin plugin) throws Exception {
        this.plugin = plugin;
        text = new DialogUiText(plugin);
        text.load(Configuration.language);
        navigator = new DialogNavigator(this);
        controller = new DialogMenuController(this, text, navigator);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getOnlinePlayers().forEach(this::installBridge);
    }

    public boolean available(Player player) {
        return factory() != null && bridge() != null
                && (bridge().isInstalled(player) || bridge().install(player))
                && NMSManager.instance().isDialogAvailable(player);
    }

    public boolean hasSession(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    public void openMain(Player player) {
        Scheduler.runEntityTask(() -> {
            if (!available(player)) {
                DominionUi.openChest(player);
                return;
            }
            closeSession(player, false);
            DialogMenuSession session = new DialogMenuSession(player.getUniqueId(), homeRoute);
            sessions.put(player.getUniqueId(), session);
            render(player, session);
        }, player);
    }

    public void reload() {
        try {
            text.load(Configuration.language);
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                DialogMenuSession session = sessions.get(player.getUniqueId());
                if (session != null) navigate(player, DialogMenuSession::touch);
            });
        } catch (Exception exception) {
            XLogger.error(exception);
        }
    }

    @Override
    public DialogNavigator navigator() {
        return navigator;
    }

    @Override
    public void navigate(Player player, Consumer<DialogMenuSession> mutation) {
        Scheduler.runEntityTask(() -> {
            DialogMenuSession session = sessions.get(player.getUniqueId());
            if (session == null || session.busy()) return;
            mutation.accept(session);
            render(player, session);
        }, player);
    }

    @Override
    public void close(Player player) {
        closeSession(player, true);
    }

    @Override
    public void requestInput(Player player, String hint, Consumer<String> success) {
        DialogMenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        session.touch();
        String key = "value";
        DialogSpec.ActionButton submit = DialogSpec.ActionButton.of(
                Component.text(text.text("dialog.submit"), NamedTextColor.GREEN),
                text.icon("submit"),
                new DialogSpec.CallbackAction((viewer, response) -> {
                    String value = response.getText(key);
                    if (value != null) success.accept(value);
                })
        );
        DialogSpec.ActionButton cancel = DialogSpec.ActionButton.of(
                Component.text(text.text("dialog.cancel"), NamedTextColor.RED),
                text.icon("cancel"),
                new DialogSpec.CallbackAction((viewer, response) -> render(viewer, session))
        );
        DialogSpec dialog = DialogSpec.builder(
                        Component.text(text.text("dialog.input-title")),
                        new DialogSpec.Confirmation(submit, cancel))
                .input(new DialogSpec.TextInput(key, 336, Component.text(hint), true, "", 128, null))
                .afterAction(DialogSpec.AfterAction.CLOSE)
                .build();
        show(player, session, dialog);
    }

    @Override
    public <T> void submit(Player player, CompletableFuture<T> future, Consumer<T> success) {
        submit(player, future, (value, session) -> success.accept(value));
    }

    @Override
    public <T> void submit(Player player, CompletableFuture<T> future, BiConsumer<T, DialogMenuSession> success) {
        DialogMenuSession session = sessions.get(player.getUniqueId());
        if (session == null || session.busy()) return;
        long generation = session.beginAsync();
        showWaiting(player, session);
        future.whenComplete((value, throwable) -> Scheduler.runEntityTask(() -> {
            DialogMenuSession current = sessions.get(player.getUniqueId());
            if (current == null || !current.isCurrentAsync(generation)) {
                if (throwable != null && player.isOnline()) Notification.error(player, throwable);
                return;
            }
            current.busy(false);
            if (throwable != null) Notification.error(player, throwable);
            else if (value != null) success.accept(value, current);
            current.touch();
            render(player, current);
        }, player));
    }

    @Override
    public void confirm(Player player, String summary, Consumer<Player> action) {
        DialogMenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        session.confirmation(action);
        session.push(confirmationRoute.with("summary", summary));
        render(player, session);
    }

    @Override
    public DialogMenuSession session(Player player) {
        return sessions.get(player.getUniqueId());
    }

    private void render(Player player, DialogMenuSession session) {
        if (!player.isOnline()) return;
        try {
            DialogSpec dialog = controller.render(player, session);
            show(player, session, dialog);
        } catch (Exception exception) {
            Notification.error(player, text.text("errors.unavailable"));
            XLogger.warn("Dialog render failed for {0} at {1}: {2}",
                    player.getName(), session.current().id(), exception.getMessage());
            XLogger.debug("{0}: {1}", exception.getClass().getName(), exception.getMessage());
            closeSession(player, false);
            DominionUi.openChest(player);
        }
    }

    private void showWaiting(Player player, DialogMenuSession session) {
        DialogSpec.ActionButton close = DialogSpec.ActionButton.of(
                Component.text(text.text("dialog.continue-in-background"), NamedTextColor.RED),
                text.icon("cancel"),
                new DialogSpec.CallbackAction((viewer, response) -> closeSession(viewer, true))
        );
        DialogSpec waiting = DialogSpec.builder(
                        Component.text(text.text("dialog.waiting-title")),
                        new DialogSpec.Notice(close))
                .body(new DialogSpec.PlainMessageBody(Component.text(text.text("dialog.waiting-message")), 300))
                .canCloseWithEscape(false)
                .afterAction(DialogSpec.AfterAction.CLOSE)
                .build();
        show(player, session, waiting);
    }

    private void show(Player player, DialogMenuSession session, DialogSpec dialog) {
        NMSDialogFactory factory = factory();
        if (factory == null) throw new IllegalStateException("Dialog backend is unavailable");
        DialogEncodingResult result = factory.show(
                player,
                dialog,
                new DialogSessionContext(session.sessionId(), session.revision())
        );
        if (!result.successful()) throw new IllegalStateException(result.message());
    }

    private void closeSession(Player player, boolean closeClient) {
        sessions.remove(player.getUniqueId());
        DialogCallbackRegistry.INSTANCE.invalidate(player.getUniqueId());
        if (closeClient && factory() != null) factory().close(player);
    }

    private void installBridge(Player player) {
        NMSDialogCallbackBridge bridge = bridge();
        if (bridge != null) bridge.install(player);
    }

    private NMSDialogFactory factory() {
        return NMSManager.instance().getDialogFactory().orElse(null);
    }

    private NMSDialogCallbackBridge bridge() {
        return NMSManager.instance().getDialogCallbackBridge().orElse(null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Scheduler.runEntityTask(() -> installBridge(event.getPlayer()), event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        closeSession(event.getPlayer(), false);
        if (bridge() != null) bridge().uninstall(event.getPlayer());
    }
}
