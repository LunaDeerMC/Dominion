package cn.lunadeer.dominion.v1_20_1.events.player.ResStone;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Comparer implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Material clicked = event.getClickedBlock().getType();
        if (clicked != Material.COMPARATOR) {
            return;
        }
        Player player = event.getPlayer();
        checkPrivilegeFlag(event.getClickedBlock().getLocation(), Flags.COMPARER, player, event);
    }
}
