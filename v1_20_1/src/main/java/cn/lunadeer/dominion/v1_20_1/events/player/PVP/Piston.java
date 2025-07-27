package cn.lunadeer.dominion.v1_20_1.events.player.PVP;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;
import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlagSilence;

public class Piston implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void handler(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player attacker)) {
            return;
        }

        if (checkPrivilegeFlag(event.getPotion().getLocation(), Flags.PVP, attacker, null)) {
            for (LivingEntity entity : event.getAffectedEntities()) {
                if (!(entity instanceof Player victim) || victim == attacker) {
                    continue;
                }
                if (!checkPrivilegeFlagSilence(event.getPotion().getLocation(), Flags.PVP, victim, null)) {
                    event.setIntensity(victim, 0);
                }
            }
        } else {
            for (LivingEntity entity : event.getAffectedEntities()) {
                if (!(entity instanceof Player damaged) || damaged == attacker) {
                    continue;
                }
                event.setIntensity(damaged, 0);
            }
        }
    }
}
