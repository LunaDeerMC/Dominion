package cn.lunadeer.dominion.v1_21_8.nms;

import cn.lunadeer.dominion.nms.EntityIdAllocator;
import cn.lunadeer.dominion.nms.FakeEntity;
import cn.lunadeer.dominion.nms.FakeEntityFactory;
import net.minecraft.world.entity.Entity;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

/**
 * Fake entity factory implementation for Minecraft 1.21.8.
 */
public class FakeEntityFactoryImpl implements FakeEntityFactory {

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
        return EntityIdAllocator.nextEntityId(Entity.class);
    }
}
