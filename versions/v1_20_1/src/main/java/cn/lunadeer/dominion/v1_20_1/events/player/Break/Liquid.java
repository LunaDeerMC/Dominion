package cn.lunadeer.dominion.v1_20_1.events.player.Break;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketFillEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Liquid implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(PlayerBucketFillEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        checkPrivilegeFlag(event.getBlockClicked().getLocation(), Flags.BREAK_BLOCK, player, event);
    }
}
