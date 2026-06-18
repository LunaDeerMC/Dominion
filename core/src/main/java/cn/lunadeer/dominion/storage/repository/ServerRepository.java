package cn.lunadeer.dominion.storage.repository;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class ServerRepository extends RepositorySupport {
    public static String getServerName(Integer id) throws SQLException {
        return sql((session, mapper) -> {
            Object value = mapper.selectValue(SERVER_INFO, SERVER_NAME, SERVER_ID, id);
            return value == null ? null : value.toString();
        });
    }

    public static Integer getServerId(String name) throws SQLException {
        return sql((session, mapper) -> toInteger(mapper.selectValue(SERVER_INFO, SERVER_ID, SERVER_NAME, name)));
    }

    public static void insertServer(Integer id, String name) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(SERVER_ID, id);
            values.put(SERVER_NAME, name);
            return mapper.insertIgnore(SERVER_INFO, values, databaseType(), SERVER_ID);
        });
    }

    public static void updateServerName(Integer id, String name) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(SERVER_NAME, name);
            return mapper.updateColumns(SERVER_INFO, SERVER_ID, id, values);
        });
    }
}
