package cn.lunadeer.dominion.v26_2.nms;

import cn.lunadeer.dominion.nms.FakeEntity;
import cn.lunadeer.dominion.nms.FakeEntityFactory;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fake entity factory implementation for Minecraft 26.2+.
 * Uses self-maintained negative ID counter to avoid dependency on NMS.
 */
public class FakeEntityFactoryImpl implements FakeEntityFactory {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(-1);

    @Override
    public FakeEntity createBlockDisplay(Location location, BlockData blockData) {
        int entityId = nextEntityId();
        return new FakeEntityImpl(entityId, location, FakeEntity.DisplayType.BLOCK_DISPLAY, blockData, null);
    }

    @Override
    public FakeEntity createItemDisplay(Location location, ItemStack itemStack) {
        int entityId = nextEntityId();
        return new FakeEntityImpl(entityId, location, FakeEntity.DisplayType.ITEM_DISPLAY, null, itemStack);
    }

    @Override
    public int nextEntityId() {
        return NEXT_ID.decrementAndGet(); // -1, -2, -3, ...
    }
}
