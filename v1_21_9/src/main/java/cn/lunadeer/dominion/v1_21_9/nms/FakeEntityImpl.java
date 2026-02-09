package cn.lunadeer.dominion.v1_21_9.nms;

import cn.lunadeer.dominion.nms.FakeEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fake entity implementation for Minecraft 1.21.9+.
 * <p>
 * Same metadata indices as 1.21, but uses PositionMoveRotation-based teleport packet.
 */
@SuppressWarnings("unchecked")
public class FakeEntityImpl implements FakeEntity {

    // ==================== Metadata Accessors (resolved from Display class via reflection) ====================
    // We avoid direct EntityDataSerializers field references because they may differ across
    // server forks (e.g., DeerFolia adds CopperGolemState serializer, shifting field mappings).
    // Instead, we reflectively extract Display's own EntityDataAccessor fields and match by index.

    private static final EntityDataAccessor<Integer> INTERPOLATION_START;
    private static final EntityDataAccessor<Integer> INTERPOLATION_DURATION;
    private static final EntityDataAccessor<Integer> POS_ROT_INTERPOLATION_DURATION;
    private static final EntityDataAccessor<Vector3f> TRANSLATION;
    private static final EntityDataAccessor<Vector3f> SCALE;
    private static final EntityDataAccessor<Quaternionf> LEFT_ROTATION;
    private static final EntityDataAccessor<Quaternionf> RIGHT_ROTATION;
    private static final EntityDataAccessor<Byte> BILLBOARD;
    private static final EntityDataAccessor<Integer> BRIGHTNESS;
    private static final EntityDataAccessor<Float> VIEW_RANGE;
    private static final EntityDataAccessor<Float> SHADOW_RADIUS;
    private static final EntityDataAccessor<Float> SHADOW_STRENGTH;
    private static final EntityDataAccessor<Float> WIDTH;
    private static final EntityDataAccessor<Float> HEIGHT;
    private static final EntityDataAccessor<Integer> GLOW_COLOR;
    private static final EntityDataAccessor<BlockState> BLOCK_STATE_ACCESSOR;
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> ITEM_STACK_ACCESSOR;
    private static final EntityDataAccessor<Byte> ITEM_TRANSFORM;

    static {
        Map<Integer, EntityDataAccessor<?>> da = resolveAccessors(Display.class);
        Map<Integer, EntityDataAccessor<?>> bda = resolveAccessors(Display.BlockDisplay.class);
        Map<Integer, EntityDataAccessor<?>> ida = resolveAccessors(Display.ItemDisplay.class);

        INTERPOLATION_START = (EntityDataAccessor<Integer>) da.get(8);
        INTERPOLATION_DURATION = (EntityDataAccessor<Integer>) da.get(9);
        POS_ROT_INTERPOLATION_DURATION = (EntityDataAccessor<Integer>) da.get(10);
        TRANSLATION = (EntityDataAccessor<Vector3f>) da.get(11);
        SCALE = (EntityDataAccessor<Vector3f>) da.get(12);
        LEFT_ROTATION = (EntityDataAccessor<Quaternionf>) da.get(13);
        RIGHT_ROTATION = (EntityDataAccessor<Quaternionf>) da.get(14);
        BILLBOARD = (EntityDataAccessor<Byte>) da.get(15);
        BRIGHTNESS = (EntityDataAccessor<Integer>) da.get(16);
        VIEW_RANGE = (EntityDataAccessor<Float>) da.get(17);
        SHADOW_RADIUS = (EntityDataAccessor<Float>) da.get(18);
        SHADOW_STRENGTH = (EntityDataAccessor<Float>) da.get(19);
        WIDTH = (EntityDataAccessor<Float>) da.get(20);
        HEIGHT = (EntityDataAccessor<Float>) da.get(21);
        GLOW_COLOR = (EntityDataAccessor<Integer>) da.get(22);
        BLOCK_STATE_ACCESSOR = (EntityDataAccessor<BlockState>) bda.get(23);
        ITEM_STACK_ACCESSOR = (EntityDataAccessor<net.minecraft.world.item.ItemStack>) ida.get(23);
        ITEM_TRANSFORM = (EntityDataAccessor<Byte>) ida.get(24);
    }

