package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.configuration.Configuration;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class PlayerRepository extends RepositorySupport {

    public record PlayerRow(Integer id, UUID uuid, String lastKnownName, LocalDateTime lastJoinAt,
                            Integer usingGroupTitleId, String skinUrl, String uiPreference) {
    }

    public static List<PlayerRow> all() throws SQLException {
        return sql((session, mapper) -> rows(mapper.selectWhereCompare(PLAYER_NAME, PLAYER_ID, ">", 0)));
    }

    public static PlayerRow selectById(Integer id) throws SQLException {
        return sql((session, mapper) -> row(mapper.selectWhere(PLAYER_NAME, PLAYER_ID, id)));
    }

    public static PlayerRow selectByUuid(UUID uuid) throws SQLException {
        return sql((session, mapper) -> row(mapper.selectWhere(PLAYER_NAME, PLAYER_UUID, uuid.toString())));
    }

    public static PlayerRow createOrUpdate(UUID uuid, String name) throws SQLException {
        return sql((session, mapper) -> {
            PlayerRow existing = row(mapper.selectWhere(PLAYER_NAME, PLAYER_UUID, uuid.toString()));
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                String uiPreference = Configuration.defaultUiType;
                if (uuid.toString().startsWith("00000000") && PlayerUiType.TUI.name().equals(uiPreference)) {
                    uiPreference = PlayerUiType.CUI.name();
                }
                Map<String, Object> values = new LinkedHashMap<>();
                values.put(PLAYER_UUID, uuid.toString());
                values.put(PLAYER_LAST_KNOWN_NAME, name);
                values.put(PLAYER_LAST_JOIN_AT, Timestamp.valueOf(now));
                values.put(PLAYER_UI_PREFERENCE, uiPreference);
                mapper.insert(PLAYER_NAME, values);
                Integer id = toInteger(values.get(PLAYER_ID));
                if (id != null) {
                    return row(mapper.selectWhere(PLAYER_NAME, PLAYER_ID, id));
                }
                return row(mapper.selectWhere(PLAYER_NAME, PLAYER_UUID, uuid.toString()));
            }
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(PLAYER_LAST_KNOWN_NAME, name);
            values.put(PLAYER_LAST_JOIN_AT, Timestamp.valueOf(now));
            mapper.updateColumns(PLAYER_NAME, PLAYER_UUID, uuid.toString(), values);
            return row(mapper.selectWhere(PLAYER_NAME, PLAYER_ID, existing.id()));
        });
    }

    public static void delete(Integer id) throws SQLException {
        sql((session, mapper) -> mapper.deleteWhere(PLAYER_NAME, PLAYER_ID, id));
    }

    public static void updateProfile(UUID uuid, String name, String skinUrl, LocalDateTime lastJoinAt) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(PLAYER_LAST_KNOWN_NAME, name);
            values.put(PLAYER_SKIN_URL, skinUrl);
            values.put(PLAYER_LAST_JOIN_AT, Timestamp.valueOf(lastJoinAt));
            return mapper.updateColumns(PLAYER_NAME, PLAYER_UUID, uuid.toString(), values);
        });
    }

    public static void updateUiPreference(UUID uuid, String uiPreference) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(PLAYER_UI_PREFERENCE, uiPreference);
            return mapper.updateColumns(PLAYER_NAME, PLAYER_UUID, uuid.toString(), values);
        });
    }

    public static void updateUsingGroupTitle(Integer id, Integer groupTitleId) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(PLAYER_USING_GROUP_TITLE_ID, groupTitleId);
            return mapper.updateColumns(PLAYER_NAME, PLAYER_ID, id, values);
        });
    }

    private static PlayerRow row(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return null;
        return row(rows.get(0));
    }

    private static List<PlayerRow> rows(List<Map<String, Object>> rows) {
        return rows.stream().map(PlayerRepository::row).toList();
    }

    private static PlayerRow row(Map<String, Object> row) {
        return new PlayerRow(
                integer(row, PLAYER_ID),
                UUID.fromString(string(row, PLAYER_UUID)),
                string(row, PLAYER_LAST_KNOWN_NAME),
                toLocalDateTime(value(row, PLAYER_LAST_JOIN_AT)),
                integer(row, PLAYER_USING_GROUP_TITLE_ID),
                string(row, PLAYER_SKIN_URL),
                string(row, PLAYER_UI_PREFERENCE)
        );
    }

    private enum PlayerUiType {
        CUI, TUI
    }
}
