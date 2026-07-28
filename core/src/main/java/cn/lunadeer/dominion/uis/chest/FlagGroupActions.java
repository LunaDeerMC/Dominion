package cn.lunadeer.dominion.uis.chest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Pure state and sequencing logic used by grouped flag menus.
 */
final class FlagGroupActions {

    enum State {
        ALL_ENABLED,
        ALL_DISABLED,
        MIXED
    }

    record Result(int changed, List<Throwable> failures) {
        Result {
            failures = List.copyOf(failures);
        }

        int failed() {
            return failures.size();
        }
    }

    private FlagGroupActions() {
    }

    static <T> State state(Collection<T> values, Predicate<T> enabled) {
        long enabledCount = values.stream().filter(enabled).count();
        if (enabledCount == 0) {
            return State.ALL_DISABLED;
        }
        if (enabledCount == values.size()) {
            return State.ALL_ENABLED;
        }
        return State.MIXED;
    }

    static <T> boolean bulkTarget(Collection<T> values, Predicate<T> enabled) {
        return state(values, enabled) != State.ALL_ENABLED;
    }

    static <T> CompletableFuture<Result> applySequentially(Collection<T> values,
                                                           Predicate<T> needsChange,
                                                           Function<T, CompletableFuture<?>> update) {
        return applySequentially(values, needsChange, Runnable::run, update);
    }

    static <T> CompletableFuture<Result> applySequentially(Collection<T> values,
                                                           Predicate<T> needsChange,
                                                           Executor invocationExecutor,
                                                           Function<T, CompletableFuture<?>> update) {
        CompletableFuture<Result> sequence = CompletableFuture.completedFuture(new Result(0, List.of()));
        for (T value : values) {
            if (!needsChange.test(value)) {
                continue;
            }
            sequence = sequence.thenCompose(result ->
                    invoke(invocationExecutor, () -> update.apply(value))
                    .handle((ignored, throwable) -> throwable == null
                        ? new Result(result.changed() + 1, result.failures())
                        : withFailure(result, throwable)));
        }
        return sequence;
    }

    private static CompletableFuture<?> invoke(Executor executor,
                                               Supplier<CompletableFuture<?>> operation) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    operation.get().whenComplete((value, throwable) -> {
                        if (throwable == null) {
                            result.complete(value);
                        } else {
                            result.completeExceptionally(throwable);
                        }
                    });
                } catch (Throwable throwable) {
                    result.completeExceptionally(throwable);
                }
            });
        } catch (Throwable throwable) {
            result.completeExceptionally(throwable);
        }
        return result;
    }

    private static Result withFailure(Result result, Throwable throwable) {
        List<Throwable> failures = new ArrayList<>(result.failures());
        failures.add(throwable);
        return new Result(result.changed(), failures);
    }
}
