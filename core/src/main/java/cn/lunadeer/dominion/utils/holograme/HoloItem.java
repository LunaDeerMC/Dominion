package cn.lunadeer.dominion.utils.holograme;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;

import java.util.*;

/**
 * Represents a holographic item composed of multiple {@link HoloElement}s (BlockDisplay / ItemDisplay entities).
 * <p>
 * A HoloItem has an <b>anchor position</b> and a <b>rotation</b> in the world.
 * Each child element is positioned relative to the anchor, and when the HoloItem
 * moves or rotates, all child elements update accordingly.
 * <p>
 * HoloItems are non-persistent — they are removed when the server restarts.
 * Use {@link HoloManager} to create and manage HoloItems.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create a coordinate axes indicator
 * HoloItem axes = HoloManager.instance().create("axes", playerLocation);
 *
 * // X axis - red
 * axes.addBlockDisplay("x_axis", Material.RED_CONCRETE)
 *     .offset(0.5f, 0, 0)
 *     .scale(1.0f, 0.1f, 0.1f)
 *     .translation(-0.5f, -0.05f, -0.05f);
 *
 * // Y axis - green
 * axes.addBlockDisplay("y_axis", Material.GREEN_CONCRETE)
 *     .offset(0, 0.5f, 0)
 *     .scale(0.1f, 1.0f, 0.1f)
 *     .translation(-0.05f, -0.5f, -0.05f);
 *
 * // Z axis - blue
 * axes.addBlockDisplay("z_axis", Material.BLUE_CONCRETE)
 *     .offset(0, 0, 0.5f)
 *     .scale(0.1f, 0.1f, 1.0f)
 *     .translation(-0.05f, -0.05f, -0.5f);
 *
 * // Rotate the entire thing 45 degrees
 * axes.setRotation(45, 0);
 *
 * // Move it somewhere else
 * axes.setPosition(newLocation);
 *
 * // Hide / show
 * axes.hide();
 * axes.show();
 *
 * // Clean up
 * axes.remove();
 * }</pre>
 */
public class HoloItem {

    private final String name;
    private Location anchor;
    private Quaternionf rotation = new Quaternionf(); // identity
    private final Map<String, HoloElement> elements = new LinkedHashMap<>();
    private boolean visible = true;
    private float savedViewRange = 1.0f;

    /**
     * Create a new HoloItem at the specified anchor location.
     * <p>
     * Use {@link HoloManager#create(String, Location)} instead of calling this directly.
     *
     * @param name   unique name for this HoloItem
     * @param anchor the world position to anchor this HoloItem
     */
    HoloItem(String name, Location anchor) {
        this.name = name;
        this.anchor = anchor.clone();
    }

    // ==================== Element Management ====================

    /**
     * Add a BlockDisplay element using a Material.
     *
     * @param elementName unique name for this element within this HoloItem
     * @param material    the block material to display
     * @return the created {@link HoloElement} for further configuration (fluent API)
     * @throws IllegalArgumentException if an element with this name already exists
     * @throws IllegalStateException    if the anchor location has no world
     */
    public HoloElement addBlockDisplay(String elementName, Material material) {
        return addBlockDisplay(elementName, material.createBlockData());
    }

    /**
     * Add a BlockDisplay element using specific BlockData.
     *
     * @param elementName unique name for this element
     * @param blockData   the block data to display
     * @return the created {@link HoloElement} for further configuration
     */
    public HoloElement addBlockDisplay(String elementName, BlockData blockData) {
        if (elements.containsKey(elementName)) {
            throw new IllegalArgumentException("Element '" + elementName + "' already exists in HoloItem '" + name + "'");
        }
        World world = anchor.getWorld();
        if (world == null) {
            throw new IllegalStateException("Anchor location has no world");
        }
        HoloElement element = new HoloElement(elementName, this, world, anchor.clone(), blockData);
        elements.put(elementName, element);
        if (!visible) {
            element.viewRange(0);
        }
        element.update();
        return element;
    }

    /**
     * Add an ItemDisplay element using a Material.
     *
     * @param elementName unique name for this element
     * @param material    the item material to display
     * @return the created {@link HoloElement} for further configuration
     */
    public HoloElement addItemDisplay(String elementName, Material material) {
        return addItemDisplay(elementName, new ItemStack(material));
    }

    /**
     * Add an ItemDisplay element using an ItemStack.
     *
     * @param elementName unique name for this element
     * @param itemStack   the item stack to display
     * @return the created {@link HoloElement} for further configuration
     */
    public HoloElement addItemDisplay(String elementName, ItemStack itemStack) {
        if (elements.containsKey(elementName)) {
            throw new IllegalArgumentException("Element '" + elementName + "' already exists in HoloItem '" + name + "'");
        }
        World world = anchor.getWorld();
        if (world == null) {
            throw new IllegalStateException("Anchor location has no world");
        }
        HoloElement element = new HoloElement(elementName, this, world, anchor.clone(), itemStack);
        elements.put(elementName, element);
        if (!visible) {
            element.viewRange(0);
        }
        element.update();
        return element;
    }

