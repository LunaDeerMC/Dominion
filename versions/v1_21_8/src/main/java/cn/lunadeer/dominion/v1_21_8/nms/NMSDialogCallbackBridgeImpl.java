package cn.lunadeer.dominion.v1_21_8.nms;

import cn.lunadeer.dominion.nms.NMSDialogCallbackBridge;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.dialogui.DialogCallbackRegistry;
import cn.lunadeer.dominion.utils.dialogui.DialogPayload;
import cn.lunadeer.dominion.utils.dialogui.DialogResponse;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft 1.21.8 inbound packet bridge for vanilla custom-click actions.
 */
public final class NMSDialogCallbackBridgeImpl implements NMSDialogCallbackBridge {
    private static final String HANDLER_NAME = "dominion_dialog_callback";
    private static final int MAX_PAYLOAD_DEPTH = 16;
    private static final int MAX_PAYLOAD_VALUES = 256;
    private static final int MAX_KEY_LENGTH = 128;
    private static final int MAX_STRING_LENGTH = 32_767;
    private static final int MAX_LIST_LENGTH = 256;
    private static final Field LISTENER_CONNECTION = field(ServerCommonPacketListenerImpl.class, "connection");
    private static final Field CONNECTION_CHANNEL = field(Connection.class, "channel");
    private final Map<UUID, Channel> channels = new ConcurrentHashMap<>();

