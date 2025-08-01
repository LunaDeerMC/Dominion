package cn.lunadeer.dominion.v1_20_1.events.environment.Trample;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;
import static org.bukkit.Material.FARMLAND;

public class ByPlayer implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != FARMLAND) {
            return;
        }
        checkEnvironmentFlag(block.getLocation(), Flags.TRAMPLE, event);
    }
}
