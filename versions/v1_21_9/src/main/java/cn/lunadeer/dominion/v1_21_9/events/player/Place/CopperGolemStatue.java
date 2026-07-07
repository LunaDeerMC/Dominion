package cn.lunadeer.dominion.v1_21_9.events.player.Place;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

@LowestVersion(XVersionManager.ImplementationVersion.v1_21_9)
public class CopperGolemStatue implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!Tag.COPPER_GOLEM_STATUES.isTagged(event.getClickedBlock().getType())) {
            return;
        }
        checkPrivilegeFlag(event.getClickedBlock().getLocation(), Flags.PLACE, event.getPlayer(), event);
    }
}
