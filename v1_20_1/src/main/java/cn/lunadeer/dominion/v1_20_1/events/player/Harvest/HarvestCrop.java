package cn.lunadeer.dominion.v1_20_1.events.player.Harvest;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class HarvestCrop implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!Tag.CROPS.isTagged(event.getBlock().getType())) return;
        Player player = event.getPlayer();
        checkPrivilegeFlag(block.getLocation(), Flags.HARVEST, player, event);
    }
}
