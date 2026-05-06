package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import org.jooq.Record;

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
        return sql(() -> {
            var records = db().select()
                    .from(DOMINION)
                    .where(DOM_SERVER_ID.eq(serverId).and(DOM_ID.ge(0)))
                    .fetch();
            return rows(records);
        });
    }

    public static DominionRow select(Integer id) throws SQLException {
        return sql(() -> {
            Record record = db().select()
                    .from(DOMINION)
                    .where(DOM_ID.eq(id))
                    .fetchOne();
            if (record == null) return null;
            return rows(List.of(record)).get(0);
        });
    }

    public static DominionRow select(String name) throws SQLException {
        return sql(() -> {
            Record record = db().select()
                    .from(DOMINION)
                    .where(DOM_NAME.eq(name))
                    .fetchOne();
            if (record == null) return null;
            return rows(List.of(record)).get(0);
        });
    }

    public static DominionRow insert(DominionRow dominion) throws SQLException {
        return sql(() -> {
            Map<org.jooq.Field<?>, Object> values = new LinkedHashMap<>();
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
            Integer id = db().insertInto(DOMINION)
                    .set(values)
                    .returningResult(DOM_ID)
                    .fetchOne(DOM_ID);
            return select(id);
        });
    }

    public static void deleteById(Integer id) throws SQLException {
        sql(() -> db().deleteFrom(DOMINION).where(DOM_ID.eq(id)).execute());
    }

    public static void deleteByPlayerUuid(UUID playerUUID) throws SQLException {
        sql(() -> db().deleteFrom(DOMINION).where(DOM_OWNER.eq(playerUUID.toString())).execute());
    }

    public static void updateOwner(Integer id, UUID owner) throws SQLException {
        update(DOM_OWNER, owner.toString(), id);
    }

    public static void updateName(Integer id, String name) throws SQLException {
        update(DOM_NAME, name, id);
    }

    public static void updateCuboid(Integer id, int x1, int y1, int z1, int x2, int y2, int z2) throws SQLException {
        sql(() -> db().update(DOMINION)
                .set(DOM_X1, x1).set(DOM_Y1, y1).set(DOM_Z1, z1)
                .set(DOM_X2, x2).set(DOM_Y2, y2).set(DOM_Z2, z2)
                .where(DOM_ID.eq(id))
                .execute());
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
        sql(() -> {
            updateFlag(DOMINION, DOM_ID, id, flag, value);
            return 0;
        });
    }

    public static void updateGuestFlag(Integer id, PriFlag flag, Boolean value) throws SQLException {
        sql(() -> {
            updateFlag(DOMINION, DOM_ID, id, flag, value);
            return 0;
        });
    }

    private static <T> void update(org.jooq.Field<T> field, T value, Integer id) throws SQLException {
        sql(() -> db().update(DOMINION).set(field, value).where(DOM_ID.eq(id)).execute());
    }

    private static List<DominionRow> rows(Collection<? extends Record> records) {
        List<DominionRow> rows = new ArrayList<>();
        for (Record record : records) {
            rows.add(new DominionRow(
                    record.get(DOM_ID),
                    UUID.fromString(record.get(DOM_OWNER)),
                    record.get(DOM_NAME),
                    UUID.fromString(record.get(DOM_WORLD_UID)),
                    record.get(DOM_X1),
                    record.get(DOM_Y1),
                    record.get(DOM_Z1),
                    record.get(DOM_X2),
                    record.get(DOM_Y2),
                    record.get(DOM_Z2),
                    record.get(DOM_PARENT_DOM_ID),
                    record.get(DOM_JOIN_MESSAGE),
                    record.get(DOM_LEAVE_MESSAGE),
                    readEnvFlags(record),
                    readPriFlags(record),
                    record.get(DOM_TP_LOCATION),
                    record.get(DOM_COLOR),
                    record.get(DOM_SERVER_ID),
                    toBoolean(record.get(DOM_OWNER_GLOW), false)
            ));
        }
        return rows;
    }
}
