package cn.lunadeer.dominion.v1_20_1.events.player.Break;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class ItemFrameBroken implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(HangingBreakByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getRemover() instanceof Player player)) {
            return;
        }
        Entity entity = event.getEntity();
        if (event.getCause() != HangingBreakEvent.RemoveCause.ENTITY) {
            return;
        }
        if (entity instanceof ItemFrame) {
            if (((ItemFrame) entity).getItem().getType() != Material.AIR) {
                // ItemFrame is not empty, handle it as a container
                return;
            }
        }
        checkPrivilegeFlag(entity.getLocation(), Flags.BREAK_BLOCK, player, event);
    }
}
