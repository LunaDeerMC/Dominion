package cn.lunadeer.dominion.utils.holograme;

import org.bukkit.Location;

import java.util.*;

/**
 * Singleton manager for all {@link HoloItem}s.
 * <p>
 * Provides creation, lookup, and cleanup utilities for holographic display items.
 * All HoloItems created through this manager are tracked and can be cleaned up
 * together when the plugin is disabled via {@link #removeAll()}.
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Create a new HoloItem
 * HoloItem item = HoloManager.instance().create("myItem", location);
 *
 * // Retrieve later
 * HoloItem item = HoloManager.instance().get("myItem");
 *
 * // Remove
 * HoloManager.instance().remove("myItem");
 *
 * // Cleanup everything (call on plugin disable)
 * HoloManager.instance().removeAll();
 * }</pre>
 */
public class HoloManager {

    private static final HoloManager INSTANCE = new HoloManager();
    private final Map<String, HoloItem> items = new LinkedHashMap<>();

    private HoloManager() {
    }

    /**
     * Get the singleton HoloManager instance.
     */
    public static HoloManager instance() {
        return INSTANCE;
    }

    /**
     * Create a new HoloItem at the specified anchor location.
     *
     * @param name   unique name for the HoloItem
     * @param anchor the world position to anchor the HoloItem
     * @return the newly created HoloItem
     * @throws IllegalArgumentException if a HoloItem with this name already exists
     */
    public HoloItem create(String name, Location anchor) {
        if (items.containsKey(name)) {
            throw new IllegalArgumentException("HoloItem '" + name + "' already exists");
        }
        HoloItem item = new HoloItem(name, anchor);
        items.put(name, item);
        return item;
    }

    /**
     * Get a HoloItem by name.
     *
     * @param name the HoloItem name
     * @return the HoloItem, or null if not found
     */
    public HoloItem get(String name) {
        return items.get(name);
    }

    /**
     * Check if a HoloItem with the given name exists.
     */
    public boolean exists(String name) {
        return items.containsKey(name);
    }

    /**
     * Remove and clean up a HoloItem by name.
     * This removes all child display entities from the world.
     *
     * @param name the name of the HoloItem to remove
     */
    public void remove(String name) {
        HoloItem item = items.remove(name);
        if (item != null) {
            for (HoloElement element : item.getElements()) {
                element.remove();
            }
        }
    }

    /**
     * Get an unmodifiable set of all HoloItem names.
     */
    public Set<String> getNames() {
        return Collections.unmodifiableSet(items.keySet());
    }

    /**
     * Get an unmodifiable collection of all HoloItems.
     */
    public Collection<HoloItem> getAll() {
        return Collections.unmodifiableCollection(items.values());
    }

    /**
     * Get the number of tracked HoloItems.
     */
    public int count() {
        return items.size();
    }

    /**
     * Remove and clean up all HoloItems.
     * <p>
     * Should be called when the plugin is disabled to ensure
     * all non-persistent display entities are removed from the world.
     */
    public void removeAll() {
        for (HoloItem item : items.values()) {
            for (HoloElement element : item.getElements()) {
                element.remove();
            }
        }
        items.clear();
    }

    /**
     * Untrack a HoloItem by name. Called internally by {@link HoloItem#remove()}.
     * This only removes the item from the tracking map without cleaning up entities.
     */
    void untrack(String name) {
        items.remove(name);
    }
}