    private static Map<Integer, EntityDataAccessor<?>> resolveAccessors(Class<?> clazz) {
        Map<Integer, EntityDataAccessor<?>> map = new java.util.HashMap<>();
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                    EntityDataAccessor.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    EntityDataAccessor<?> accessor = (EntityDataAccessor<?>) field.get(null);
                    map.put(accessor.id(), accessor);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access Display metadata field: " + field.getName(), e);
                }
            }
        }
        return map;
    }

    // ==================== Instance Fields ====================

    private final int entityId;
    private final UUID uuid;
    private final DisplayType displayType;
    private Location location;

    // Display common properties
    private int interpolationDelay = 0;
    private int interpolationDuration = 0;
    private int posRotInterpolationDuration = 0;
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
            this.blockState = toNmsBlockState(blockData);
        }
        if (displayType == DisplayType.ITEM_DISPLAY && itemStack != null) {
            this.nmsItemStack = toNmsItemStack(itemStack);
        }
    }

    @Override
    public int getEntityId() {
        return entityId;
    }

    @Override
    public DisplayType getDisplayType() {
        return displayType;
    }

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
        } else {
            this.glowColorOverride = color.asARGB();
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

    @Override
    public void setBlockData(BlockData blockData) {
        if (displayType != DisplayType.BLOCK_DISPLAY) return;
        this.blockState = toNmsBlockState(blockData);
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        if (displayType != DisplayType.ITEM_DISPLAY) return;
        this.nmsItemStack = toNmsItemStack(itemStack);
    }

    @Override
    public void setItemTransform(byte transform) {
        if (displayType != DisplayType.ITEM_DISPLAY) return;
        this.itemTransform = transform;
    }

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
        getServerPlayer(player).connection.send(packet);
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

        dataValues.add(SynchedEntityData.DataValue.create(INTERPOLATION_START, interpolationDelay));
        dataValues.add(SynchedEntityData.DataValue.create(INTERPOLATION_DURATION, interpolationDuration));
        dataValues.add(SynchedEntityData.DataValue.create(POS_ROT_INTERPOLATION_DURATION, posRotInterpolationDuration));
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

    /**
     * Create teleport packet using the 1.21.2+ PositionMoveRotation format.
     */
    private ClientboundTeleportEntityPacket createTeleportPacket() {
        PositionMoveRotation positionMoveRotation = new PositionMoveRotation(
                new Vec3(location.getX(), location.getY(), location.getZ()),
                Vec3.ZERO,
                location.getYaw(),
                location.getPitch()
        );
        return new ClientboundTeleportEntityPacket(
                entityId,
                positionMoveRotation,
                Set.of(),
                false
        );
    }

    // ==================== CraftBukkit Reflection Helpers ====================
    // Avoid compile-time CraftBukkit imports which get reobfuscated by paperweight
    // to versioned packages that may not match the runtime server.

    private static ServerPlayer getServerPlayer(Player player) {
        try {
            return (ServerPlayer) player.getClass().getMethod("getHandle").invoke(player);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get ServerPlayer handle", e);
        }
    }

    private static BlockState toNmsBlockState(BlockData blockData) {
        try {
            return (BlockState) blockData.getClass().getMethod("getState").invoke(blockData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert BlockData to NMS BlockState", e);
        }
    }

    private static net.minecraft.world.item.ItemStack toNmsItemStack(ItemStack itemStack) {
        try {
            String cbPkg = Bukkit.getServer().getClass().getPackage().getName();
            Class<?> cls = Class.forName(cbPkg + ".inventory.CraftItemStack");
            return (net.minecraft.world.item.ItemStack) cls.getMethod("asNMSCopy", ItemStack.class).invoke(null, itemStack);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert ItemStack to NMS", e);
        }
    }
}
