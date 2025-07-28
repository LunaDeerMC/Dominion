package cn.lunadeer.dominion.v1_20_1.events.player.Harvest;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class HarvestBlock implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        checkPrivilegeFlag(event.getHarvestedBlock().getLocation(), Flags.HARVEST, player, event);
    }
}
