package cn.lunadeer.dominion.utils.dialogui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DialogCallbackRegistryTest {
    private final DialogCallbackRegistry registry = DialogCallbackRegistry.INSTANCE;

    @BeforeEach
    @AfterEach
    void clearRegistry() {
        registry.clear();
    }

    @Test
    void tokenIsPlayerAndRevisionBoundAndSingleUseByDefault() {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        DialogSessionContext context = new DialogSessionContext(UUID.randomUUID(), 1);
        AtomicInteger calls = new AtomicInteger();
        registry.beginRender(owner, context);
        String token = registry.register(owner, context,
                (player, response) -> calls.incrementAndGet(), DialogSpec.CallbackOptions.DEFAULT);
        DialogResponse response = new DialogResponse(new DialogPayload(
                Map.of("value", DialogPayload.string("answer"))));

        assertEquals(22, token.length());
        assertNull(registry.consume(stranger, token, response));
        assertEquals(1, registry.size(), "another player must not invalidate the owner's token");

        DialogCallbackRegistry.Invocation invocation = registry.consume(owner, token, response);
        assertNotNull(invocation);
        invocation.execute(null);
        assertEquals(1, calls.get());
        assertNull(registry.consume(owner, token, response), "default callback must reject replay");
    }

    @Test
    void newRenderInvalidatesOldRevisionAndConfiguredUsesAreHonored() {
        UUID player = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        DialogSessionContext first = new DialogSessionContext(session, 1);
        DialogSessionContext second = new DialogSessionContext(session, 2);
        DialogResponse response = new DialogResponse(DialogPayload.EMPTY);

        registry.beginRender(player, first);
        String stale = registry.register(player, first, (ignored, value) -> {
        }, DialogSpec.CallbackOptions.DEFAULT);
        registry.beginRender(player, second);
        assertNull(registry.consume(player, stale, response));

        String twice = registry.register(player, second, (ignored, value) -> {
        }, new DialogSpec.CallbackOptions(Duration.ofMinutes(1), 2));
        assertNotNull(registry.consume(player, twice, response));
        assertNotNull(registry.consume(player, twice, response));
        assertNull(registry.consume(player, twice, response));
    }

    @Test
    void rejectsRegistrationOutsideCurrentRenderAndInvalidOptions() {
        UUID player = UUID.randomUUID();
        DialogSessionContext context = new DialogSessionContext(UUID.randomUUID(), 0);
        assertThrows(IllegalStateException.class,
                () -> registry.register(player, context, (ignored, response) -> {
                }, DialogSpec.CallbackOptions.DEFAULT));
        assertThrows(IllegalArgumentException.class,
                () -> new DialogSpec.CallbackOptions(Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new DialogSpec.CallbackOptions(Duration.ofSeconds(1), 0));
    }
}
