package cn.lunadeer.dominion.v1_20_1.events.environment.CreeperExplode;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class EntityExplode implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        EnvFlag flag;
        if (entity instanceof Creeper) {
            flag = Flags.CREEPER_EXPLODE;
        } else if (entity instanceof WitherSkull) {
            flag = Flags.WITHER_SKULL_EXPLODE;
        } else if (entity instanceof EnderCrystal) {
            flag = Flags.ENDER_CRYSTAL_EXPLODE;
        } else if (entity instanceof Fireball) {
            flag = Flags.FIREBALL_EXPLODE;
        } else {
            return;
        }
        event.blockList().removeIf(block -> !checkEnvironmentFlag(block.getLocation(), flag, null));
    }
}
