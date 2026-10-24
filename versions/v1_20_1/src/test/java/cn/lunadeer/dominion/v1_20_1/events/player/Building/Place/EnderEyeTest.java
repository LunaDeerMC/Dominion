package cn.lunadeer.dominion.v1_20_1.events.player.Building.Place;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EnderEyeTest {
    private final EnderEye listener = new EnderEye();
    private MockedStatic<Others> others;
    private Player player;
    private Block frame;
    private EndPortalFrame data;
    private Location frameLocation;

    @BeforeEach
    void setUp() {
        others = mockStatic(Others.class);
        player = mock(Player.class);
        frame = mock(Block.class);
        data = mock(EndPortalFrame.class);
        frameLocation = new Location(mock(World.class), 100, 64, 200);
        when(frame.getType()).thenReturn(Material.END_PORTAL_FRAME);
        when(frame.getLocation()).thenReturn(frameLocation);
        when(frame.getBlockData()).thenReturn(data);
    }

    @AfterEach
    void tearDown() {
        others.close();
    }

    @ParameterizedTest
    @CsvSource({"HAND, false", "OFF_HAND, false", "HAND, true", "OFF_HAND, true"})
    void checksFramePlacementPermissionForTheActualInteractionHand(EquipmentSlot hand, boolean allowed) {
        PlayerInteractEvent event = interaction(Action.RIGHT_CLICK_BLOCK, Material.ENDER_EYE, frame, hand);
        Event.Result blockUse = event.useInteractedBlock();
        Event.Result itemUse = event.useItemInHand();
        permission(event, allowed);

        listener.handler(event);

        assertEquals(allowed ? blockUse : Event.Result.DENY, event.useInteractedBlock());
        assertEquals(allowed ? itemUse : Event.Result.DENY, event.useItemInHand());
        verifyPermission(event);
        // Neither the player's position nor the main-hand inventory determines this check.
        verifyNoInteractions(player);
    }

    @ParameterizedTest
    @CsvSource({"DEFAULT, false", "ALLOW, false", "DEFAULT, true", "ALLOW, true"})
    void blockUseDenialDoesNotSkipAnEyeWhoseItemUseCanStillRun(Event.Result itemUse, boolean allowed) {
        PlayerInteractEvent event = interaction(Action.RIGHT_CLICK_BLOCK, Material.ENDER_EYE, frame, EquipmentSlot.OFF_HAND);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(itemUse);
        assertTrue(event.isCancelled(), "Bukkit cancellation only reflects the block-use result");
        permission(event, allowed);

        listener.handler(event);

        assertEquals(Event.Result.DENY, event.useInteractedBlock());
        assertEquals(allowed ? itemUse : Event.Result.DENY, event.useItemInHand());
        verifyPermission(event);
    }

    @ParameterizedTest
    @EnumSource(Event.Result.class)
    void preservesAlreadyDeniedItemUseWithoutRecheckingPermission(Event.Result blockUse) {
        PlayerInteractEvent event = interaction(Action.RIGHT_CLICK_BLOCK, Material.ENDER_EYE, frame, EquipmentSlot.HAND);
        event.setUseInteractedBlock(blockUse);
        event.setUseItemInHand(Event.Result.DENY);

        listener.handler(event);

        assertEquals(blockUse, event.useInteractedBlock());
        assertEquals(Event.Result.DENY, event.useItemInHand());
        others.verifyNoInteractions();
    }

    @Test
    void leavesAnAlreadyFilledFrameUntouched() {
        when(data.hasEye()).thenReturn(true);
        assertIgnored(interaction(Action.RIGHT_CLICK_BLOCK, Material.ENDER_EYE, frame, EquipmentSlot.HAND));
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {"ENDER_PEARL", "STONE", "AIR"})
    void doesNotTreatOtherItemsAsEyesOfEnder(Material material) {
        assertIgnored(interaction(Action.RIGHT_CLICK_BLOCK, material, frame, EquipmentSlot.OFF_HAND));
    }

    @Test
    void leavesAnEmptyHandUntouched() {
        assertIgnored(interaction(Action.RIGHT_CLICK_BLOCK, null, frame, EquipmentSlot.HAND));
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {"STONE", "CHEST", "END_PORTAL"})
    void leavesOtherClickedBlocksUntouched(Material material) {
        when(frame.getType()).thenReturn(material);
        assertIgnored(interaction(Action.RIGHT_CLICK_BLOCK, Material.ENDER_EYE, frame, EquipmentSlot.HAND));
    }

    @ParameterizedTest
    @EnumSource(value = Action.class, names = {"LEFT_CLICK_BLOCK", "LEFT_CLICK_AIR", "RIGHT_CLICK_AIR", "PHYSICAL"})
    void leavesOtherActionsUntouched(Action action) {
        assertIgnored(interaction(action, Material.ENDER_EYE, frame, EquipmentSlot.HAND));
    }

    @Test
    void toleratesAnInteractionWithoutAClickedBlock() {
        assertIgnored(interaction(Action.RIGHT_CLICK_BLOCK, Material.ENDER_EYE, null, EquipmentSlot.HAND));
    }

    @Test
    void receivesPartiallyDeniedInteractionsAtTheEarliestInteractionPriority() throws Exception {
        EventHandler handler = EnderEye.class.getMethod("handler", PlayerInteractEvent.class)
                .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.LOWEST, handler.priority());
        assertFalse(handler.ignoreCancelled(), "A block-use denial must not bypass the item-use check");
    }

    private PlayerInteractEvent interaction(Action action, Material material, Block block, EquipmentSlot hand) {
        ItemStack item = material == null ? null : new ItemStack(material);
        return new PlayerInteractEvent(player, action, item, block, BlockFace.UP, hand);
    }

    private void permission(PlayerInteractEvent event, boolean allowed) {
        others.when(() -> Others.checkPrivilegeFlag(frameLocation, Flags.PLACE, player, event)).thenAnswer(invocation -> {
            if (!allowed) event.setCancelled(true);
            return allowed;
        });
    }

    private void verifyPermission(PlayerInteractEvent event) {
        others.verify(() -> Others.checkPrivilegeFlag(frameLocation, Flags.PLACE, player, event));
        others.verifyNoMoreInteractions();
    }

    private void assertIgnored(PlayerInteractEvent event) {
        Event.Result blockUse = event.useInteractedBlock();
        Event.Result itemUse = event.useItemInHand();
        listener.handler(event);
        assertEquals(blockUse, event.useInteractedBlock());
        assertEquals(itemUse, event.useItemInHand());
        others.verifyNoInteractions();
    }
}
