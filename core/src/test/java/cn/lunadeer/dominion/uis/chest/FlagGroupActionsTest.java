package cn.lunadeer.dominion.uis.chest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagGroupActionsTest {

    @Test
    void identifiesAllThreeStatesAndSmartTarget() {
        assertEquals(FlagGroupActions.State.ALL_DISABLED,
                FlagGroupActions.state(List.of(false, false), Boolean::booleanValue));
        assertEquals(FlagGroupActions.State.ALL_ENABLED,
                FlagGroupActions.state(List.of(true, true), Boolean::booleanValue));
        assertEquals(FlagGroupActions.State.MIXED,
                FlagGroupActions.state(List.of(true, false), Boolean::booleanValue));
        assertTrue(FlagGroupActions.bulkTarget(List.of(true, false), Boolean::booleanValue));
        assertFalse(FlagGroupActions.bulkTarget(List.of(true, true), Boolean::booleanValue));
    }

    @Test
    void skipsUnchangedValuesAndContinuesAfterFailure() {
        List<Integer> attempted = new ArrayList<>();
        AtomicInteger scheduled = new AtomicInteger();
        FlagGroupActions.Result result = FlagGroupActions.applySequentially(
                List.of(1, 2, 3, 4),
                value -> value != 2,
                command -> {
                    scheduled.incrementAndGet();
                    command.run();
                },
                value -> {
                    attempted.add(value);
                    if (value == 3) {
                        return CompletableFuture.failedFuture(new IllegalStateException("expected"));
                    }
                    return CompletableFuture.completedFuture(value);
                }
        ).join();

        assertEquals(List.of(1, 3, 4), attempted);
        assertEquals(3, scheduled.get());
        assertEquals(2, result.changed());
        assertEquals(1, result.failed());
    }
}
