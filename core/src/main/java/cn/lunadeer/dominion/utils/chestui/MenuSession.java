package cn.lunadeer.dominion.utils.chestui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class MenuSession {
    private final UUID playerId;
    private final MenuRoute homeRoute;
    private final Deque<MenuRoute> history = new ArrayDeque<>();
    private MenuRoute current;
    private long revision;
    private boolean transitioning;
    private boolean inputPending;
    private boolean busy;
    private long asyncGeneration;
    private Consumer<Player> confirmation;

    public MenuSession(UUID playerId, MenuRoute homeRoute) {
        this.playerId = playerId;
        this.homeRoute = homeRoute;
        this.current = homeRoute;
    }
    public UUID playerId() { return playerId; }
    public MenuRoute current() { return current; }
    public long revision() { return revision; }
    public boolean transitioning() { return transitioning; }
    public void transitioning(boolean value) { transitioning = value; }
    public boolean inputPending() { return inputPending; }
    public void inputPending(boolean value) { inputPending = value; }
    public boolean busy() { return busy; }
    public void busy(boolean value) { busy = value; }
    public long beginAsync() { busy = true; return ++asyncGeneration; }
    public boolean isCurrentAsync(long generation) { return asyncGeneration == generation; }
    public void confirmation(Consumer<Player> value) { confirmation = value; }
    public Consumer<Player> takeConfirmation() { Consumer<Player> value = confirmation; confirmation = null; return value; }

    public void push(MenuRoute route) { history.push(current); current = route; revision++; }
    public void replace(MenuRoute route) { current = route; revision++; }
    public boolean back() { if (history.isEmpty()) return false; current = history.pop(); revision++; return true; }
    public void home() { history.clear(); current = homeRoute; revision++; }
    public void touch() { revision++; }
}
