package cn.lunadeer.dominion.v1_20_1.events.environment.CreatureBehavior.AnimalMove;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.SpigotOnly;
import cn.lunadeer.dominion.v1_20_1.events.environment.CreatureBehavior.SpigotEntityMove;
import org.bukkit.entity.Animals;
import org.bukkit.event.Listener;

@SpigotOnly
public class SpigotAnimalMove implements Listener {
    public SpigotAnimalMove() {
        SpigotEntityMove.track(Animals.class, Flags.ANIMAL_MOVE);
    }
}
