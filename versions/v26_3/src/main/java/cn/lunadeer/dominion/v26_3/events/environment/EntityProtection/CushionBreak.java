package cn.lunadeer.dominion.v26_3.events.environment.EntityProtection;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.events.PaperOnly;
import cn.lunadeer.dominion.utils.XVersionManager;
import cn.lunadeer.dominion.v26_3.protection.CushionBreakSource;
import io.papermc.paper.event.entity.EntityBreakEvent;
import org.bukkit.entity.Cushion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;
import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;

@PaperOnly
@LowestVersion(XVersionManager.ImplementationVersion.v26_3)
public class CushionBreak implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void handle(EntityBreakEvent event) {
        if (!(event.getEntity() instanceof Cushion)) return;
        CushionBreakSource source = CushionBreakSource.from(event);
        if (source.player() != null) {
            checkPrivilegeFlag(event.getEntity().getLocation(), Flags.CUSHION_BREAK, source.player(), event);
        } else {
            checkEnvironmentFlag(event.getEntity().getLocation(), (EnvFlag) source.flag(), event);
        }
    }
}
