package cn.lunadeer.dominion.v1_20_1.events.player.PVP;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;
import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlagSilence;

public class CloudEffect implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void handler(AreaEffectCloudApplyEvent event) {
        if (!(event.getEntity().getSource() instanceof Player attacker)) {
            return;
        }

        if ((checkPrivilegeFlag(event.getEntity().getLocation(), Flags.PVP, attacker, null))) {
            event.getAffectedEntities().removeIf(entity -> {
                if (!(entity instanceof Player victim) || victim == attacker) {
                    return false;
                }
                return !checkPrivilegeFlagSilence(event.getEntity().getLocation(), Flags.PVP, victim, null);
            });
        } else {
            event.getAffectedEntities().removeIf(entity -> entity instanceof Player damaged && damaged != attacker);
        }
    }
}
