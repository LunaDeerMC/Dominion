package cn.lunadeer.dominion.utils.dialogui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, NMS-free representation of a dialog custom-click payload.
 */
public record DialogPayload(Map<String, Value> values) {
    public static final DialogPayload EMPTY = new DialogPayload(Map.of());

    public DialogPayload {
        Objects.requireNonNull(values, "values");
        Map<String, Value> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("Payload key cannot be blank");
            copy.put(key, Objects.requireNonNull(value, "payload value"));
        });
        values = Map.copyOf(copy);
    }

    public DialogPayload with(String key, Value value) {
        Map<String, Value> copy = new LinkedHashMap<>(values);
        copy.put(key, value);
        return new DialogPayload(copy);
    }

    public Value get(String key) {
        return values.get(key);
    }

    public interface Value {
    }

    public record StringValue(String value) implements Value {
        public StringValue {
            Objects.requireNonNull(value, "value");
        }
    }

    public record BooleanValue(boolean value) implements Value {
    }

    public record IntegerValue(long value) implements Value {
    }

    public record FloatValue(double value) implements Value {
        public FloatValue {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Float payload must be finite");
        }
    }

    public record ListValue(List<Value> values) implements Value {
        public ListValue {
            Objects.requireNonNull(values, "values");
            values = List.copyOf(new ArrayList<>(values));
            if (values.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Payload list cannot contain null");
            }
        }
    }

    public record CompoundValue(DialogPayload value) implements Value {
        public CompoundValue {
            Objects.requireNonNull(value, "value");
        }
    }

    public static Value string(String value) {
        return new StringValue(value);
    }

    public static Value bool(boolean value) {
        return new BooleanValue(value);
    }

    public static Value integer(long value) {
        return new IntegerValue(value);
    }

    public static Value floating(double value) {
        return new FloatValue(value);
    }

    public static Value list(List<Value> values) {
        return new ListValue(values);
    }

    public static Value compound(DialogPayload value) {
        return new CompoundValue(value);
    }
}
