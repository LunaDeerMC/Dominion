package cn.lunadeer.dominion.v26_3.events.player.Building;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.events.PaperOnly;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.entity.Cushion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

@PaperOnly
@LowestVersion(XVersionManager.ImplementationVersion.v26_3)
public class CushionPlace implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void handle(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Cushion) || event.getPlayer() == null) return;
        checkPrivilegeFlag(event.getEntity().getLocation(), Flags.CUSHION_PLACE, event.getPlayer(), event);
    }
}
