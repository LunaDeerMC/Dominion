package cn.lunadeer.dominion.v1_20_1.events.player.Access;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class FenceGate implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !Tag.FENCE_GATES.isTagged(block.getType())) return;
        Player player = event.getPlayer();
        checkPrivilegeFlag(block.getLocation(), Flags.FENCE_GATE, player, event);
    }
}
