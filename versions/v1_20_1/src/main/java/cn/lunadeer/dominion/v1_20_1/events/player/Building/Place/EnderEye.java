package cn.lunadeer.dominion.v1_20_1.events.player.Building.Place;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class EnderEye implements Listener {
    // Interact runs before item use: cancelling BlockPlaceEvent is too late to
    // prevent container drops from portal creation, even if blocks are restored.
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // Block-use denial alone can still allow item use; do not use isCancelled().
        if (event.useItemInHand() == Event.Result.DENY) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.END_PORTAL_FRAME) return;
        // The event item belongs to the interacting hand, including the off hand.
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_EYE) return;
        if (!(block.getBlockData() instanceof EndPortalFrame frame) || frame.hasEye()) return;
        checkPrivilegeFlag(block.getLocation(), Flags.PLACE, event.getPlayer(), event);
    }
}
