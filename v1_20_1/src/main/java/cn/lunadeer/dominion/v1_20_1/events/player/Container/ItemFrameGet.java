package cn.lunadeer.dominion.v1_20_1.events.player.Container;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class ItemFrameGet implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame itemFrame)) {
            return;
        }
        if (itemFrame.getItem().getType() == Material.AIR) {
            return;
        }
        if (!(event.getDamager() instanceof Player bukkitPlayer)) {
            return;
        }
        checkPrivilegeFlag(itemFrame.getLocation(), Flags.CONTAINER, bukkitPlayer, event);
    }
}
