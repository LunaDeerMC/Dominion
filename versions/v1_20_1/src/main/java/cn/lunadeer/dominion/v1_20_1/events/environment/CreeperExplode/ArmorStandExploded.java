package cn.lunadeer.dominion.v1_20_1.events.environment.CreeperExplode;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

public class ArmorStandExploded implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.ARMOR_STAND) {
            return;
        }
        Entity damager = event.getDamager();
        EnvFlag flag;
        if (damager instanceof Creeper) {
            flag = Flags.CREEPER_DAMAGE_ENTITY;
        } else if (damager instanceof WitherSkull) {
            flag = Flags.WITHER_SKULL_DAMAGE_ENTITY;
        } else if (damager instanceof EnderCrystal) {
            flag = Flags.ENDER_CRYSTAL_DAMAGE_ENTITY;
        } else if (damager instanceof Fireball) {
            flag = Flags.FIREBALL_DAMAGE_ENTITY;
        } else {
            return;
        }
        checkEnvironmentFlag(entity.getLocation(), flag, event);
    }
}
