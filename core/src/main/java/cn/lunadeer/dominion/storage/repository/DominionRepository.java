package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class DominionRepository extends RepositorySupport {

    public record DominionRow(Integer id, UUID owner, String name, UUID worldUid, Integer x1, Integer y1, Integer z1,
                              Integer x2, Integer y2, Integer z2, Integer parentDomId, String joinMessage,
                              String leaveMessage, Map<EnvFlag, Boolean> envFlags, Map<PriFlag, Boolean> guestFlags,
                              String tpLocation, String color, Integer serverId, Boolean ownerGlow) {
    }

    public static List<DominionRow> selectAll(Integer serverId) throws SQLException {
        return sql((session, mapper) -> rows(mapper.selectDominionsByServer(serverId)));
    }

    public static DominionRow select(Integer id) throws SQLException {
        return sql((session, mapper) -> row(mapper.selectWhere(DOMINION, DOM_ID, id)));
    }

    public static DominionRow select(String name) throws SQLException {
        return sql((session, mapper) -> row(mapper.selectWhere(DOMINION, DOM_NAME, name)));
    }

    public static DominionRow insert(DominionRow dominion) throws SQLException {
        return sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(DOM_OWNER, dominion.owner().toString());
            values.put(DOM_NAME, dominion.name());
            values.put(DOM_WORLD_UID, dominion.worldUid().toString());
            values.put(DOM_X1, dominion.x1());
            values.put(DOM_Y1, dominion.y1());
            values.put(DOM_Z1, dominion.z1());
            values.put(DOM_X2, dominion.x2());
            values.put(DOM_Y2, dominion.y2());
            values.put(DOM_Z2, dominion.z2());
            values.put(DOM_PARENT_DOM_ID, dominion.parentDomId());
            values.put(DOM_JOIN_MESSAGE, dominion.joinMessage());
            values.put(DOM_LEAVE_MESSAGE, dominion.leaveMessage());
            values.put(DOM_TP_LOCATION, dominion.tpLocation());
            values.put(DOM_COLOR, dominion.color());
            values.put(DOM_SERVER_ID, dominion.serverId());
            values.put(DOM_OWNER_GLOW, dominion.ownerGlow());
            putEnvFlags(values, dominion.envFlags());
            putPriFlags(values, dominion.guestFlags());
            mapper.insert(DOMINION, values);
            Integer id = toInteger(values.get(DOM_ID));
            if (id != null) {
                return row(mapper.selectWhere(DOMINION, DOM_ID, id));
            }
            return row(mapper.selectWhere(DOMINION, DOM_NAME, dominion.name()));
        });
    }

    public static void deleteById(Integer id) throws SQLException {
        sql((session, mapper) -> mapper.deleteWhere(DOMINION, DOM_ID, id));
    }

    public static void deleteByPlayerUuid(UUID playerUUID) throws SQLException {
        sql((session, mapper) -> mapper.deleteWhere(DOMINION, DOM_OWNER, playerUUID.toString()));
    }

    public static void updateOwner(Integer id, UUID owner) throws SQLException {
        update(DOM_OWNER, owner.toString(), id);
    }

    public static void updateName(Integer id, String name) throws SQLException {
        update(DOM_NAME, name, id);
    }

    public static void updateCuboid(Integer id, int x1, int y1, int z1, int x2, int y2, int z2) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(DOM_X1, x1);
            values.put(DOM_Y1, y1);
            values.put(DOM_Z1, z1);
            values.put(DOM_X2, x2);
            values.put(DOM_Y2, y2);
            values.put(DOM_Z2, z2);
            return mapper.updateColumns(DOMINION, DOM_ID, id, values);
        });
    }

    public static void updateJoinMessage(Integer id, String message) throws SQLException {
        update(DOM_JOIN_MESSAGE, message, id);
    }

    public static void updateLeaveMessage(Integer id, String message) throws SQLException {
        update(DOM_LEAVE_MESSAGE, message, id);
    }

    public static void updateTpLocation(Integer id, String location) throws SQLException {
        update(DOM_TP_LOCATION, location, id);
    }

    public static void updateColor(Integer id, String color) throws SQLException {
        update(DOM_COLOR, color, id);
    }

    public static void updateOwnerGlow(Integer id, Boolean ownerGlow) throws SQLException {
        update(DOM_OWNER_GLOW, ownerGlow, id);
    }

    public static void updateEnvFlag(Integer id, EnvFlag flag, Boolean value) throws SQLException {
        sql((session, mapper) -> {
            updateFlag(mapper, DOMINION, DOM_ID, id, flag, value);
            return 0;
        });
    }

    public static void updateGuestFlag(Integer id, PriFlag flag, Boolean value) throws SQLException {
        sql((session, mapper) -> {
            updateFlag(mapper, DOMINION, DOM_ID, id, flag, value);
            return 0;
        });
    }

    private static void update(String field, Object value, Integer id) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(field, value);
            return mapper.updateColumns(DOMINION, DOM_ID, id, values);
        });
    }

    private static DominionRow row(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return null;
        return row(rows.get(0));
    }

    private static List<DominionRow> rows(List<Map<String, Object>> rows) {
        return rows.stream().map(DominionRepository::row).toList();
    }

    private static DominionRow row(Map<String, Object> row) {
        return new DominionRow(
                integer(row, DOM_ID),
                UUID.fromString(string(row, DOM_OWNER)),
                string(row, DOM_NAME),
                UUID.fromString(string(row, DOM_WORLD_UID)),
                integer(row, DOM_X1),
                integer(row, DOM_Y1),
                integer(row, DOM_Z1),
                integer(row, DOM_X2),
                integer(row, DOM_Y2),
                integer(row, DOM_Z2),
                integer(row, DOM_PARENT_DOM_ID),
                string(row, DOM_JOIN_MESSAGE),
                string(row, DOM_LEAVE_MESSAGE),
                readEnvFlags(row),
                readPriFlags(row),
                string(row, DOM_TP_LOCATION),
                string(row, DOM_COLOR),
                integer(row, DOM_SERVER_ID),
                toBoolean(value(row, DOM_OWNER_GLOW), false)
        );
    }
}
