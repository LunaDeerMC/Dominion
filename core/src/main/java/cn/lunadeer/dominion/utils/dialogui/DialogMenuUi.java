package cn.lunadeer.dominion.utils.dialogui;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface DialogMenuUi {
    DialogNavigator navigator();
    void navigate(Player player, Consumer<DialogMenuSession> mutation);
    void close(Player player);
    void requestInput(Player player, String hint, Consumer<String> success);
    <T> void submit(Player player, CompletableFuture<T> future, Consumer<T> success);
    void confirm(Player player, String summary, Consumer<Player> action);
    DialogMenuSession session(Player player);
}
