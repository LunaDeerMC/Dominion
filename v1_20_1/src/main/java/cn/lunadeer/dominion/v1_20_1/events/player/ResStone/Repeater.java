package cn.lunadeer.dominion.v1_20_1.events.player.ResStone;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Repeater implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        Material clicked = block.getType();
        if (clicked != Material.REPEATER) {
            return;
        }
        Player player = event.getPlayer();
        checkPrivilegeFlag(block.getLocation(), Flags.REPEATER, player, event);
    }
}
