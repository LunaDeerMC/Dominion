package cn.lunadeer.dominion.storage.repository;

import java.sql.SQLException;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class ServerRepository extends RepositorySupport {
    public static String getServerName(Integer id) throws SQLException {
        return sql(() -> db().select(SERVER_NAME).from(SERVER_INFO).where(SERVER_ID.eq(id)).fetchOne(SERVER_NAME));
    }

    public static Integer getServerId(String name) throws SQLException {
        return sql(() -> db().select(SERVER_ID).from(SERVER_INFO).where(SERVER_NAME.eq(name)).fetchOne(SERVER_ID));
    }

    public static void insertServer(Integer id, String name) throws SQLException {
        sql(() -> db().insertInto(SERVER_INFO)
                .set(SERVER_ID, id)
                .set(SERVER_NAME, name)
                .onDuplicateKeyIgnore()
                .execute());
    }

    public static void updateServerName(Integer id, String name) throws SQLException {
        sql(() -> db().update(SERVER_INFO).set(SERVER_NAME, name).where(SERVER_ID.eq(id)).execute());
    }
}
