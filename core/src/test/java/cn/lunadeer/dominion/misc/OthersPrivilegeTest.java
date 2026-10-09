package cn.lunadeer.dominion.misc;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.DominionDOO;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OthersPrivilegeTest {
    private CacheManager previousCache;
    private DominionDOO dominion;
    private Player player;
    private Location location;
    private boolean chestEnabled;

    @BeforeEach
    void setUp() {
        previousCache = CacheManager.instance;
        chestEnabled = Flags.CHEST.getEnable();
        Flags.CHEST.setEnable(true);
        CacheManager.instance = mock(CacheManager.class);
        dominion = new DominionDOO(UUID.randomUUID(), "test", UUID.randomUUID(), CuboidDTO.ZERO, -1);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        location = new Location(null, 0, 0, 0);
        when(CacheManager.instance.getDominion(location)).thenReturn(dominion);
    }

    @AfterEach
    void tearDown() {
        CacheManager.instance = previousCache;
        Flags.CHEST.setEnable(chestEnabled);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingTargetFlagDeniesGuestWithoutThrowing(boolean useLocation) {
        dominion.getGuestPrivilegeFlagValue().remove(Flags.CHEST);
        assertFalse(dominion.getGuestPrivilegeFlagValue().containsKey(Flags.CONTAINER));
        Cancellable event = mock(Cancellable.class);

        assertFalse(check(useLocation, Flags.CONTAINER, event));
        verify(event).setCancelled(true);
        assertFalse(check(useLocation, Flags.CONTAINER, null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void existingGuestPermissionIsRespected(boolean useLocation) {
        for (boolean allowed : new boolean[]{false, true}) {
            dominion.getGuestPrivilegeFlagValue().put(Flags.CHEST, allowed);
            Cancellable event = mock(Cancellable.class);

            assertEquals(allowed, check(useLocation, Flags.CONTAINER, event));
            if (allowed) {
                verifyNoInteractions(event);
            } else {
                verify(event).setCancelled(true);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ownerStillBypassesMissingFlag(boolean useLocation) {
        dominion.getGuestPrivilegeFlagValue().remove(Flags.CHEST);
        when(player.getUniqueId()).thenReturn(dominion.getOwner());
        Cancellable event = mock(Cancellable.class);

        assertTrue(check(useLocation, Flags.CONTAINER, event));
        verifyNoInteractions(event);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void legacyFlagUsesMemberAndGroupChestPrivileges(boolean useLocation) {
        MemberDTO member = mock(MemberDTO.class);
        when(CacheManager.instance.getMember(dominion, player)).thenReturn(member);
        when(member.getGroupId()).thenReturn(-1);
        when(member.getFlagValue(Flags.CHEST)).thenReturn(true);
        assertTrue(check(useLocation, Flags.CONTAINER, null));
        when(member.getFlagValue(Flags.CHEST)).thenReturn(false);
        assertFalse(check(useLocation, Flags.CONTAINER, null));

        GroupDTO group = mock(GroupDTO.class);
        when(member.getGroupId()).thenReturn(7);
        when(CacheManager.instance.getGroup(7)).thenReturn(group);
        when(group.getFlagValue(Flags.CHEST)).thenReturn(true);
        assertTrue(check(useLocation, Flags.CONTAINER, null));
        when(group.getFlagValue(Flags.CHEST)).thenReturn(false);
        assertFalse(check(useLocation, Flags.CONTAINER, null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void legacyFlagUsesTargetEnableSetting(boolean useLocation) {
        dominion.getGuestPrivilegeFlagValue().put(Flags.CHEST, false);
        assertFalse(check(useLocation, Flags.CONTAINER, null));
        Flags.CHEST.setEnable(false);
        assertTrue(check(useLocation, Flags.CONTAINER, null));
    }

    private boolean check(boolean useLocation, PriFlag flag, Cancellable event) {
        return useLocation
                ? Others.checkPrivilegeFlagSilence(location, flag, player, event)
                : Others.checkPrivilegeFlagSilence(dominion, flag, player, event);
    }
}
