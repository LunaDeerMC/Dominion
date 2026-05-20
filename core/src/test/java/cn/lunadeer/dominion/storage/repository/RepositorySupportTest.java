package cn.lunadeer.dominion.storage.repository;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RepositorySupportTest {

    @Test
    void toLocalDateTime_handlesTimestamp() {
        Timestamp ts = Timestamp.valueOf("2026-05-20 12:00:00");
        LocalDateTime result = RepositorySupport.toLocalDateTime(ts);
        assertEquals(LocalDateTime.of(2026, 5, 20, 12, 0, 0), result);
    }

    @Test
    void toLocalDateTime_handlesLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2026, 5, 20, 12, 0, 0);
        LocalDateTime result = RepositorySupport.toLocalDateTime(ldt);
        assertEquals(ldt, result);
    }

    @Test
    void toLocalDateTime_handlesNull_returnsEpoch() {
        LocalDateTime result = RepositorySupport.toLocalDateTime(null);
        assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0), result);
    }

    @Test
    void toLocalDateTime_handlesTimestampString() {
        LocalDateTime result = RepositorySupport.toLocalDateTime("2026-05-20 12:00:00");
        assertEquals(LocalDateTime.of(2026, 5, 20, 12, 0, 0), result);
    }

    // Reproducer: SQLite returns millis-since-epoch as a Number (Long/Integer)
    // This previously threw: "Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]"
    @Test
    void toLocalDateTime_handlesLongValue_doesNotThrow() {
        assertDoesNotThrow(() -> RepositorySupport.toLocalDateTime(-28800000L));
        assertDoesNotThrow(() -> RepositorySupport.toLocalDateTime(0L));
        assertDoesNotThrow(() -> RepositorySupport.toLocalDateTime(1716192000000L));
    }

    @Test
    void toLocalDateTime_handlesIntegerValue_doesNotThrow() {
        assertDoesNotThrow(() -> RepositorySupport.toLocalDateTime(0));
        assertDoesNotThrow(() -> RepositorySupport.toLocalDateTime(-28800000));
    }

    @Test
    void toLocalDateTime_longValue_returnsNonNull() {
        LocalDateTime result = RepositorySupport.toLocalDateTime(0L);
        assertNotNull(result);
    }
}
