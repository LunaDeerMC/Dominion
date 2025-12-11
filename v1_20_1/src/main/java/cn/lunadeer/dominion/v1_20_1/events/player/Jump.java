package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Jump implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerJumpEvent event) {
        if (event.isCancelled()) return;
        checkPrivilegeFlag(event.getPlayer().getLocation(), Flags.JUMP, event.getPlayer(), event);
    }
}
