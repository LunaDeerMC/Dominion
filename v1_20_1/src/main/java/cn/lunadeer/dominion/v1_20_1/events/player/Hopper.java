package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Hopper implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.HOPPER &&
                event.getClickedBlock().getType() != Material.DROPPER &&
                event.getClickedBlock().getType() != Material.DISPENSER &&
                event.getClickedBlock().getType() != Material.FURNACE &&
                event.getClickedBlock().getType() != Material.BLAST_FURNACE &&
                event.getClickedBlock().getType() != Material.SMOKER &&
                event.getClickedBlock().getType() != Material.FLOWER_POT
        ) {
            return;
        }
        checkPrivilegeFlag(event.getClickedBlock().getLocation(), Flags.HOPPER, event.getPlayer(), event);
    }
}
