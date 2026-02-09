package cn.lunadeer.dominion.v1_21_4.nms;

import cn.lunadeer.dominion.nms.FakeEntity;
import cn.lunadeer.dominion.nms.FakeEntityFactory;
import net.minecraft.world.entity.Entity;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fake entity factory implementation for Minecraft 1.21.4.
 */
public class FakeEntityFactoryImpl implements FakeEntityFactory {

    private static final AtomicInteger ENTITY_COUNTER;

    static {
        try {
            Field counterField = Entity.class.getDeclaredField("ENTITY_COUNTER");
            counterField.setAccessible(true);
            ENTITY_COUNTER = (AtomicInteger) counterField.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError("Failed to access Entity.ENTITY_COUNTER: " + e.getMessage());
        }
    }

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
        return ENTITY_COUNTER.incrementAndGet();
    }
}
