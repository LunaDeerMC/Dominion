package cn.lunadeer.dominion.misc;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.WorldWide;
import cn.lunadeer.dominion.doos.DominionDOO;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.util.UUID;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EnderCrystalPrivilegeTest {
    private CacheManager previousCache;
    private boolean previousEnabled;
    private boolean previousDefault;
    private DominionDOO dominion;
    private Player player;
    private World world;
    private Location location;

    @BeforeEach
    void setUp() {
        previousCache = CacheManager.instance;
        previousEnabled = Flags.PLACE_ENDER_CRYSTAL.getEnable();
        previousDefault = Flags.PLACE_ENDER_CRYSTAL.getDefaultValue();
        Flags.PLACE_ENDER_CRYSTAL.setEnable(true);
        Flags.PLACE_ENDER_CRYSTAL.setDefaultValue(false);
        CacheManager.instance = mock(CacheManager.class);
        dominion = new DominionDOO(UUID.randomUUID(), "test", UUID.randomUUID(), CuboidDTO.ZERO, -1);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        world = mock(World.class);
        location = new Location(world, 0.5, 65, 3.5);
        when(CacheManager.instance.getDominion(location)).thenReturn(dominion);
        dominion.getGuestPrivilegeFlagValue().put(Flags.PLACE_ENDER_CRYSTAL, false);
    }

    @AfterEach
    void tearDown() {
        CacheManager.instance = previousCache;
        Flags.PLACE_ENDER_CRYSTAL.setEnable(previousEnabled);
        Flags.PLACE_ENDER_CRYSTAL.setDefaultValue(previousDefault);
    }

    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void guestCrystalPermissionIsIndependentOfOrdinaryBlockPlacement(boolean blocksAllowed, boolean crystalsAllowed) {
        dominion.getGuestPrivilegeFlagValue().put(Flags.PLACE, blocksAllowed);
        dominion.getGuestPrivilegeFlagValue().put(Flags.PLACE_ENDER_CRYSTAL, crystalsAllowed);

        assertPermission(crystalsAllowed);
    }

    @Test
    void missingCrystalPermissionUsesItsDenyingDefault() {
        dominion.getGuestPrivilegeFlagValue().put(Flags.PLACE, true);
        dominion.getGuestPrivilegeFlagValue().remove(Flags.PLACE_ENDER_CRYSTAL);

        assertPermission(false);
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void usesMemberOrGroupPermissionInsteadOfTheGuestValue(boolean grouped, boolean allowed) {
        dominion.getGuestPrivilegeFlagValue().put(Flags.PLACE_ENDER_CRYSTAL, !allowed);
        MemberDTO member = member(grouped ? 7 : -1);
        when(member.getFlagValue(Flags.PLACE_ENDER_CRYSTAL)).thenReturn(grouped ? !allowed : allowed);
        if (grouped) {
            GroupDTO group = mock(GroupDTO.class);
            when(CacheManager.instance.getGroup(7)).thenReturn(group);
            when(group.getFlagValue(Flags.PLACE_ENDER_CRYSTAL)).thenReturn(allowed);
        }

        assertPermission(allowed);
    }

    enum Bypass { OWNER, OP, SERVER_ADMIN, MEMBER_ADMIN, GROUP_ADMIN }

    @ParameterizedTest
    @EnumSource(Bypass.class)
    void preservesExistingOwnerAndAdministratorExemptions(Bypass bypass) {
        switch (bypass) {
            case OWNER -> when(player.getUniqueId()).thenReturn(dominion.getOwner());
            case OP -> when(player.isOp()).thenReturn(true);
            case SERVER_ADMIN -> when(player.hasPermission(adminPermission)).thenReturn(true);
            case MEMBER_ADMIN -> when(member(-1).getFlagValue(Flags.ADMIN)).thenReturn(true);
            case GROUP_ADMIN -> {
                member(7);
                GroupDTO group = mock(GroupDTO.class);
                when(CacheManager.instance.getGroup(7)).thenReturn(group);
                when(group.getFlagValue(Flags.ADMIN)).thenReturn(true);
            }
        }

        assertPermission(true);
    }

    @ParameterizedTest
    @CsvSource({"false, false, true", "false, true, true", "true, false, false", "true, true, true"})
    void wildernessUsesTheWorldWideSwitchAndCrystalPermission(boolean enabled, boolean allowed, boolean expected) {
        when(CacheManager.instance.getDominion(location)).thenReturn(null);
        try (MockedStatic<WorldWide> worldWide = mockStatic(WorldWide.class)) {
            worldWide.when(() -> WorldWide.isWorldWideEnabled(world)).thenReturn(enabled);
            worldWide.when(() -> WorldWide.getGuestFlagValue(world, Flags.PLACE_ENDER_CRYSTAL)).thenReturn(allowed);

            assertPermission(expected);
        }
    }

    @Test
    void disablingTheFlagStillDisablesItsProtectionCheck() {
        Flags.PLACE_ENDER_CRYSTAL.setEnable(false);

        assertPermission(true);
        verifyNoInteractions(CacheManager.instance);
    }

    private MemberDTO member(int groupId) {
        MemberDTO member = mock(MemberDTO.class);
        when(member.getGroupId()).thenReturn(groupId);
        when(CacheManager.instance.getMember(dominion, player)).thenReturn(member);
        return member;
    }

    private void assertPermission(boolean expected) {
        Cancellation event = new Cancellation();
        assertEquals(expected, Others.checkPrivilegeFlagSilence(location, Flags.PLACE_ENDER_CRYSTAL, player, event));
        assertEquals(!expected, event.isCancelled());
    }

    private static class Cancellation implements Cancellable {
        private boolean cancelled;

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}
