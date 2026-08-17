package cn.lunadeer.dominion.v1_20_1.events.player.Storage;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class ShulkerBox implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || !Tag.SHULKER_BOXES.isTagged(event.getClickedBlock().getType())) return;
        checkPrivilegeFlag(event.getClickedBlock().getLocation(), Flags.SHULKER_BOX, event.getPlayer(), event);
    }
}
