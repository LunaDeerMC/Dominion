package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class AnimalKilling implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player bukkitPlayer)) {
            return;
        }
        if (!(event.getEntity() instanceof Animals)) {
            return;
        }
        checkPrivilegeFlag(event.getEntity().getLocation(), Flags.ANIMAL_KILLING, bukkitPlayer, event);
    }
}
