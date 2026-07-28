package cn.lunadeer.dominion.utils.dialogui;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies one rendered revision of a dialog session.
 */
public record DialogSessionContext(UUID sessionId, long revision) {
    public DialogSessionContext {
        Objects.requireNonNull(sessionId, "sessionId");
        if (revision < 0) throw new IllegalArgumentException("revision cannot be negative");
    }
}
