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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlagSilence;

public class FlyGlowCheckHandler implements Listener {

    // 记录由 Dominion 主动开启飞行的玩家，用于区分 Dominion 和其他插件（Essentials/CMI）授予的飞行
    private static final Set<UUID> dominionFlightPlayers = new HashSet<>();

    public FlyGlowCheckHandler(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onPlayerCrossBorder(PlayerCrossDominionBorderEvent event) {
        Player player = event.getPlayer();
        handle(event.getPlayer(), event.getTo(), Flags.GLOW, 
        () -> {
            // If the player is the owner of the dominion, check the owner glow flag
            if (event.getPlayer().getUniqueId().equals(event.getTo().getOwner())){
                if (event.getTo().getOwnerGlow()) {
                    player.setGlowing(true);
                } else {
                    player.setGlowing(false);
                }
                return;
            }
            player.setGlowing(true);
        }, 
        () -> {
            player.setGlowing(false);
        });
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        handle(event.getPlayer(), event.getTo(), Flags.FLY, () -> allowFly(player), () -> disableFly(player));
    }

    @EventHandler
    public void onDominionDelete(PlayerMoveOutDominionEvent event) {
        DominionDTO dominion = event.getDominion();
        if (dominion != null) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // 领地被删除时强制关闭飞行，不受追踪来源限制
        dominionFlightPlayers.remove(player.getUniqueId());
        player.setAllowFlight(false);
        if (event.getPlayer().isFlying()) player.setFlying(false);
        player.setGlowing(false);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // 死亡后原版重置飞行状态，清除追踪记录
        dominionFlightPlayers.remove(player.getUniqueId());
        // 清除缓存的领地ID，确保后续移动能正确触发边界事件
        CacheManager.instance.resetPlayerCurrentDominionId(player);
        // 在重生位置重新检查飞行状态
        DominionDTO dominion = CacheManager.instance.getDominion(event.getRespawnLocation());
        handle(player, dominion, Flags.FLY, () -> allowFly(player), () -> disableFly(player));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // 玩家上线时重新检查飞行状态，防止在领地内下线后上线无法飞行
        DominionDTO dominion = CacheManager.instance.getDominion(player.getLocation());
        handle(player, dominion, Flags.FLY, () -> allowFly(player), () -> disableFly(player));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家退出时清理飞行追踪记录
        dominionFlightPlayers.remove(event.getPlayer().getUniqueId());
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
            if (!flag.getEnable()) {
                onDisable.run();
                return;
            }
            if (checkPrivilegeFlagSilence(to, flag, player, null)) {
                onAllow.run();
            } else {
                onDisable.run();
            }
        }
    }


    private static void allowFly(Player player) {
        // 记录由 Dominion 授予飞行的玩家，用于区分其他插件（Essentials/CMI）的飞行
        if (!player.getAllowFlight()) {
            dominionFlightPlayers.add(player.getUniqueId());
        }
        player.setAllowFlight(true);
    }

    private static void disableFly(Player player) {
        // 只关闭由 Dominion 开启的飞行，不干涉其他插件授予的飞行
        if (!dominionFlightPlayers.contains(player.getUniqueId())) {
            return;
        }
        dominionFlightPlayers.remove(player.getUniqueId());
        player.setAllowFlight(false);
        if (player.isFlying()) {
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onPlayerTryFly(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
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
