package cn.lunadeer.dominion.v1_20_1.events.player.Sign;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Save implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        checkPrivilegeFlag(block.getLocation(), Flags.EDIT_SIGN, player, event);
    }
}
