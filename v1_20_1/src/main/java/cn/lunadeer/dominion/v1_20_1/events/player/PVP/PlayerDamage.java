package cn.lunadeer.dominion.v1_20_1.events.player.PVP;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;
import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlagSilence;

public class PlayerDamage implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void handler(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = null;
        Entity attacker_entity = event.getDamager();
        if (attacker_entity instanceof Player p) {
            attacker = p;
        } else if (attacker_entity instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        } else if (attacker_entity instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            attacker = p;
        } else if (attacker_entity instanceof Firework) {
            if (!checkPrivilegeFlagSilence(attacker_entity.getLocation(), Flags.PVP, victim, null)) {
                event.setCancelled(true);
            }
            return;
        }
        if (attacker == null || victim == attacker) {
            return;
        }

        if (!checkPrivilegeFlag(victim.getLocation(), Flags.PVP, attacker, null)
                || !checkPrivilegeFlagSilence(victim.getLocation(), Flags.PVP, victim, null)) {
            event.setCancelled(true);
        }
    }
}
