package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import org.jooq.Record;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class MemberRepository extends RepositorySupport {
    public record MemberRow(Integer id, UUID playerUUID, Integer domID, Map<PriFlag, Boolean> flags, Integer groupId) {
    }

    public static MemberRow insert(UUID playerUUID, Integer domId, Map<PriFlag, Boolean> flags) throws SQLException {
        return sql(() -> {
            Map<org.jooq.Field<?>, Object> values = new LinkedHashMap<>();
            values.put(MEMBER_PLAYER_UUID, playerUUID.toString());
            values.put(MEMBER_DOM_ID, domId);
            values.put(MEMBER_GROUP_ID, -1);
            putPriFlags(values, flags);
            Integer id = db().insertInto(MEMBER)
                    .set(values)
                    .returningResult(MEMBER_ID)
                    .fetchOne(MEMBER_ID);
            return select(id);
        });
    }

    public static List<MemberRow> select() throws SQLException {
        return sql(() -> rows(db().select().from(MEMBER).fetch()));
    }

    public static MemberRow select(Integer id) throws SQLException {
        return sql(() -> {
            Record record = db().select()
                    .from(MEMBER).where(MEMBER_ID.eq(id)).fetchOne();
            if (record == null) return null;
            return rows(List.of(record)).get(0);
        });
    }

    public static List<MemberRow> selectByDominionId(Integer domId) throws SQLException {
        return sql(() -> rows(db().select()
                .from(MEMBER).where(MEMBER_DOM_ID.eq(domId)).fetch()));
    }

    public static List<MemberRow> selectByGroupId(Integer groupId) throws SQLException {
        return sql(() -> rows(db().select()
                .from(MEMBER).where(MEMBER_GROUP_ID.eq(groupId)).fetch()));
    }

    public static void deleteById(Integer id) throws SQLException {
        sql(() -> db().deleteFrom(MEMBER).where(MEMBER_ID.eq(id)).execute());
    }

    public static void deleteByPlayerUuid(UUID playerUUID) throws SQLException {
        sql(() -> db().deleteFrom(MEMBER).where(MEMBER_PLAYER_UUID.eq(playerUUID.toString())).execute());
    }

    public static void updateGroupId(Integer id, Integer groupId) throws SQLException {
        sql(() -> db().update(MEMBER).set(MEMBER_GROUP_ID, groupId).where(MEMBER_ID.eq(id)).execute());
    }

    public static void updateFlag(Integer id, PriFlag flag, Boolean value) throws SQLException {
        sql(() -> {
            updateFlag(MEMBER, MEMBER_ID, id, flag, value);
            return 0;
        });
    }

    public static void updateFlags(Integer id, Map<PriFlag, Boolean> flags) throws SQLException {
        sql(() -> {
            updatePriFlags(MEMBER, MEMBER_ID, id, flags);
            return 0;
        });
    }

    private static List<MemberRow> rows(Collection<? extends Record> records) {
        List<MemberRow> rows = new ArrayList<>();
        for (Record record : records) {
            rows.add(new MemberRow(
                    record.get(MEMBER_ID),
                    UUID.fromString(record.get(MEMBER_PLAYER_UUID)),
                    record.get(MEMBER_DOM_ID),
                    readPriFlags(record),
                    record.get(MEMBER_GROUP_ID)
            ));
        }
        return rows;
    }
}
