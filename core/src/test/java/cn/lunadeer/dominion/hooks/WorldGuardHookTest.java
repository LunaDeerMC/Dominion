package cn.lunadeer.dominion.hooks;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.SimpleFlagRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldGuardHookTest {

    @Test
    void registersClaimingFlagWithNoDefaultValue() {
        SimpleFlagRegistry registry = new SimpleFlagRegistry();

        WorldGuardHook.FlagRegistration registration = WorldGuardHook.registerFlag(registry);

        assertTrue(registration.newlyRegistered());
        assertSame(registration.flag(), registry.get(WorldGuardHook.CLAIMING_FLAG_NAME));
        assertNull(registration.flag().getDefault());
    }

    @Test
    void reusesExistingStateFlagInstance() throws Exception {
        SimpleFlagRegistry registry = new SimpleFlagRegistry();
        StateFlag existing = new StateFlag(WorldGuardHook.CLAIMING_FLAG_NAME, false);
        registry.register(existing);

        WorldGuardHook.FlagRegistration registration = WorldGuardHook.registerFlag(registry);

        assertFalse(registration.newlyRegistered());
        assertSame(existing, registration.flag());
    }

    @Test
    void rejectsExistingFlagWithIncompatibleType() throws Exception {
        SimpleFlagRegistry registry = new SimpleFlagRegistry();
        registry.register(new StringFlag(WorldGuardHook.CLAIMING_FLAG_NAME));

        assertThrows(IllegalStateException.class, () -> WorldGuardHook.registerFlag(registry));
    }
}
