package cn.lunadeer.dominion.v1_20_1.events.player.Combat.PVP;

import cn.lunadeer.dominion.v1_20_1.protection.PotionProtection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

public class CloudEffect implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void handler(AreaEffectCloudApplyEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity().getSource() instanceof Player attacker)) {
            return;
        }

        var effects = PotionProtection.effectTypes(event.getEntity());
        event.getAffectedEntities().removeIf(target -> PotionProtection.blocks(attacker, target, effects));
    }
}
