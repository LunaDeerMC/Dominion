package cn.lunadeer.dominion.v1_21_8.nms;

import cn.lunadeer.dominion.nms.NMSPacketSender;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;

/**
 * NMS packet sender implementation for Minecraft 1.21.8.
 */
public class NMSPacketSenderImpl implements NMSPacketSender {

    @Override
    public void sendPacket(Player player, Object packet) {
        if (player == null || packet == null) return;
        getServerPlayer(player).connection.send((Packet<?>) packet);
    }

    private static ServerPlayer getServerPlayer(Player player) {
        try {
            return (ServerPlayer) player.getClass().getMethod("getHandle").invoke(player);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get ServerPlayer handle", e);
        }
    }
}
