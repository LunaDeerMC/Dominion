package cn.lunadeer.dominion.v1_20_1.events.environment.NaturalChanges;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class NetherPortalCreate implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PortalCreateEvent event) {
        if (event.isCancelled()) return;
        if (event.getReason() != PortalCreateEvent.CreateReason.NETHER_PAIR) return;

        // Protect the destination regardless of the actor: entities can create exits,
        // and Folia may report a null entity even when a player initiated the portal.
        for (BlockState block : event.getBlocks()) {
            // Include blocks cleared to AIR as well as the portal frame and interior.
            if (!checkEnvironmentFlag(block.getLocation(), Flags.NETHER_PORTAL_CREATE, event)) return;
        }
    }
}
