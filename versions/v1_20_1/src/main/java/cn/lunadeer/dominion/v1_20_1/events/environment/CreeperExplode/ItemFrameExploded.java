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
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class ItemFrameExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(HangingBreakByEntityEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION) {
            return;
        }
        Entity remover = event.getRemover();
        EnvFlag flag;
        if (remover instanceof Creeper) {
            flag = Flags.CREEPER_DAMAGE_ENTITY;
        } else if (remover instanceof WitherSkull) {
            flag = Flags.WITHER_SKULL_DAMAGE_ENTITY;
        } else if (remover instanceof EnderCrystal) {
            flag = Flags.ENDER_CRYSTAL_DAMAGE_ENTITY;
        } else if (remover instanceof Fireball) {
            flag = Flags.FIREBALL_DAMAGE_ENTITY;
        } else {
            return;
        }
        checkEnvironmentFlag(entity.getLocation(), flag, event);
    }
}
