package cn.lunadeer.dominion.utils.dialogui;

import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns short-lived dialog callbacks. The registry is NMS-free and safe to
 * consume from a network thread; callback execution is intentionally returned
 * to the caller so it can be scheduled onto the player's server thread.
 */
public final class DialogCallbackRegistry {
    public static final DialogCallbackRegistry INSTANCE = new DialogCallbackRegistry();
    public static final String TOKEN_KEY = "__dominion_token";
    public static final String CALLBACK_ACTION_ID = "dominion:dialog_callback";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, StoredCallback> callbacks = new ConcurrentHashMap<>();
    private final Map<UUID, DialogSessionContext> currentContexts = new ConcurrentHashMap<>();

    private DialogCallbackRegistry() {
    }

    public void beginRender(UUID playerId, DialogSessionContext context) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(context, "context");
        currentContexts.put(playerId, context);
        callbacks.entrySet().removeIf(entry -> entry.getValue().playerId.equals(playerId));
        removeExpired();
    }

    public String register(
            UUID playerId,
            DialogSessionContext context,
            DialogSpec.Callback callback,
            DialogSpec.CallbackOptions options
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(callback, "callback");
        options = options == null ? DialogSpec.CallbackOptions.DEFAULT : options;
        if (!context.equals(currentContexts.get(playerId))) {
            throw new IllegalStateException("Dialog render context is not current for player " + playerId);
        }

        String token;
        do {
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (callbacks.containsKey(token));

        callbacks.put(token, new StoredCallback(
                playerId,
                context,
                callback,
                expiresAt(options.lifetime()),
                options.uses()
        ));
        return token;
    }

    public Invocation consume(UUID playerId, String token, DialogResponse response) {
        if (playerId == null || token == null || token.isBlank() || response == null) return null;
        AtomicReference<Invocation> invocation = new AtomicReference<>();
        long now = System.nanoTime();

        callbacks.computeIfPresent(token, (ignored, stored) -> {
            if (!stored.playerId.equals(playerId)) return stored;
            if (stored.expiresAtNanos <= now
                    || !stored.context.equals(currentContexts.get(playerId))) {
                return null;
            }
            invocation.set(new Invocation(stored.callback, response));
            if (stored.remainingUses == DialogSpec.CallbackOptions.UNLIMITED_USES) return stored;
            int remaining = stored.remainingUses - 1;
            return remaining > 0 ? stored.withRemainingUses(remaining) : null;
        });

        return invocation.get();
    }

    public void invalidate(UUID playerId) {
        if (playerId == null) return;
        currentContexts.remove(playerId);
        callbacks.entrySet().removeIf(entry -> entry.getValue().playerId.equals(playerId));
    }

    public void clear() {
        currentContexts.clear();
        callbacks.clear();
    }

    public int size() {
        removeExpired();
        return callbacks.size();
    }

    private void removeExpired() {
        long now = System.nanoTime();
        callbacks.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos <= now);
    }

    private static long expiresAt(Duration lifetime) {
        try {
            return Math.addExact(System.nanoTime(), lifetime.toNanos());
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    public record Invocation(DialogSpec.Callback callback, DialogResponse response) {
        public void execute(Player player) {
            callback.handle(player, response);
        }
    }

    private record StoredCallback(
            UUID playerId,
            DialogSessionContext context,
            DialogSpec.Callback callback,
            long expiresAtNanos,
            int remainingUses
    ) {
        private StoredCallback withRemainingUses(int value) {
            return new StoredCallback(playerId, context, callback, expiresAtNanos, value);
        }
    }
}
