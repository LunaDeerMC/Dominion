package cn.lunadeer.dominion.utils.dialogui;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class DialogMenuSession {
    private final UUID playerId;
    private final UUID sessionId = UUID.randomUUID();
    private final DialogRoute homeRoute;
    private final Deque<DialogRoute> history = new ArrayDeque<>();
    private DialogRoute current;
    private long revision;
    private boolean busy;
    private long asyncGeneration;
    private Consumer<Player> confirmation;
    private final Map<String, Object> state = new HashMap<>();

    public DialogMenuSession(UUID playerId, DialogRoute homeRoute) {
        this.playerId = playerId;
        this.homeRoute = homeRoute;
        this.current = homeRoute;
    }

    public UUID playerId() { return playerId; }
    public UUID sessionId() { return sessionId; }
    public DialogRoute current() { return current; }
    public long revision() { return revision; }
    public boolean busy() { return busy; }
    public void busy(boolean value) { busy = value; }
    public long beginAsync() { busy = true; return ++asyncGeneration; }
    public boolean isCurrentAsync(long value) { return asyncGeneration == value; }
    public void confirmation(Consumer<Player> value) { confirmation = value; }
    public Consumer<Player> takeConfirmation() { Consumer<Player> value = confirmation; confirmation = null; return value; }
    public void state(String key, Object value) {
        if (value == null) state.remove(key);
        else state.put(key, value);
    }
    public <T> T state(String key, Class<T> type) {
        Object value = state.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }
    public void clearState(String prefix) {
        state.keySet().removeIf(key -> key.startsWith(prefix));
    }
    public void push(DialogRoute route) { history.push(current); current = route; revision++; }
    public void replace(DialogRoute route) { current = route; revision++; }
    public boolean back() { if (history.isEmpty()) return false; current = history.pop(); revision++; return true; }
    public void home() { history.clear(); current = homeRoute; revision++; }
    public void touch() { revision++; }
}
