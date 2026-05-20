package cn.lunadeer.dominion.storage.repository;

import java.sql.SQLException;
import java.util.UUID;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class TeleportRepository extends RepositorySupport {
    public static Integer getCachedDominionId(UUID uuid) throws SQLException {
        return sql((session, mapper) -> toInteger(mapper.selectValue(TP_CACHE, TP_DOM_ID, TP_UUID, uuid.toString())));
    }

    public static void delete(UUID uuid) throws SQLException {
        sql((session, mapper) -> mapper.deleteWhere(TP_CACHE, TP_UUID, uuid.toString()));
    }

    public static void upsert(UUID uuid, Integer dominionId) throws SQLException {
        sql((session, mapper) -> mapper.upsertTeleport(uuid.toString(), dominionId, databaseType()));
    }
}
