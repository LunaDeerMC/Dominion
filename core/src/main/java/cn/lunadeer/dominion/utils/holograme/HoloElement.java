package cn.lunadeer.dominion.utils.holograme;

import cn.lunadeer.dominion.nms.FakeEntity;
import cn.lunadeer.dominion.nms.NMSManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;

/**
 * Represents a single display element (BlockDisplay or ItemDisplay) within a {@link HoloItem}.
 * <p>
 * Each element has a relative offset from the parent HoloItem's anchor point,
 * and can be individually scaled, rotated, and styled using a fluent API.
 * <p>
 * This implementation uses NMS packets to create client-side only entities.
 * No real entities are spawned on the server. Each element is backed by a {@link FakeEntity}.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * HoloItem item = HoloManager.instance().create("myItem", playerLocation);
 * item.addBlockDisplay("red_axis", Material.RED_CONCRETE)
 *     .offset(0.5f, 0, 0)
 *     .scale(1.0f, 0.1f, 0.1f)
 *     .glowColor(Color.RED)
 *     .brightness(15, 15);
 * }</pre>
 */
public class HoloElement {

    /**
     * The type of display entity backing this element.
     */
    public enum Type {
        BLOCK_DISPLAY,
        ITEM_DISPLAY
    }

    private final String name;
    private final Type type;
    private FakeEntity fakeEntity;
    private HoloItem parent;

    // Relative position within the parent HoloItem
    private final Vector3f relativeOffset = new Vector3f(0, 0, 0);
    // Local scale
    private final Vector3f localScale = new Vector3f(1, 1, 1);
    // Local rotation (relative to parent)
    private Quaternionf localRotation = new Quaternionf();
    // Translation offset for the Transformation (useful for centering pivot)
    private final Vector3f translationOffset = new Vector3f(0, 0, 0);

    // Creation data (for reference / debugging)
    private BlockData blockData;
    private ItemStack itemStack;

    /**
     * Create a new HoloElement backed by a client-side BlockDisplay entity.
     *
     * @param name          unique name of this element within its parent HoloItem
     * @param parent        the parent HoloItem
     * @param spawnLocation initial spawn location
     * @param blockData     the block data to display
     */
    HoloElement(String name, HoloItem parent, Location spawnLocation, BlockData blockData) {
        this.name = name;
        this.type = Type.BLOCK_DISPLAY;
        this.parent = parent;
        this.blockData = blockData;
        this.fakeEntity = NMSManager.instance().getFakeEntityFactory()
                .createBlockDisplay(spawnLocation, blockData);
    }

    /**
     * Create a new HoloElement backed by a client-side ItemDisplay entity.
     *
     * @param name          unique name of this element within its parent HoloItem
     * @param parent        the parent HoloItem
     * @param spawnLocation initial spawn location
     * @param itemStack     the item to display
     */
    HoloElement(String name, HoloItem parent, Location spawnLocation, ItemStack itemStack) {
        this.name = name;
        this.type = Type.ITEM_DISPLAY;
        this.parent = parent;
        this.itemStack = itemStack;
        this.fakeEntity = NMSManager.instance().getFakeEntityFactory()
                .createItemDisplay(spawnLocation, itemStack);
    }

    // ==================== Getters ====================

    /**
     * Get the element name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the element type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the underlying FakeEntity.
     *
     * @return the FakeEntity, or null if removed
     */
    public FakeEntity getFakeEntity() {
        return fakeEntity;
    }

    /**
     * Get a copy of the relative offset from the parent HoloItem's anchor.
     */
    public Vector3f getRelativeOffset() {
        return new Vector3f(relativeOffset);
    }

    /**
     * Get a copy of the local scale.
     */
    public Vector3f getLocalScale() {
        return new Vector3f(localScale);
    }

    /**
     * Get a copy of the local rotation quaternion.
     */
    public Quaternionf getLocalRotation() {
        return new Quaternionf(localRotation);
    }

    /**
     * Get a copy of the translation offset.
     */
    public Vector3f getTranslationOffset() {
        return new Vector3f(translationOffset);
    }

    /**
     * Get the block data (only meaningful for BLOCK_DISPLAY elements).
     */
    public BlockData getBlockData() {
        return blockData;
    }

