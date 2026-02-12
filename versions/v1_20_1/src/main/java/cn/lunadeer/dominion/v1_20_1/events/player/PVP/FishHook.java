package cn.lunadeer.dominion.v1_20_1.events.player.PVP;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;
import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlagSilence;

public class FishHook implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void handler(PlayerFishEvent event) {
        if (!(event.getCaught() instanceof Player victim)) {
            return;
        }
        Player attacker = event.getPlayer();
        if (victim == attacker) {
            return;
        }

        if (!checkPrivilegeFlag(victim.getLocation(), Flags.PVP, attacker, null)
                || !checkPrivilegeFlagSilence(victim.getLocation(), Flags.PVP, victim, null)) {
            event.getHook().remove();
            event.setCancelled(true);
        }
    }
}
