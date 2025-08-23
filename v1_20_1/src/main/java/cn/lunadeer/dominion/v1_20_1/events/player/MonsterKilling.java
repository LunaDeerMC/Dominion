package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class MonsterKilling implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player bukkitPlayer)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }
        checkPrivilegeFlag(entity.getLocation(), Flags.MONSTER_KILLING, bukkitPlayer, event);
    }
}
