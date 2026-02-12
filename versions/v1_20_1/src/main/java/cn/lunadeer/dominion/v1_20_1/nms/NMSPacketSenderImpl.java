package cn.lunadeer.dominion.v1_20_1.nms;

import cn.lunadeer.dominion.nms.NMSPacketSender;
import net.minecraft.network.protocol.Packet;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class NMSPacketSenderImpl implements NMSPacketSender {

    @Override
    public void sendPacket(Player player, Object packet) {
        if (player == null || packet == null) return;
        ((CraftPlayer) player).getHandle().connection.send((Packet<?>) packet);
    }
}
