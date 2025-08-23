package cn.lunadeer.dominion.v1_20_1.events.player.ResStone;

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

public class PressurePlate implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        checkPrivilegeFlag(block.getLocation(), Flags.PRESSURE, player, event);
    }
}
