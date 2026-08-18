package cn.lunadeer.dominion.v1_20_1.events.environment.NaturalChanges;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.EntityBlockFormEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class IceForm implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(BlockFormEvent event) {
        if (event.isCancelled()) return;
        if (!event.getNewState().getType().name().endsWith("ICE")) {
            return;
        }
        EnvFlag flag = event instanceof EntityBlockFormEvent
                ? Flags.ICE_FORM_FROST_WALKER
                : Flags.ICE_FORM_NATURAL;
        checkEnvironmentFlag(event.getBlock().getLocation(), flag, event);
    }
}