    /**
     * Remove an element by name.
     *
     * @param elementName the name of the element to remove
     * @return this HoloItem for chaining
     */
    public HoloItem removeElement(String elementName) {
        HoloElement element = elements.remove(elementName);
        if (element != null) {
            element.remove();
        }
        return this;
    }

    /**
     * Get an element by name.
     *
     * @param elementName the element name
     * @return the element, or null if not found
     */
    public HoloElement getElement(String elementName) {
        return elements.get(elementName);
    }

    /**
     * Check if an element with the given name exists.
     */
    public boolean hasElement(String elementName) {
        return elements.containsKey(elementName);
    }

    /**
     * Get an unmodifiable set of all element names.
     */
    public Set<String> getElementNames() {
        return Collections.unmodifiableSet(elements.keySet());
    }

    /**
     * Get an unmodifiable collection of all elements.
     */
    public Collection<HoloElement> getElements() {
        return Collections.unmodifiableCollection(elements.values());
    }

    /**
     * Get the number of elements in this HoloItem.
     */
    public int getElementCount() {
        return elements.size();
    }

    // ==================== Position & Rotation ====================

    /**
     * Set the anchor position (world position) of this HoloItem.
     * All elements will be updated to reflect the new position.
     *
     * @param location the new anchor location
     * @return this HoloItem for chaining
     */
    public HoloItem setPosition(Location location) {
        this.anchor = location.clone();
        updateAll();
        return this;
    }

    /**
     * Move the HoloItem by a delta offset in world coordinates.
     *
     * @param dx offset along world X
     * @param dy offset along world Y
     * @param dz offset along world Z
     * @return this HoloItem for chaining
     */
    public HoloItem move(double dx, double dy, double dz) {
        this.anchor.add(dx, dy, dz);
        updateAll();
        return this;
    }

    /**
     * Set the rotation of this HoloItem using yaw and pitch (in degrees).
     *
     * @param yaw   rotation around Y axis
     * @param pitch rotation around X axis
     * @return this HoloItem for chaining
     */
    public HoloItem setRotation(float yaw, float pitch) {
        return setRotation(yaw, pitch, 0);
    }

    /**
     * Set the rotation of this HoloItem using Euler angles (in degrees).
     *
     * @param yaw   rotation around Y axis
     * @param pitch rotation around X axis
     * @param roll  rotation around Z axis
     * @return this HoloItem for chaining
     */
    public HoloItem setRotation(float yaw, float pitch, float roll) {
        this.rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
        updateAll();
        return this;
    }

    /**
     * Set the rotation of this HoloItem using a quaternion directly.
     *
     * @param rotation the rotation quaternion
     * @return this HoloItem for chaining
     */
    public HoloItem setRotation(Quaternionf rotation) {
        this.rotation = new Quaternionf(rotation);
        updateAll();
        return this;
    }

    // ==================== Visibility ====================

    /**
     * Show the HoloItem (make all elements visible).
     *
     * @return this HoloItem for chaining
     */
    public HoloItem show() {
        if (visible) return this;
        visible = true;
        for (HoloElement element : elements.values()) {
            element.viewRange(savedViewRange);
        }
        return this;
    }

    /**
     * Hide the HoloItem (make all elements invisible by setting view range to 0).
     *
     * @return this HoloItem for chaining
     */
    public HoloItem hide() {
        if (!visible) return this;
        visible = false;
        for (HoloElement element : elements.values()) {
            element.viewRange(0);
        }
        return this;
    }

    /**
     * Check if the HoloItem is currently visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set the view range for all elements.
     * This value is saved and re-applied when the HoloItem is shown.
     *
     * @param range the view range multiplier (default 1.0)
     * @return this HoloItem for chaining
     */
    public HoloItem setViewRange(float range) {
        this.savedViewRange = range;
        if (visible) {
            for (HoloElement element : elements.values()) {
                element.viewRange(range);
            }
        }
        return this;
    }

    // ==================== Getters ====================

    /**
     * Get the HoloItem name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get a clone of the anchor location.
     */
    public Location getAnchor() {
        return anchor.clone();
    }

    /**
     * Get a copy of the rotation quaternion.
     */
    public Quaternionf getRotation() {
        return new Quaternionf(rotation);
    }

    // ==================== Lifecycle ====================

    /**
     * Update all elements' positions and transformations.
     * <p>
     * Called automatically when the HoloItem's position or rotation changes.
     * Can also be called manually to force a refresh.
     */
    public void updateAll() {
        for (HoloElement element : elements.values()) {
            element.update();
        }
    }

    /**
     * Remove all elements and clean up this HoloItem.
     * Also unregisters this HoloItem from the {@link HoloManager}.
     */
    public void remove() {
        for (HoloElement element : elements.values()) {
            element.remove();
        }
        elements.clear();
        HoloManager.instance().untrack(name);
    }

    @Override
    public String toString() {
        return String.format("HoloItem{name='%s', anchor=%s, elements=%d, visible=%s}",
                name,
                anchor.getWorld() != null
                        ? String.format("(%s, %.1f, %.1f, %.1f)", anchor.getWorld().getName(),
                        anchor.getX(), anchor.getY(), anchor.getZ())
                        : "null",
                elements.size(), visible);
    }
}
