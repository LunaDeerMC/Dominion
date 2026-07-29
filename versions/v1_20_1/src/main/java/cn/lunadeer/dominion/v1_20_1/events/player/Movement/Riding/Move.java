package cn.lunadeer.dominion.v1_20_1.events.player.Movement.Riding;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Move implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (!player.isInsideVehicle()) return;
        if (!checkPrivilegeFlag(player.getLocation(), Flags.RIDING, player, event)) {
            player.leaveVehicle();
        }
    }
}
