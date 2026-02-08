package cn.lunadeer.dominion.utils.holograme;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static cn.lunadeer.dominion.utils.Misc.isPaper;

/**
 * Represents a single display element (BlockDisplay or ItemDisplay) within a {@link HoloItem}.
 * <p>
 * Each element has a relative offset from the parent HoloItem's anchor point,
 * and can be individually scaled, rotated, and styled using a fluent API.
 * <p>
 * <b>Thread Safety:</b> All methods that modify the underlying entity must be called
 * from the main server thread (or the entity's region thread on Folia).
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * HoloItem item = HoloManager.instance().create("myItem", playerLocation);
 * item.addBlockDisplay("red_axis", Material.RED_CONCRETE)
 *     .offset(0.5f, 0, 0)
 *     .scale(1.0f, 0.1f, 0.1f)
 *     .glow(true)
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
    private Display entity;
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
     * Create a new HoloElement backed by a BlockDisplay entity.
     *
     * @param name          unique name of this element within its parent HoloItem
     * @param parent        the parent HoloItem
     * @param world         the world to spawn in
     * @param spawnLocation initial spawn location
     * @param blockData     the block data to display
     */
    HoloElement(String name, HoloItem parent, World world, Location spawnLocation, BlockData blockData) {
        this.name = name;
        this.type = Type.BLOCK_DISPLAY;
        this.parent = parent;
        this.blockData = blockData;
        this.entity = world.spawn(spawnLocation, BlockDisplay.class, display -> {
            display.setBlock(blockData);
            display.setPersistent(false);
        });
    }

    /**
     * Create a new HoloElement backed by an ItemDisplay entity.
     *
     * @param name          unique name of this element within its parent HoloItem
     * @param parent        the parent HoloItem
     * @param world         the world to spawn in
     * @param spawnLocation initial spawn location
     * @param itemStack     the item to display
     */
    HoloElement(String name, HoloItem parent, World world, Location spawnLocation, ItemStack itemStack) {
        this.name = name;
        this.type = Type.ITEM_DISPLAY;
        this.parent = parent;
        this.itemStack = itemStack;
        this.entity = world.spawn(spawnLocation, ItemDisplay.class, display -> {
            display.setItemStack(itemStack);
            display.setPersistent(false);
        });
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
     * Get the underlying Display entity.
     *
     * @return the Display entity, or null if removed
     */
    public Display getEntity() {
        return entity;
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
        update();
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
        update();
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
        update();
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
        update();
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
        update();
        return this;
    }

    /**
     * Set whether this element is glowing.
     *
     * @param glowing true to make the element glow
     * @return this element for chaining
     */
    public HoloElement glow(boolean glowing) {
        if (entity != null && !entity.isDead()) {
            entity.setGlowing(glowing);
        }
        return this;
    }

    /**
     * Set the glow color override.
     *
     * @param color the glow color
     * @return this element for chaining
     */
    public HoloElement glowColor(Color color) {
        if (entity != null && !entity.isDead()) {
            entity.setGlowColorOverride(color);
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
        if (entity != null && !entity.isDead()) {
            entity.setBrightness(new Display.Brightness(blockLight, skyLight));
        }
        return this;
    }

    /**
     * Set the billboard mode of this element.
     * <p>
     * Billboard mode controls whether the display rotates to face the viewer:
     * <ul>
     *   <li>{@link Display.Billboard#FIXED} - no billboarding (default)</li>
     *   <li>{@link Display.Billboard#VERTICAL} - rotates around Y axis to face viewer</li>
     *   <li>{@link Display.Billboard#HORIZONTAL} - rotates around horizontal axis</li>
     *   <li>{@link Display.Billboard#CENTER} - always faces the viewer</li>
     * </ul>
     *
     * @param billboard the billboard mode
     * @return this element for chaining
     */
    public HoloElement billboard(Display.Billboard billboard) {
        if (entity != null && !entity.isDead()) {
            entity.setBillboard(billboard);
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
        if (entity != null && !entity.isDead()) {
            entity.setViewRange(range);
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
        if (entity != null && !entity.isDead()) {
            entity.setShadowRadius(radius);
            entity.setShadowStrength(strength);
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
        if (entity != null && !entity.isDead()) {
            entity.setInterpolationDuration(ticks);
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
        if (entity != null && !entity.isDead()) {
            entity.setInterpolationDelay(ticks);
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
        if (type == Type.BLOCK_DISPLAY && entity instanceof BlockDisplay bd && !entity.isDead()) {
            this.blockData = data;
            bd.setBlock(data);
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
        if (type == Type.ITEM_DISPLAY && entity instanceof ItemDisplay id && !entity.isDead()) {
            this.itemStack = stack;
            id.setItemStack(stack);
        }
        return this;
    }

    /**
     * Set the item display transform mode (only works for ITEM_DISPLAY elements).
     * Controls how the item is rendered (e.g., NONE, THIRDPERSON_LEFTHAND, HEAD, GROUND, FIXED, GUI).
     *
     * @param transform the item display transform
     * @return this element for chaining
     */
    public HoloElement itemTransform(ItemDisplay.ItemDisplayTransform transform) {
        if (type == Type.ITEM_DISPLAY && entity instanceof ItemDisplay id && !entity.isDead()) {
            id.setItemDisplayTransform(transform);
        }
        return this;
    }

    // ==================== Internal Methods ====================

    /**
     * Update the entity's position and transformation based on the parent HoloItem's state.
     * <p>
     * This recalculates the world position from the parent's anchor + rotated relative offset,
     * and applies the combined rotation and scale via the Display's Transformation.
     */
    void update() {
        if (entity == null || entity.isDead()) return;
        if (parent == null) return;

        Location anchor = parent.getAnchor();
        Quaternionf parentRotation = parent.getRotation();

        // Calculate world position: anchor + parentRotation.transform(relativeOffset)
        Vector3f rotatedOffset = parentRotation.transform(new Vector3f(relativeOffset));
        Location worldPos = anchor.clone().add(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z);
        worldPos.setYaw(0);
        worldPos.setPitch(0);

        // Teleport entity to calculated world position
        if (!isPaper()) {
            entity.teleport(worldPos);
        } else {
            entity.teleportAsync(worldPos);
        }

        // Calculate combined rotation: parent rotation * local rotation
        Quaternionf combinedRotation = new Quaternionf(parentRotation).mul(localRotation);

        // Apply transformation with translation offset, combined rotation, and local scale
        Transformation transformation = new Transformation(
                new Vector3f(translationOffset),
                combinedRotation,
                new Vector3f(localScale),
                new Quaternionf() // identity rightRotation
        );
        entity.setTransformation(transformation);
    }

    /**
     * Remove the display entity from the world.
     */
    void remove() {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        entity = null;
    }

    /**
     * Check if the underlying entity is valid (exists and is not dead).
     */
    public boolean isValid() {
        return entity != null && !entity.isDead();
    }

    @Override
    public String toString() {
        return String.format("HoloElement{name='%s', type=%s, offset=(%.2f, %.2f, %.2f), scale=(%.2f, %.2f, %.2f)}",
                name, type, relativeOffset.x, relativeOffset.y, relativeOffset.z,
                localScale.x, localScale.y, localScale.z);
    }
}
