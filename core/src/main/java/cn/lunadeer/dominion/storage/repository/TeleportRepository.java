package cn.lunadeer.dominion.storage.repository;

import java.sql.SQLException;
import java.util.UUID;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class TeleportRepository extends RepositorySupport {
    public static Integer getCachedDominionId(UUID uuid) throws SQLException {
        return sql(() -> db().select(TP_DOM_ID).from(TP_CACHE).where(TP_UUID.eq(uuid.toString())).fetchOne(TP_DOM_ID));
    }

    public static void delete(UUID uuid) throws SQLException {
        sql(() -> db().deleteFrom(TP_CACHE).where(TP_UUID.eq(uuid.toString())).execute());
    }

    public static void upsert(UUID uuid, Integer dominionId) throws SQLException {
        sql(() -> db().insertInto(TP_CACHE)
                .set(TP_UUID, uuid.toString())
                .set(TP_DOM_ID, dominionId)
                .onDuplicateKeyUpdate()
                .set(TP_DOM_ID, dominionId)
                .execute());
    }
}
