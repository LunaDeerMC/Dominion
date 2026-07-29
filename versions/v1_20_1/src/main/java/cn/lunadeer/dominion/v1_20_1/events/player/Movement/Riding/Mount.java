package cn.lunadeer.dominion.v1_20_1.events.player.Movement.Riding;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityMountEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Mount implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityMountEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        checkPrivilegeFlag(event.getMount().getLocation(), Flags.RIDING, player, event);
    }
}
