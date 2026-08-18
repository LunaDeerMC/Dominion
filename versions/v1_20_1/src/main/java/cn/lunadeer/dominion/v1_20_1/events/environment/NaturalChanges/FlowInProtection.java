package cn.lunadeer.dominion.v1_20_1.events.environment.NaturalChanges;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import java.util.Objects;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class FlowInProtection implements Listener {
    @EventHandler(priority = EventPriority.LOWEST) // flow_in_protection
    public void handler(BlockFromToEvent event) {
        if (event.isCancelled()) return;
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
        Material source = event.getBlock().getType();
        EnvFlag flag;
        if (source == Material.LAVA) {
            flag = Flags.FLOW_IN_LAVA;
        } else if (source == Material.WATER || source == Material.BUBBLE_COLUMN) {
            flag = Flags.FLOW_IN_WATER;
        } else {
            return;
        }
        checkEnvironmentFlag(to, flag, event);
    }
}
