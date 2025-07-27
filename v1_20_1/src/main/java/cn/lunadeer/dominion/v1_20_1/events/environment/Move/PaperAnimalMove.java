package cn.lunadeer.dominion.v1_20_1.events.environment.Move;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.PaperOnly;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static cn.lunadeer.dominion.misc.Others.checkEnvironmentFlag;

@PaperOnly
public class PaperAnimalMove implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(EntityPathfindEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Animals)) {
            return;
        }
        checkEnvironmentFlag(event.getLoc(), Flags.ANIMAL_MOVE, event);
    }
}
