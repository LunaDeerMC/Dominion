package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Hook implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerFishEvent event) {
        if (event.isCancelled()) return;
        Entity caught = event.getCaught();
        if (caught == null) {
            return;
        }
        Player player = event.getPlayer();
        checkPrivilegeFlag(caught.getLocation(), Flags.HOOK, player, event);
    }
}
