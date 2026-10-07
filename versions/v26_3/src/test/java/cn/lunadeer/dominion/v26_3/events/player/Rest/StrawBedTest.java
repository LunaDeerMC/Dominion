package cn.lunadeer.dominion.v26_3.events.player.Rest;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

class StrawBedTest {
    @Test
    void strawBedUsesExistingBedPrivilegeAtClickedLocation() {
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        Location location = new Location(null, 3, 70, 5);
        when(block.getType()).thenReturn(Material.STRAW_BED);
        when(block.getLocation()).thenReturn(location);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            new StrawBed().handle(event);
            others.verify(() -> Others.checkPrivilegeFlag(location, Flags.BED, player, event));
            others.verifyNoMoreInteractions();
        }
    }

    @Test
    void unrelatedBlocksAndActionsDoNotCheckBedPrivilege() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Block block = mock(Block.class);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(block.getType()).thenReturn(Material.STONE);
        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            StrawBed listener = new StrawBed();
            listener.handle(event);
            when(block.getType()).thenReturn(Material.STRAW_BED);
            when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
            listener.handle(event);
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
            when(event.getClickedBlock()).thenReturn(null);
            listener.handle(event);
            others.verifyNoInteractions();
        }
    }
}
