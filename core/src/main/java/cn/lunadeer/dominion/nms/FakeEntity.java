package cn.lunadeer.dominion.nms;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;

/**
 * Represents a client-side fake entity that only exists in players' clients.
 * <p>
 * No real entity is spawned on the server. All state is maintained locally and
 * sent to players via NMS packets. This means:
 * <ul>
 *   <li>Zero server-side entity overhead</li>
 *   <li>Per-player visibility control</li>
 *   <li>No interference with other plugins</li>
 * </ul>
 * <p>
 * Implementations are version-specific. Obtain instances via {@link FakeEntityFactory}.
 */
public interface FakeEntity {

    /**
     * The type of display entity.
     */
    enum DisplayType {
        BLOCK_DISPLAY,
        ITEM_DISPLAY
    }

    // ==================== Identity ====================

    /**
     * Get the fake entity ID used in packets.
     */
    int getEntityId();

    /**
     * Get the display type of this fake entity.
     */
    DisplayType getDisplayType();

    // ==================== Spawn / Destroy ====================

    /**
     * Spawn the entity on the specified players' clients.
     * Sends the spawn packet and initial metadata to each player.
     *
     * @param players the players who should see this entity
     */
    void spawn(Collection<? extends Player> players);

    /**
     * Spawn the entity on a single player's client.
     *
     * @param player the player who should see this entity
     */
    void spawn(Player player);

    /**
     * Destroy (remove) the entity from the specified players' clients.
     *
     * @param players the players who should no longer see this entity
     */
    void destroy(Collection<? extends Player> players);

    /**
     * Destroy (remove) the entity from a single player's client.
     *
     * @param player the player who should no longer see this entity
     */
    void destroy(Player player);

    // ==================== Position & Transform ====================

    /**
     * Set the location of this entity and send teleport packets to the specified players.
     *
     * @param location the new location
     * @param players  the players to update
     */
    void teleport(Location location, Collection<? extends Player> players);

    /**
     * Set the location of this entity and send the teleport packet to a single player.
     *
     * @param location the new location
     * @param player   the player to update
     */
    void teleport(Location location, Player player);

    /**
     * Set the translation offset (within the Display's Transformation).
     *
     * @param translation the translation vector
     */
    void setTranslation(Vector3f translation);

    /**
     * Set the scale of this display entity.
     *
     * @param scale the scale vector
     */
    void setScale(Vector3f scale);

    /**
     * Set the left rotation quaternion (within the Display's Transformation).
     *
     * @param rotation the rotation quaternion
     */
    void setLeftRotation(Quaternionf rotation);

    /**
     * Set the right rotation quaternion (within the Display's Transformation).
     *
     * @param rotation the rotation quaternion
     */
    void setRightRotation(Quaternionf rotation);

    // ==================== Display Properties ====================

    /**
     * Set the billboard mode.
     * <ul>
     *     <li>0 = FIXED</li>
     *     <li>1 = VERTICAL</li>
     *     <li>2 = HORIZONTAL</li>
     *     <li>3 = CENTER</li>
     * </ul>
     *
     * @param mode the billboard mode byte
     */
    void setBillboard(byte mode);

    /**
     * Set the brightness override.
     *
     * @param blockLight block light level (0-15)
     * @param skyLight   sky light level (0-15)
     */
    void setBrightness(int blockLight, int skyLight);

    /**
     * Set the view range multiplier.
     *
     * @param range the view range (default 1.0, 0 = invisible)
     */
    void setViewRange(float range);

    /**
     * Set the shadow radius and strength.
     *
     * @param radius   shadow radius in blocks
     * @param strength shadow strength (0.0 - 1.0)
     */
    void setShadow(float radius, float strength);

    /**
     * Set the glow color override (ARGB).
     *
     * @param color the glow color, or null to clear
     */
    void setGlowColor(Color color);

    /**
     * Set the interpolation duration for smooth transitions.
     *
     * @param ticks duration in ticks
     */
    void setInterpolationDuration(int ticks);

    /**
     * Set the interpolation delay.
     *
     * @param ticks delay in ticks
     */
    void setInterpolationDelay(int ticks);

    // ==================== Type-Specific Properties ====================

    /**
     * Set the block data (only for BLOCK_DISPLAY entities).
     *
     * @param blockData the new block data
     */
    void setBlockData(BlockData blockData);

    /**
     * Set the item stack (only for ITEM_DISPLAY entities).
     *
     * @param itemStack the new item stack
     */
    void setItemStack(ItemStack itemStack);

    /**
     * Set the item display transform mode (only for ITEM_DISPLAY entities).
     * <ul>
     *     <li>0 = NONE</li>
     *     <li>1 = THIRDPERSON_LEFTHAND</li>
     *     <li>2 = THIRDPERSON_RIGHTHAND</li>
     *     <li>3 = FIRSTPERSON_LEFTHAND</li>
     *     <li>4 = FIRSTPERSON_RIGHTHAND</li>
     *     <li>5 = HEAD</li>
     *     <li>6 = GUI</li>
     *     <li>7 = GROUND</li>
     *     <li>8 = FIXED</li>
     * </ul>
     *
     * @param transform the transform mode byte
     */
    void setItemTransform(byte transform);

    // ==================== Metadata Sync ====================

    /**
     * Send all current metadata to the specified players.
     * This should be called after changing any display properties to sync the changes.
     *
     * @param players the players to send metadata updates to
     */
    void sendMetadata(Collection<? extends Player> players);

    /**
     * Send all current metadata to a single player.
     *
     * @param player the player to send metadata updates to
     */
    void sendMetadata(Player player);

    /**
     * Get the current location of this fake entity.
     *
     * @return a clone of the current location
     */
    Location getLocation();
}
