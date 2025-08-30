package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.WorldWide;
import cn.lunadeer.dominion.events.PlayerCrossDominionBorderEvent;
import cn.lunadeer.dominion.events.PlayerMoveOutDominionEvent;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlagSilence;

public class FlyGlowCheckHandler implements Listener {

    public FlyGlowCheckHandler(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onPlayerCrossBorder(PlayerCrossDominionBorderEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        handle(event.getPlayer(), event.getTo(), Flags.FLY, () -> allowFly(player), () -> disableFly(player));
        handle(event.getPlayer(), event.getTo(), Flags.GLOW, () -> player.setGlowing(true), () -> player.setGlowing(false));
    }

    @EventHandler
    public void onDominionDelete(PlayerMoveOutDominionEvent event) {
        DominionDTO dominion = event.getDominion();
        if (dominion != null) return;
        Player player = event.getPlayer();
        if (player.isOp() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        disableFly(player);
        if (event.getPlayer().isFlying()) player.setFlying(false);
        player.setGlowing(false);
    }

    private static void handle(@NotNull Player player,
                               @Nullable DominionDTO to,
                               @NotNull PriFlag flag,
                               @NotNull Runnable onAllow,
                               @NotNull Runnable onDisable) {
        if (to == null) {
            World world = player.getWorld();
            if (WorldWide.isWorldWideEnabled(world) && WorldWide.getGuestFlagValue(world, flag)) {
                onAllow.run();
            } else {
                onDisable.run();
            }
        } else {
            if (checkPrivilegeFlagSilence(to, flag, player, null)) {
                onAllow.run();
            } else {
                onDisable.run();
            }
        }
    }


    private static void allowFly(Player player) {
        for (String flyPN : Configuration.flyPermissionNodes) {
            if (player.hasPermission(flyPN)) {
                return;
            }
        }
        player.setAllowFlight(true);
    }

    private static void disableFly(Player player) {
        for (String flyPN : Configuration.flyPermissionNodes) {
            if (player.hasPermission(flyPN)) {
                return;
            }
        }
        player.setAllowFlight(false);
        if (player.isFlying()) {
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onPlayerTryFly(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        DominionDTO dominion = CacheManager.instance.getDominion(event.getPlayer().getLocation());
        handle(event.getPlayer(), dominion, Flags.FLY, () -> {
            allowFly(player);
            if (!event.getPlayer().isFlying()) player.setFlying(true);
        }, () -> {
            disableFly(player);
            if (event.getPlayer().isFlying()) player.setFlying(false);
        });
    }
}