    @Override
    public boolean install(Player player) {
        if (player == null || !player.isOnline()) return false;
        try {
            Channel channel = channel(player);
            if (!channel.isOpen()) return false;
            Runnable install = () -> {
                if (channel.pipeline().get(HANDLER_NAME) == null) {
                    channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new InboundHandler(player));
                }
            };
            runOnEventLoop(channel, install);
            boolean installed = channel.pipeline().get(HANDLER_NAME) != null;
            if (installed) channels.put(player.getUniqueId(), channel);
            return installed;
        } catch (Throwable throwable) {
            XLogger.debug("Unable to install dialog callback bridge for {0}: {1}",
                    player.getName(), throwable.getMessage());
            return false;
        }
    }

    @Override
    public void uninstall(Player player) {
        if (player == null) return;
        Channel channel = channels.remove(player.getUniqueId());
        if (channel != null) remove(channel);
        DialogCallbackRegistry.INSTANCE.invalidate(player.getUniqueId());
    }

    @Override
    public boolean isInstalled(Player player) {
        if (player == null) return false;
        Channel channel = channels.get(player.getUniqueId());
        return channel != null && channel.isOpen() && channel.pipeline().get(HANDLER_NAME) != null;
    }

    @Override
    public void shutdown() {
        channels.values().forEach(this::remove);
        channels.clear();
        DialogCallbackRegistry.INSTANCE.clear();
    }

    private void remove(Channel channel) {
        try {
            runOnEventLoop(channel, () -> {
                if (channel.pipeline().get(HANDLER_NAME) != null) channel.pipeline().remove(HANDLER_NAME);
            });
        } catch (Throwable ignored) {
        }
    }

    private static void runOnEventLoop(Channel channel, Runnable action) {
        if (channel.eventLoop().inEventLoop()) {
            action.run();
        } else {
            channel.eventLoop().submit(action).syncUninterruptibly();
        }
    }

    private static Channel channel(Player player) throws ReflectiveOperationException {
        ServerPlayer serverPlayer = (ServerPlayer) player.getClass().getMethod("getHandle").invoke(player);
        Connection connection = (Connection) LISTENER_CONNECTION.get(serverPlayer.connection);
        return (Channel) CONNECTION_CHANNEL.get(connection);
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final class InboundHandler extends ChannelDuplexHandler {
        private final Player player;

        private InboundHandler(Player player) {
            this.player = player;
        }

        @Override
        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            if (!(message instanceof ServerboundCustomClickActionPacket packet)
                    || !DialogCallbackRegistry.CALLBACK_ACTION_ID.equals(packet.id().toString())) {
                context.fireChannelRead(message);
                return;
            }

            if (packet.payload().isEmpty() || !(packet.payload().get() instanceof CompoundTag compound)) return;
            String token = compound.getString(DialogCallbackRegistry.TOKEN_KEY).orElse(null);
            if (token == null) return;

            DialogResponse response;
            try {
                response = new DialogResponse(decodePayload(compound));
            } catch (IllegalArgumentException ignored) {
                return;
            }
            DialogCallbackRegistry.Invocation invocation = DialogCallbackRegistry.INSTANCE.consume(
                    player.getUniqueId(), token, response);
            if (invocation == null) return;

            Scheduler.runEntityTask(() -> {
                if (!player.isOnline()) return;
                try {
                    invocation.execute(player);
                } catch (Throwable throwable) {
                    XLogger.error("Dialog callback failed for {0}: {1}", player.getName(), throwable.getMessage());
                }
            }, player);
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) throws Exception {
            channels.remove(player.getUniqueId(), context.channel());
            DialogCallbackRegistry.INSTANCE.invalidate(player.getUniqueId());
            context.fireChannelInactive();
        }
    }

    private static DialogPayload payload(CompoundTag compound, DecodeBudget budget, int depth) {
        if (depth > MAX_PAYLOAD_DEPTH) throw new IllegalArgumentException("Dialog payload is too deeply nested");
        Map<String, DialogPayload.Value> values = new LinkedHashMap<>();
        for (Map.Entry<String, Tag> entry : compound.entrySet()) {
            if (entry.getKey().length() > MAX_KEY_LENGTH) {
                throw new IllegalArgumentException("Dialog payload key is too long");
            }
            budget.take();
            values.put(entry.getKey(), value(entry.getValue(), budget, depth + 1));
        }
        return new DialogPayload(values);
    }

    static DialogPayload decodePayload(CompoundTag compound) {
        return payload(compound, new DecodeBudget(), 0);
    }

    private static DialogPayload.Value value(Tag tag, DecodeBudget budget, int depth) {
        if (depth > MAX_PAYLOAD_DEPTH) throw new IllegalArgumentException("Dialog payload is too deeply nested");
        return switch (tag.getId()) {
            case Tag.TAG_STRING -> {
                String value = tag.asString().orElse("");
                if (value.length() > MAX_STRING_LENGTH) {
                    throw new IllegalArgumentException("Dialog payload string is too long");
                }
                yield DialogPayload.string(value);
            }
            case Tag.TAG_BYTE -> DialogPayload.bool(tag.asBoolean().orElse(false));
            case Tag.TAG_SHORT, Tag.TAG_INT, Tag.TAG_LONG -> DialogPayload.integer(tag.asLong().orElse(0L));
            case Tag.TAG_FLOAT, Tag.TAG_DOUBLE -> DialogPayload.floating(tag.asDouble().orElse(0D));
            case Tag.TAG_COMPOUND -> DialogPayload.compound(payload((CompoundTag) tag, budget, depth));
            case Tag.TAG_LIST -> {
                if (((ListTag) tag).size() > MAX_LIST_LENGTH) {
                    throw new IllegalArgumentException("Dialog payload list is too long");
                }
                ArrayList<DialogPayload.Value> entries = new ArrayList<>();
                for (Tag entry : (ListTag) tag) {
                    budget.take();
                    entries.add(value(entry, budget, depth + 1));
                }
                yield DialogPayload.list(entries);
            }
            default -> throw new IllegalArgumentException("Unsupported dialog payload tag " + tag.getId());
        };
    }

    private static final class DecodeBudget {
        private int remaining = MAX_PAYLOAD_VALUES;

        private void take() {
            if (--remaining < 0) throw new IllegalArgumentException("Dialog payload contains too many values");
        }
    }
}
