package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Feed implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getRightClicked() instanceof Animals)) {
            return;
        }
        // if shearing sheep instead
        if (event.getPlayer().getInventory().getItem(event.getHand()).getType() == Material.SHEARS) {
            return;
        }
        Player player = event.getPlayer();
        checkPrivilegeFlag(event.getRightClicked().getLocation(), Flags.FEED, player, event);
    }
}