    /**
     * Get the item stack (only meaningful for ITEM_DISPLAY elements).
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    // ==================== Fluent Configuration Methods ====================

    /**
     * Set the relative offset from the parent HoloItem's anchor point.
     * <p>
     * When the parent HoloItem rotates, this offset is rotated accordingly,
     * so (1, 0, 0) always means "1 block in the HoloItem's local X direction".
     *
     * @param x offset along X axis
     * @param y offset along Y axis
     * @param z offset along Z axis
     * @return this element for chaining
     */
    public HoloElement offset(float x, float y, float z) {
        this.relativeOffset.set(x, y, z);
        updateTransform();
        return this;
    }

    /**
     * Set the local scale of this element.
     *
     * @param x scale along X axis
     * @param y scale along Y axis
     * @param z scale along Z axis
     * @return this element for chaining
     */
    public HoloElement scale(float x, float y, float z) {
        this.localScale.set(x, y, z);
        updateTransform();
        return this;
    }

    /**
     * Set uniform scale for this element.
     *
     * @param uniform the uniform scale factor
     * @return this element for chaining
     */
    public HoloElement scale(float uniform) {
        return scale(uniform, uniform, uniform);
    }

    /**
     * Set the local rotation using Euler angles (in degrees).
     * The rotation is applied in YXZ order (yaw, then pitch, then roll).
     *
     * @param yaw   rotation around Y axis (degrees)
     * @param pitch rotation around X axis (degrees)
     * @param roll  rotation around Z axis (degrees)
     * @return this element for chaining
     */
    public HoloElement rotate(float yaw, float pitch, float roll) {
        this.localRotation = new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
        updateTransform();
        return this;
    }

    /**
     * Set the local rotation using a Quaternionf directly.
     *
     * @param rotation the rotation quaternion
     * @return this element for chaining
     */
    public HoloElement rotate(Quaternionf rotation) {
        this.localRotation = new Quaternionf(rotation);
        updateTransform();
        return this;
    }

    /**
     * Set the translation offset within the Display's Transformation.
     * <p>
     * This is useful for adjusting the pivot/origin of the display.
     * For example, {@code translation(-0.5f, -0.5f, -0.5f)} centers a 1x1x1 block
     * so that it rotates around its center instead of the default NW bottom corner.
     *
     * @param x translation X
     * @param y translation Y
     * @param z translation Z
     * @return this element for chaining
     */
    public HoloElement translation(float x, float y, float z) {
        this.translationOffset.set(x, y, z);
        updateTransform();
        return this;
    }

    /**
     * Set whether this element is glowing.
     * <p>
     * Note: For packet-based fake entities, glow is controlled via the glow color.
     * Setting glow(true) without a glowColor may have no visible effect.
     *
     * @param glowing true to make the element glow (currently a no-op for NMS entities)
     * @return this element for chaining
     */
    public HoloElement glow(boolean glowing) {
        // Glow is implicitly enabled when glowColorOverride != -1
        // For packet-based entities, we don't need a separate glow flag
        return this;
    }

    /**
     * Set the glow color override.
     *
     * @param color the glow color
     * @return this element for chaining
     */
    public HoloElement glowColor(Color color) {
        if (fakeEntity != null) {
            fakeEntity.setGlowColor(color);
            syncToViewers();
        }
        return this;
    }

    /**
     * Set the brightness of this element.
     *
     * @param blockLight block light level (0-15)
     * @param skyLight   sky light level (0-15)
     * @return this element for chaining
     */
    public HoloElement brightness(int blockLight, int skyLight) {
        if (fakeEntity != null) {
            fakeEntity.setBrightness(blockLight, skyLight);
            syncToViewers();
        }
        return this;
    }

    /**
     * Set the billboard mode of this element.
     * <p>
     * Billboard mode controls whether the display rotates to face the viewer:
     * <ul>
     *   <li>0 = FIXED - no billboarding (default)</li>
     *   <li>1 = VERTICAL - rotates around Y axis to face viewer</li>
     *   <li>2 = HORIZONTAL - rotates around horizontal axis</li>
     *   <li>3 = CENTER - always faces the viewer</li>
     * </ul>
     *
     * @param billboard the billboard mode byte
     * @return this element for chaining
     */
    public HoloElement billboard(byte billboard) {
        if (fakeEntity != null) {
            fakeEntity.setBillboard(billboard);
            syncToViewers();
        }
        return this;
    }

