package cn.lunadeer.dominion.v26_2.events.environment.CreeperExplode;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

@LowestVersion(XVersionManager.ImplementationVersion.v26_2)
public class ItemFrameExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(HangingBreakByEntityEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION) return;
        if (!(event.getRemover() instanceof SulfurCube)) return;
        Entity entity = event.getEntity();
        checkEnvironmentFlag(entity.getLocation(), Flags.CREEPER_DAMAGE_ENTITY, event);
    }
}
