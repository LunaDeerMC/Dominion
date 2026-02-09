package cn.lunadeer.dominion.nms;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Factory interface for creating client-side fake entities via NMS packets.
 * <p>
 * Fake entities only exist on the client side - they are never spawned as real server entities.
 * This makes them invisible to other plugins and eliminates server-side performance overhead.
 * <p>
 * Version-specific implementations handle the differences in NMS packet construction
 * across Minecraft versions. Use {@link NMSManager#getFakeEntityFactory()} to obtain
 * the current implementation.
 */
public interface FakeEntityFactory {

    /**
     * Create a client-side BlockDisplay entity.
     *
     * @param location  the location to spawn the entity at
     * @param blockData the block data to display
     * @return a {@link FakeEntity} handle for further manipulation
     */
    FakeEntity createBlockDisplay(Location location, BlockData blockData);

    /**
     * Create a client-side ItemDisplay entity.
     *
     * @param location  the location to spawn the entity at
     * @param itemStack the item stack to display
     * @return a {@link FakeEntity} handle for further manipulation
     */
    FakeEntity createItemDisplay(Location location, ItemStack itemStack);

    /**
     * Generate a unique entity ID that won't conflict with real server entities.
     *
     * @return a new unique entity ID
     */
    int nextEntityId();
}