    /**
     * Set the view range multiplier. A value of 0 effectively hides the display.
     *
     * @param range the view range multiplier (default 1.0)
     * @return this element for chaining
     */
    public HoloElement viewRange(float range) {
        if (fakeEntity != null) {
            fakeEntity.setViewRange(range);
            syncToViewers();
        }
        return this;
    }

    /**
     * Set the shadow radius and strength.
     *
     * @param radius   shadow radius in blocks
     * @param strength shadow strength (0.0 to 1.0)
     * @return this element for chaining
     */
    public HoloElement shadow(float radius, float strength) {
        if (fakeEntity != null) {
            fakeEntity.setShadow(radius, strength);
            syncToViewers();
        }
        return this;
    }

    /**
     * Set the interpolation duration for smooth transitions between transformations.
     *
     * @param ticks duration in ticks (20 ticks = 1 second)
     * @return this element for chaining
     */
    public HoloElement interpolationDuration(int ticks) {
        if (fakeEntity != null) {
            fakeEntity.setInterpolationDuration(ticks);
            syncToViewers();
        }
        return this;
    }

    /**
     * Set the interpolation delay.
     *
     * @param ticks delay in ticks before interpolation starts
     * @return this element for chaining
     */
    public HoloElement interpolationDelay(int ticks) {
        if (fakeEntity != null) {
            fakeEntity.setInterpolationDelay(ticks);
            syncToViewers();
        }
        return this;
    }

    /**
     * Change the block data (only works for BLOCK_DISPLAY elements).
     *
     * @param data the new block data
     * @return this element for chaining
     */
    public HoloElement setBlockData(BlockData data) {
        if (type == Type.BLOCK_DISPLAY && fakeEntity != null) {
            this.blockData = data;
            fakeEntity.setBlockData(data);
            syncToViewers();
        }
        return this;
    }

    /**
     * Change the item stack (only works for ITEM_DISPLAY elements).
     *
     * @param stack the new item stack
     * @return this element for chaining
     */
    public HoloElement setItemStack(ItemStack stack) {
        if (type == Type.ITEM_DISPLAY && fakeEntity != null) {
            this.itemStack = stack;
            fakeEntity.setItemStack(stack);
            syncToViewers();
        }
        return this;
    }

    /**
     * Set the item display transform mode (only works for ITEM_DISPLAY elements).
     * Controls how the item is rendered (e.g., NONE=0, THIRDPERSON_LEFTHAND=1,
     * THIRDPERSON_RIGHTHAND=2, FIRSTPERSON_LEFTHAND=3, FIRSTPERSON_RIGHTHAND=4,
     * HEAD=5, GUI=6, GROUND=7, FIXED=8).
     *
     * @param transform the item display transform byte
     * @return this element for chaining
     */
    public HoloElement itemTransform(byte transform) {
        if (type == Type.ITEM_DISPLAY && fakeEntity != null) {
            fakeEntity.setItemTransform(transform);
            syncToViewers();
        }
        return this;
    }

    // ==================== Internal Methods ====================

    /**
     * Send the current metadata to all online viewers.
     * This is a helper method called by property setters to sync changes immediately.
     */
    private void syncToViewers() {
        if (fakeEntity == null || parent == null) return;
        java.util.Set<Player> viewers = parent.getViewers();
        if (!viewers.isEmpty()) {
            java.util.List<Player> onlineViewers = new java.util.ArrayList<>();
            for (Player viewer : viewers) {
                if (viewer.isOnline()) {
                    onlineViewers.add(viewer);
                }
            }
            if (!onlineViewers.isEmpty()) {
                fakeEntity.sendMetadata(onlineViewers);
            }
        }
    }

