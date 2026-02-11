package cn.lunadeer.dominion.v1_20_1.nms;

import cn.lunadeer.dominion.nms.FakeEntity;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Fake entity implementation for Minecraft 1.20.1.
 * <p>
 * Metadata indices for Display entities in 1.20.1 (no pos/rot interpolation duration):
 * <pre>
 *  8: INT   - interpolation start delay
 *  9: INT   - interpolation duration
 * 10: VEC3  - translation
 * 11: VEC3  - scale
 * 12: QUAT  - left rotation
 * 13: QUAT  - right rotation
 * 14: BYTE  - billboard
 * 15: INT   - brightness override
 * 16: FLOAT - view range
 * 17: FLOAT - shadow radius
 * 18: FLOAT - shadow strength
 * 19: FLOAT - width
 * 20: FLOAT - height
 * 21: INT   - glow color override
 * BlockDisplay 22: BLOCK_STATE
 * ItemDisplay  22: ITEM_STACK, 23: BYTE (item transform)
 * </pre>
 */
public class FakeEntityImpl implements FakeEntity {

    // ==================== Metadata Accessors (1.20.1 indices) ====================
    // These are created manually because Display.DATA_*_ID fields are private.

    private static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID =
            new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> INTERPOLATION_START =
            new EntityDataAccessor<>(8, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> INTERPOLATION_DURATION =
            new EntityDataAccessor<>(9, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> TRANSLATION =
            new EntityDataAccessor<>(10, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> SCALE =
            new EntityDataAccessor<>(11, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Quaternionf> LEFT_ROTATION =
            new EntityDataAccessor<>(12, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Quaternionf> RIGHT_ROTATION =
            new EntityDataAccessor<>(13, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Byte> BILLBOARD =
            new EntityDataAccessor<>(14, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> BRIGHTNESS =
            new EntityDataAccessor<>(15, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VIEW_RANGE =
            new EntityDataAccessor<>(16, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SHADOW_RADIUS =
            new EntityDataAccessor<>(17, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SHADOW_STRENGTH =
            new EntityDataAccessor<>(18, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WIDTH =
            new EntityDataAccessor<>(19, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            new EntityDataAccessor<>(20, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR =
            new EntityDataAccessor<>(21, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockState> BLOCK_STATE_ACCESSOR =
            new EntityDataAccessor<>(22, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> ITEM_STACK_ACCESSOR =
            new EntityDataAccessor<>(22, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Byte> ITEM_TRANSFORM =
            new EntityDataAccessor<>(23, EntityDataSerializers.BYTE);

    // ==================== Instance Fields ====================

    private final int entityId;
    private final UUID uuid;
    private final DisplayType displayType;
    private Location location;

    // Display common properties
    private int interpolationDelay = 0;
    private int interpolationDuration = 0;
    private Vector3f translation = new Vector3f(0, 0, 0);
    private Vector3f scale = new Vector3f(1, 1, 1);
    private Quaternionf leftRotation = new Quaternionf(0, 0, 0, 1);
    private Quaternionf rightRotation = new Quaternionf(0, 0, 0, 1);
    private byte billboard = 0;
    private int brightness = -1;
    private float viewRange = 1.0f;
    private float shadowRadius = 0.0f;
    private float shadowStrength = 1.0f;
    private float width = 0.0f;
    private float height = 0.0f;
    private int glowColorOverride = -1;
    private byte entityFlags = 0;

    // BlockDisplay specific
    private BlockState blockState;

    // ItemDisplay specific
    private net.minecraft.world.item.ItemStack nmsItemStack;
    private byte itemTransform = 0;

    FakeEntityImpl(int entityId, Location location, DisplayType displayType,
                   BlockData blockData, ItemStack itemStack) {
        this.entityId = entityId;
        this.uuid = UUID.randomUUID();
        this.displayType = displayType;
        this.location = location.clone();

        if (displayType == DisplayType.BLOCK_DISPLAY && blockData != null) {
            this.blockState = ((CraftBlockData) blockData).getState();
        }
        if (displayType == DisplayType.ITEM_DISPLAY && itemStack != null) {
            this.nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
        }
    }

    // ==================== Identity ====================

    @Override
    public int getEntityId() {
        return entityId;
    }

    @Override
    public DisplayType getDisplayType() {
        return displayType;
    }

    // ==================== Spawn / Destroy ====================

    @Override
    @SuppressWarnings("unchecked")
    public void spawn(Collection<? extends Player> players) {
        List<Packet<?>> packets = createSpawnPackets();
        ClientboundBundlePacket bundle = new ClientboundBundlePacket((Iterable) packets);
        for (Player player : players) {
            sendPacket(player, bundle);
        }
    }

    @Override
    public void spawn(Player player) {
        spawn(Collections.singleton(player));
    }

    @Override
    public void destroy(Collection<? extends Player> players) {
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityId);
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    @Override
    public void destroy(Player player) {
        destroy(Collections.singleton(player));
    }

    // ==================== Position & Transform ====================

    @Override
    public void teleport(Location location, Collection<? extends Player> players) {
        this.location = location.clone();
        ClientboundTeleportEntityPacket packet = createTeleportPacket();
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    @Override
    public void teleport(Location location, Player player) {
        teleport(location, Collections.singleton(player));
    }

    @Override
    public void setTranslation(Vector3f translation) {
        this.translation = new Vector3f(translation);
    }

    @Override
    public void setScale(Vector3f scale) {
        this.scale = new Vector3f(scale);
    }

    @Override
    public void setLeftRotation(Quaternionf rotation) {
        this.leftRotation = new Quaternionf(rotation);
    }

    @Override
    public void setRightRotation(Quaternionf rotation) {
        this.rightRotation = new Quaternionf(rotation);
    }

    // ==================== Display Properties ====================

    @Override
    public void setBillboard(byte mode) {
        this.billboard = mode;
    }

    @Override
    public void setBrightness(int blockLight, int skyLight) {
        this.brightness = blockLight << 4 | skyLight << 20;
    }

    @Override
    public void setViewRange(float range) {
        this.viewRange = range;
    }

    @Override
    public void setShadow(float radius, float strength) {
        this.shadowRadius = radius;
        this.shadowStrength = strength;
    }

    @Override
    public void setGlowColor(Color color) {
        if (color == null) {
            this.glowColorOverride = -1;
            this.entityFlags = (byte) (this.entityFlags & ~0x40); // Clear glow flag (bit 6)
        } else {
            this.glowColorOverride = color.asARGB();
            this.entityFlags = (byte) (this.entityFlags | 0x40); // Set glow flag (bit 6)
        }
    }

    @Override
    public void setInterpolationDuration(int ticks) {
        this.interpolationDuration = ticks;
    }

    @Override
    public void setInterpolationDelay(int ticks) {
        this.interpolationDelay = ticks;
    }

    // ==================== Type-Specific Properties ====================

    @Override
    public void setBlockData(BlockData blockData) {
        if (displayType != DisplayType.BLOCK_DISPLAY) return;
        this.blockState = ((CraftBlockData) blockData).getState();
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        if (displayType != DisplayType.ITEM_DISPLAY) return;
        this.nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
    }

    @Override
    public void setItemTransform(byte transform) {
        if (displayType != DisplayType.ITEM_DISPLAY) return;
        this.itemTransform = transform;
    }

    // ==================== Metadata Sync ====================

    @Override
    public void sendMetadata(Collection<? extends Player> players) {
        ClientboundSetEntityDataPacket packet = createMetadataPacket();
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    @Override
    public void sendMetadata(Player player) {
        sendMetadata(Collections.singleton(player));
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    // ==================== Internal Helpers ====================

    private void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    private List<Packet<?>> createSpawnPackets() {
        List<Packet<?>> packets = new ArrayList<>();

        EntityType<?> entityType = displayType == DisplayType.BLOCK_DISPLAY
                ? EntityType.BLOCK_DISPLAY
                : EntityType.ITEM_DISPLAY;

        ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(
                entityId, uuid,
                location.getX(), location.getY(), location.getZ(),
                0f, 0f,
                entityType,
                0,
                Vec3.ZERO,
                0.0
        );
        packets.add(spawnPacket);
        packets.add(createMetadataPacket());

        return packets;
    }

    private ClientboundSetEntityDataPacket createMetadataPacket() {
        List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();

        dataValues.add(SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, entityFlags));
        dataValues.add(SynchedEntityData.DataValue.create(INTERPOLATION_START, interpolationDelay));
        dataValues.add(SynchedEntityData.DataValue.create(INTERPOLATION_DURATION, interpolationDuration));
        dataValues.add(SynchedEntityData.DataValue.create(TRANSLATION, translation));
        dataValues.add(SynchedEntityData.DataValue.create(SCALE, scale));
        dataValues.add(SynchedEntityData.DataValue.create(LEFT_ROTATION, leftRotation));
        dataValues.add(SynchedEntityData.DataValue.create(RIGHT_ROTATION, rightRotation));
        dataValues.add(SynchedEntityData.DataValue.create(BILLBOARD, billboard));
        dataValues.add(SynchedEntityData.DataValue.create(BRIGHTNESS, brightness));
        dataValues.add(SynchedEntityData.DataValue.create(VIEW_RANGE, viewRange));
        dataValues.add(SynchedEntityData.DataValue.create(SHADOW_RADIUS, shadowRadius));
        dataValues.add(SynchedEntityData.DataValue.create(SHADOW_STRENGTH, shadowStrength));
        dataValues.add(SynchedEntityData.DataValue.create(WIDTH, width));
        dataValues.add(SynchedEntityData.DataValue.create(HEIGHT, height));
        dataValues.add(SynchedEntityData.DataValue.create(GLOW_COLOR, glowColorOverride));

        if (displayType == DisplayType.BLOCK_DISPLAY && blockState != null) {
            dataValues.add(SynchedEntityData.DataValue.create(BLOCK_STATE_ACCESSOR, blockState));
        } else if (displayType == DisplayType.ITEM_DISPLAY) {
            if (nmsItemStack != null) {
                dataValues.add(SynchedEntityData.DataValue.create(ITEM_STACK_ACCESSOR, nmsItemStack.copy()));
            }
            dataValues.add(SynchedEntityData.DataValue.create(ITEM_TRANSFORM, itemTransform));
        }

        return new ClientboundSetEntityDataPacket(entityId, dataValues);
    }

    private ClientboundTeleportEntityPacket createTeleportPacket() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(entityId);
        buf.writeDouble(location.getX());
        buf.writeDouble(location.getY());
        buf.writeDouble(location.getZ());
        buf.writeByte((byte) ((int) (location.getYaw() * 256.0F / 360.0F)));
        buf.writeByte((byte) ((int) (location.getPitch() * 256.0F / 360.0F)));
        buf.writeBoolean(false); // onGround
        ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(buf);
        buf.release();
        return packet;
    }
}
