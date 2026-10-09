package dominion.compat.legacy;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

/** Compile only against the pre-fallback API; never add this source to Gradle's test sources. */
@SuppressWarnings("deprecation")
public final class LegacyFlagReader {
    private final DominionDTO dominion;
    private final Map<PriFlag, Boolean> retainedFlags;

    public LegacyFlagReader(DominionDTO dominion) {
        this.dominion = dominion;
        this.retainedFlags = dominion.getGuestPrivilegeFlagValue();
    }

    public void assertReads(boolean expected) {
        PriFlag legacy = Flags.CONTAINER;
        require(Flags.getPreFlag("container") == legacy, "legacy lookup by name");
        require(Flags.getAllPriFlags().contains(legacy), "legacy flag enumeration");
        require(dominion.getGuestFlagValue(legacy) == expected, "single flag getter");
        require(dominion.getGuestFlagValue(Flags.getPreFlag("container")) == expected, "getter by name");
        require(retainedFlags.containsKey(legacy), "Map.containsKey");
        require(retainedFlags.keySet().contains(legacy), "Map.keySet");
        require(retainedFlags.get(legacy) == expected, "Map.get with Boolean unboxing");
        require(retainedFlags.getOrDefault(legacy, !expected) == expected, "Map.getOrDefault");
        boolean found = false;
        for (Map.Entry<PriFlag, Boolean> entry : retainedFlags.entrySet()) {
            if (entry.getKey() == legacy) {
                require(entry.getValue() == expected, "Map.entrySet value");
                found = true;
            }
        }
        require(found, "Map.entrySet enumeration");
    }

    public void assertChecks(Location location, Player player, boolean expected) {
        DominionAPI api = DominionAPI.getInstance();
        require(api.checkPrivilegeFlagSilence(dominion, Flags.CONTAINER, player) == expected,
                "DominionAPI check using DTO");
        require(api.checkPrivilegeFlagSilence(location, Flags.CONTAINER, player) == expected,
                "DominionAPI check using location");
    }

    private static void require(boolean condition, String operation) {
        if (!condition) {
            throw new AssertionError("Legacy binary failed: " + operation);
        }
    }
}