    /**
     * Update the FakeEntity's transformation based on the parent HoloItem's state.
     * <p>
     * This recalculates the transformation from the parent's rotation and this element's
     * local offset, scale, rotation, and translation. The changes are automatically
     * sent to all current viewers.
     */
    void updateTransform() {
        if (fakeEntity == null) return;
        if (parent == null) return;

        Quaternionf parentRotation = parent.getRotation();

        // Calculate combined rotation: parent rotation * local rotation
        Quaternionf combinedRotation = new Quaternionf(parentRotation).mul(localRotation);

        // Transform the translation offset by parent rotation to keep it consistent with world space
        Vector3f rotatedTranslation = parentRotation.transform(new Vector3f(translationOffset));

        // Apply transformation properties to the fake entity
        fakeEntity.setTranslation(rotatedTranslation);
        fakeEntity.setLeftRotation(combinedRotation);
        fakeEntity.setScale(new Vector3f(localScale));
        fakeEntity.setRightRotation(new Quaternionf()); // identity

        // Send the updated transformation to all current viewers
        java.util.Set<Player> viewers = parent.getViewers();
        if (!viewers.isEmpty()) {
            java.util.List<Player> onlineViewers = new java.util.ArrayList<>();
            for (Player viewer : viewers) {
                if (viewer.isOnline()) {
                    onlineViewers.add(viewer);
                }
            }
            if (!onlineViewers.isEmpty()) {
                sendTo(onlineViewers);
            }
        }
    }

    /**
     * Calculate the world position for this element based on the parent's anchor and rotation.
     *
     * @return the world position for this element
     */
    Location calculateWorldPosition() {
        if (parent == null) return location();
        Location anchor = parent.getAnchor();
        Quaternionf parentRotation = parent.getRotation();
        Vector3f rotatedOffset = parentRotation.transform(new Vector3f(relativeOffset));
        Location worldPos = anchor.clone().add(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z);
        worldPos.setYaw(0);
        worldPos.setPitch(0);
        return worldPos;
    }

    /**
     * Get the current location from the fake entity.
     */
    private Location location() {
        return fakeEntity != null ? fakeEntity.getLocation() : null;
    }

    /**
     * Spawn this element on specific players' clients and send current state.
     *
     * @param players the players who should see this element
     */
    void spawnFor(Collection<? extends Player> players) {
        if (fakeEntity == null) return;
        updateTransform();
        // Teleport to correct position first
        Location pos = calculateWorldPosition();
        fakeEntity.teleport(pos, players);
        // Spawn (sends spawn + metadata)
        fakeEntity.spawn(players);
    }

    /**
     * Spawn this element on a single player's client.
     *
     * @param player the player who should see this element
     */
    void spawnFor(Player player) {
        spawnFor(java.util.Collections.singleton(player));
    }

    /**
     * Remove this element from specific players' clients.
     *
     * @param players the players who should no longer see this element
     */
    void destroyFor(Collection<? extends Player> players) {
        if (fakeEntity == null) return;
        fakeEntity.destroy(players);
    }

    /**
     * Remove this element from a single player's client.
     *
     * @param player the player who should no longer see this element
     */
    void destroyFor(Player player) {
        destroyFor(java.util.Collections.singleton(player));
    }

    /**
     * Send updated position and metadata to specific players.
     *
     * @param players the players to update
     */
    void sendTo(Collection<? extends Player> players) {
        if (fakeEntity == null) return;
        Location pos = calculateWorldPosition();
        fakeEntity.teleport(pos, players);
        fakeEntity.sendMetadata(players);
    }

    /**
     * Send updated position and metadata to a single player.
     *
     * @param player the player to update
     */
    void sendTo(Player player) {
        sendTo(java.util.Collections.singleton(player));
    }

    /**
     * Clean up this element's resources. Called when the element is removed from its parent.
     * Note: This does NOT send destroy packets. Call {@link #destroyFor} first.
     */
    void remove() {
        fakeEntity = null;
    }

    /**
     * Check if the underlying fake entity is valid.
     */
    public boolean isValid() {
        return fakeEntity != null;
    }

    @Override
    public String toString() {
        return String.format("HoloElement{name='%s', type=%s, offset=(%.2f, %.2f, %.2f), scale=(%.2f, %.2f, %.2f)}",
                name, type, relativeOffset.x, relativeOffset.y, relativeOffset.z,
                localScale.x, localScale.y, localScale.z);
    }
}
