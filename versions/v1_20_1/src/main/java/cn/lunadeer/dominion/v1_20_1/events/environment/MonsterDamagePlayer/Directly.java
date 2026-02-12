package cn.lunadeer.dominion.v1_20_1.events.environment.MonsterDamagePlayer;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class Directly implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        Entity attacker = event.getDamager();
        if (!(attacker instanceof Enemy)) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        checkEnvironmentFlag(attacker.getLocation(), Flags.MONSTER_DAMAGE, event);
    }
}
