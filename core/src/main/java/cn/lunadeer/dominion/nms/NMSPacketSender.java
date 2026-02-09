package cn.lunadeer.dominion.nms;

import org.bukkit.entity.Player;

/**
 * Generic interface for sending raw NMS packets to specific players.
 * <p>
 * Version-specific implementations are loaded at runtime based on the server version.
 * Use {@link NMSManager#getPacketSender()} to obtain the current implementation.
 */
public interface NMSPacketSender {

    /**
     * Send a raw NMS packet object to a specific player.
     *
     * @param player the target player
     * @param packet the NMS packet object
     */
    void sendPacket(Player player, Object packet);
}
