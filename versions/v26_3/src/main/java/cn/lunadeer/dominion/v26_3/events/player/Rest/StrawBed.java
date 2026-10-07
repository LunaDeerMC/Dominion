package cn.lunadeer.dominion.v26_3.events.player.Rest;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.LowestVersion;
import cn.lunadeer.dominion.utils.XVersionManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

/** Straw beds are not included in Minecraft's beds tag. */
@LowestVersion(XVersionManager.ImplementationVersion.v26_3)
public class StrawBed implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void handle(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.STRAW_BED) return;
        checkPrivilegeFlag(event.getClickedBlock().getLocation(), Flags.BED, event.getPlayer(), event);
    }
}
