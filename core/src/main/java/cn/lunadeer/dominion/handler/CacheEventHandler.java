package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.events.PlayerCrossDominionBorderEvent;
import cn.lunadeer.dominion.events.PlayerMoveInDominionEvent;
import cn.lunadeer.dominion.events.PlayerMoveOutDominionEvent;
import cn.lunadeer.dominion.utils.MessageDisplay;
import cn.lunadeer.dominion.utils.ParticleUtil;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static cn.lunadeer.dominion.managers.HooksManager.setPlaceholder;

public class CacheEventHandler implements Listener {

    @EventHandler
    public void onPlayerMoveInDominion(PlayerMoveInDominionEvent event) {
        XLogger.debug("PlayerMoveInDominionEvent called.");
        // show message
        MessageDisplay.show(event.getPlayer(), MessageDisplay.Place.valueOf(Configuration.pluginMessage.enterLeaveDisplayPlace.toUpperCase()),
                setPlaceholder(
                        event.getPlayer(),
                        event.getDominion().getJoinMessage()
                                .replace("{DOM}", event.getDominion().getName())
                                .replace("{OWNER}", CacheManager.instance.getPlayerName(event.getDominion().getOwner()))
                                .replace("{PLAYER}", event.getPlayer().getName())
                )
        );
        // show border
        if (event.getDominion().getEnvironmentFlagValue().getOrDefault(Flags.SHOW_BORDER, false)) {
            ParticleUtil.showBorder(event.getPlayer(), event.getDominion());
        }
    }

    @EventHandler
    public void onPlayerMoveOutDominion(PlayerMoveOutDominionEvent event) {
        XLogger.debug("PlayerMoveOutDominionEvent called.");
        if (event.getDominion() == null) return;
        // show message
        MessageDisplay.show(event.getPlayer(), MessageDisplay.Place.valueOf(Configuration.pluginMessage.enterLeaveDisplayPlace.toUpperCase()),
                setPlaceholder(
                        event.getPlayer(),
                        event.getDominion().getLeaveMessage()
                                .replace("{DOM}", event.getDominion().getName())
                                .replace("{OWNER}", CacheManager.instance.getPlayerName(event.getDominion().getOwner()))
                                .replace("{PLAYER}", event.getPlayer().getName())
                )
        );
        // show border
        if (event.getDominion().getEnvironmentFlagValue().getOrDefault(Flags.SHOW_BORDER, false)) {
            ParticleUtil.showBorder(event.getPlayer(), event.getDominion());
        }
    }

    @EventHandler
    public void onPlayerCrossDominionBorderEvent(PlayerCrossDominionBorderEvent event) {
        XLogger.debug("PlayerCrossDominionBorderEvent called.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            CacheManager.instance.updatePlayerName(event.getPlayer());
        } catch (Exception e) {
            XLogger.error(e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        CacheManager.instance.resetPlayerCurrentDominionId(event.getPlayer());
    }

}
