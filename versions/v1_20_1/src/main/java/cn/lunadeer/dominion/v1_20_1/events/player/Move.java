package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.managers.TeleportManager;
import org.bukkit.Location;
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
        DominionDTO dom = CacheManager.instance.getPlayerCurrentDominion(player);
        if (!checkPrivilegeFlag(player.getLocation(), Flags.MOVE, player, null)) {
            Location to = player.getLocation();
            assert dom != null;
            int x1 = Math.abs(to.getBlockX() - dom.getCuboid().x1());
            int x2 = Math.abs(to.getBlockX() - dom.getCuboid().x2());
            int z1 = Math.abs(to.getBlockZ() - dom.getCuboid().z1());
            int z2 = Math.abs(to.getBlockZ() - dom.getCuboid().z2());
            // find min distance
            int min = Math.min(Math.min(x1, x2), Math.min(z1, z2));
            if (min == x1) {
                to.setX(dom.getCuboid().x1() - 2);
            } else if (min == x2) {
                to.setX(dom.getCuboid().x2() + 2);
            } else if (min == z1) {
                to.setZ(dom.getCuboid().z1() - 2);
            } else {
                to.setZ(dom.getCuboid().z2() + 2);
            }
            TeleportManager.doTeleportSafely(player, to);
        }
    }
}
