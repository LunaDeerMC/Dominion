package cn.lunadeer.dominion.v1_20_1.events.player.Break;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class NormalBlock implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (Tag.CROPS.isTagged(event.getBlock().getType())) return;
        checkPrivilegeFlag(event.getBlock().getLocation(), Flags.BREAK_BLOCK, player, event);
    }
}
