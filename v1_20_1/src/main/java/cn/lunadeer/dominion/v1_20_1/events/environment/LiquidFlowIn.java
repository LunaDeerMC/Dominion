package cn.lunadeer.dominion.v1_20_1.events.environment;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import java.util.Objects;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class LiquidFlowIn implements Listener {
    @EventHandler(priority = EventPriority.LOWEST) // flow_in_protection
    public void handler(BlockFromToEvent event) {
        Location from = event.getBlock().getLocation();
        Location to = event.getToBlock().getLocation();
        DominionDTO dom_to = CacheManager.instance.getDominion(to);
        if (dom_to == null) {
            return;
        }
        DominionDTO dom_from = CacheManager.instance.getDominion(from);
        if (dom_from != null) {
            if (Objects.equals(dom_from.getId(), dom_to.getId())) {
                return;
            }
        }
        checkEnvironmentFlag(to, Flags.FLOW_IN_PROTECTION, event);
    }
}
