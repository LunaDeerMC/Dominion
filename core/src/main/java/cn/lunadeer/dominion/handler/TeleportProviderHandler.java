package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.managers.TeleportManager;
import cn.lunadeer.dominion.providers.TeleportProvider;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class TeleportProviderHandler extends TeleportProvider {
    public TeleportProviderHandler() {
        instance = this;
    }

    @Override
    public CompletableFuture<Boolean> teleport(@NotNull Player player, @NotNull DominionDTO dominion) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Scheduler.runEntityTask(() -> {
            try {
                result.complete(TeleportManager.teleportToDominion(player, dominion));
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        }, player);
        return result;
    }
}
