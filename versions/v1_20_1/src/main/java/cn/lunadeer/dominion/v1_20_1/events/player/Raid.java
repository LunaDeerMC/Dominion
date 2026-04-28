package cn.lunadeer.dominion.v1_20_1.events.player;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidTriggerEvent;

import static cn.lunadeer.dominion.misc.Others.checkPrivilegeFlag;

public class Raid implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(RaidTriggerEvent event) {
        if (event.isCancelled()) return;
        checkPrivilegeFlag(event.getPlayer().getLocation(), Flags.RAID, event.getPlayer(), event);
    }
}
