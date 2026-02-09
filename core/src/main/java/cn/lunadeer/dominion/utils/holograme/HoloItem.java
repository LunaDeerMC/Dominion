package cn.lunadeer.dominion.utils.holograme;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;

import java.util.*;

/**
 * Represents a holographic item composed of multiple {@link HoloElement}s (client-side Display entities).
 * <p>
 * A HoloItem has an <b>anchor position</b> and a <b>rotation</b> in the world.
 * Each child element is positioned relative to the anchor, and when the HoloItem
 * moves or rotates, all child elements update accordingly.
 * <p>
 * HoloItems use NMS packets to create purely client-side entities — no real entities
 * are spawned on the server. This means:
 * <ul>
 *   <li>Zero server-side entity overhead</li>
 *   <li>Per-player visibility control via {@link #show(Player)} / {@link #hide(Player)}</li>
 *   <li>No interference with other plugins or world saves</li>
 * </ul>
 * <p>
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
 * // Show to a specific player
 * axes.show(player);
 *
 * // Hide from that player
 * axes.hide(player);
 *
 * // Show to multiple players
 * axes.show(playerList);
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
    private final Set<Player> viewers = new HashSet<>();

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
        HoloElement element = new HoloElement(elementName, this, anchor.clone(), blockData);
        elements.put(elementName, element);
        element.updateTransform();
        // If there are active viewers, spawn this new element for them
        if (!viewers.isEmpty()) {
            element.spawnFor(getOnlineViewers());
        }
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
        HoloElement element = new HoloElement(elementName, this, anchor.clone(), itemStack);
        elements.put(elementName, element);
        element.updateTransform();
        // If there are active viewers, spawn this new element for them
        if (!viewers.isEmpty()) {
            element.spawnFor(getOnlineViewers());
        }
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
            // Destroy for all viewers first
            if (!viewers.isEmpty()) {
                element.destroyFor(getOnlineViewers());
            }
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

    // ==================== Per-Player Visibility ====================

    /**
     * Show the HoloItem to a specific player.
     * Spawns all elements on the player's client via NMS packets.
     *
     * @param player the player who should see this HoloItem
     * @return this HoloItem for chaining
     */
    public HoloItem show(Player player) {
        if (player == null || !player.isOnline()) return this;
        if (viewers.contains(player)) return this;
        viewers.add(player);
        for (HoloElement element : elements.values()) {
            element.spawnFor(player);
        }
        return this;
    }

    /**
     * Show the HoloItem to multiple players.
     *
     * @param players the players who should see this HoloItem
     * @return this HoloItem for chaining
     */
    public HoloItem show(Collection<? extends Player> players) {
        for (Player player : players) {
            show(player);
        }
        return this;
    }

    /**
     * Hide the HoloItem from a specific player.
     * Destroys all elements on the player's client via NMS packets.
     *
     * @param player the player who should no longer see this HoloItem
     * @return this HoloItem for chaining
     */
    public HoloItem hide(Player player) {
        if (player == null) return this;
        if (!viewers.remove(player)) return this;
        if (player.isOnline()) {
            for (HoloElement element : elements.values()) {
                element.destroyFor(player);
            }
        }
        return this;
    }

    /**
     * Hide the HoloItem from multiple players.
     *
     * @param players the players who should no longer see this HoloItem
     * @return this HoloItem for chaining
     */
    public HoloItem hide(Collection<? extends Player> players) {
        for (Player player : players) {
            hide(player);
        }
        return this;
    }

    /**
     * Hide the HoloItem from all current viewers.
     *
     * @return this HoloItem for chaining
     */
    public HoloItem hideAll() {
        List<Player> currentViewers = new ArrayList<>(viewers);
        for (Player player : currentViewers) {
            hide(player);
        }
        return this;
    }

    /**
     * Check if a specific player is currently viewing this HoloItem.
     *
     * @param player the player to check
     * @return true if the player can see this HoloItem
     */
    public boolean isViewedBy(Player player) {
        return viewers.contains(player);
    }

    /**
     * Get an unmodifiable set of all current viewers.
     *
     * @return the set of players currently viewing this HoloItem
     */
    public Set<Player> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    /**
     * Get the list of online viewers (filters out disconnected players).
     *
     * @return list of currently online viewers
     */
    private List<Player> getOnlineViewers() {
        List<Player> online = new ArrayList<>();
        Iterator<Player> it = viewers.iterator();
        while (it.hasNext()) {
            Player p = it.next();
            if (p.isOnline()) {
                online.add(p);
            } else {
                it.remove(); // Clean up disconnected players
            }
        }
        return online;
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
     * Update all elements' positions and transformations and send to all viewers.
     * <p>
     * Called automatically when the HoloItem's position or rotation changes.
     * Can also be called manually to force a refresh.
     */
    public void updateAll() {
        List<Player> online = getOnlineViewers();
        for (HoloElement element : elements.values()) {
            element.updateTransform();
            if (!online.isEmpty()) {
                element.sendTo(online);
            }
        }
    }

    /**
     * Remove all elements and clean up this HoloItem.
     * Sends destroy packets to all viewers and unregisters from {@link HoloManager}.
     */
    public void remove() {
        List<Player> online = getOnlineViewers();
        for (HoloElement element : elements.values()) {
            if (!online.isEmpty()) {
                element.destroyFor(online);
            }
            element.remove();
        }
        elements.clear();
        viewers.clear();
        HoloManager.instance().untrack(name);
    }

    @Override
    public String toString() {
        return String.format("HoloItem{name='%s', anchor=%s, elements=%d, viewers=%d}",
                name,
                anchor.getWorld() != null
                        ? String.format("(%s, %.1f, %.1f, %.1f)", anchor.getWorld().getName(),
                        anchor.getX(), anchor.getY(), anchor.getZ())
                        : "null",
                elements.size(), viewers.size());
    }
}
