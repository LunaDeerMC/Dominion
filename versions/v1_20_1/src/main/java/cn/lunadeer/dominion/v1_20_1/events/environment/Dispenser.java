package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;

public class Dispenser implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockDispenseEvent event) {
        if (event.isCancelled()) return;
        if (!event.getBlock().getType().equals(Material.DISPENSER)) return;
        BlockFace blockFace = null;
        DominionDTO dispenserDom = CacheManager.instance.getDominion(event.getBlock().getLocation());
        if (dispenserDom != null) return;   // if the dispenser itself is in a dominion, we don't care about it
        if (event.getBlock().getBlockData() instanceof Directional directional) {
            blockFace = directional.getFacing();
        }
        if (blockFace == null) return;
        DominionDTO dominion = CacheManager.instance.getDominion(event.getBlock().getRelative(blockFace).getLocation());
        if (dominion == null) return;
        event.setCancelled(true);   // stop the dispenser from dispensing items into the dominion
    }

}
