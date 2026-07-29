package cn.lunadeer.dominion.v1_20_1.events.environment.CreatureBehavior.MonsterMove;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.events.SpigotOnly;
import cn.lunadeer.dominion.v1_20_1.events.environment.CreatureBehavior.SpigotEntityMove;
import org.bukkit.entity.Monster;
import org.bukkit.event.Listener;

@SpigotOnly
public class SpigotMonsterMove implements Listener {
    public SpigotMonsterMove() {
        SpigotEntityMove.track(Monster.class, Flags.MONSTER_MOVE);
    }
}
