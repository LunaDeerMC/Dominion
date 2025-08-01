package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Lead implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerLeashEntityEvent event) {
        Player player = event.getPlayer();
        checkPrivilegeFlag(event.getEntity().getLocation(), Flags.LEASH, player, event);
    }
}
