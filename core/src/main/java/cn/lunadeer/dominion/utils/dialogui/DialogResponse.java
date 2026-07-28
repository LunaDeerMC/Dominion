package cn.lunadeer.dominion.utils.dialogui;

import java.util.Objects;

/**
 * Version-independent view of values returned by a dialog action.
 */
public record DialogResponse(DialogPayload payload) {
    public DialogResponse {
        Objects.requireNonNull(payload, "payload");
    }

    public String getText(String key) {
        DialogPayload.Value value = payload.get(key);
        return value instanceof DialogPayload.StringValue text ? text.value() : null;
    }

    public String getOption(String key) {
        return getText(key);
    }

    public Boolean getBoolean(String key) {
        DialogPayload.Value value = payload.get(key);
        if (value instanceof DialogPayload.BooleanValue bool) return bool.value();
        if (value instanceof DialogPayload.StringValue text) {
            if ("true".equalsIgnoreCase(text.value())) return true;
            if ("false".equalsIgnoreCase(text.value())) return false;
        }
        return null;
    }

    public Float getFloat(String key) {
        DialogPayload.Value value = payload.get(key);
        if (value instanceof DialogPayload.FloatValue number) return (float) number.value();
        if (value instanceof DialogPayload.IntegerValue number) return (float) number.value();
        return null;
    }
}
