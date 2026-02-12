package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class FallingGravityBlock implements Listener {
    private static final Map<UUID, Location> fallingBlockMap = new java.util.HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityChangeBlockEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock)) {
            return;
        }
        Block block = event.getBlock();
        if (event.getTo().isAir()) {
            fallingBlockMap.put(entity.getUniqueId(), block.getLocation());
        } else {
            Location locStart = fallingBlockMap.get(entity.getUniqueId());
            if (locStart == null) {
                return;
            }
            fallingBlockMap.remove(entity.getUniqueId());
            Location locEnd = block.getLocation();
            DominionDTO domStart = CacheManager.instance.getDominion(locStart);
            DominionDTO domEnd = CacheManager.instance.getDominion(locEnd);
            if (domEnd == null) {
                return;
            }
            if (domStart != null && domStart.getId().equals(domEnd.getId())) {
                return;
            }
            if (!checkEnvironmentFlag(locEnd, Flags.GRAVITY_BLOCK, null)) {
                event.setCancelled(true);
                locEnd.getWorld().dropItemNaturally(locEnd, new ItemStack(((FallingBlock) entity).getBlockData().getMaterial()));
                entity.remove();
            }
        }
    }
}
