package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.PriFlag;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class MemberRepository extends RepositorySupport {
    public record MemberRow(Integer id, UUID playerUUID, Integer domID, Map<PriFlag, Boolean> flags, Integer groupId) {
    }

    public static MemberRow insert(UUID playerUUID, Integer domId, Map<PriFlag, Boolean> flags) throws SQLException {
        return sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(MEMBER_PLAYER_UUID, playerUUID.toString());
            values.put(MEMBER_DOM_ID, domId);
            values.put(MEMBER_GROUP_ID, -1);
            putPriFlags(values, flags);
            mapper.insert(MEMBER, values);
            Integer id = toInteger(values.get(MEMBER_ID));
            if (id != null) {
                return row(mapper.selectWhere(MEMBER, MEMBER_ID, id));
            }
            return rows(mapper.selectWhere(MEMBER, MEMBER_PLAYER_UUID, playerUUID.toString()))
                    .stream().filter(row -> Objects.equals(row.domID(), domId)).findFirst().orElse(null);
        });
    }

    public static List<MemberRow> select() throws SQLException {
        return sql((session, mapper) -> rows(mapper.selectAll(MEMBER)));
    }

    public static MemberRow select(Integer id) throws SQLException {
        return sql((session, mapper) -> row(mapper.selectWhere(MEMBER, MEMBER_ID, id)));
    }

    public static List<MemberRow> selectByDominionId(Integer domId) throws SQLException {
        return sql((session, mapper) -> rows(mapper.selectWhere(MEMBER, MEMBER_DOM_ID, domId)));
    }

    public static List<MemberRow> selectByGroupId(Integer groupId) throws SQLException {
        return sql((session, mapper) -> rows(mapper.selectWhere(MEMBER, MEMBER_GROUP_ID, groupId)));
    }

    public static void deleteById(Integer id) throws SQLException {
        sql((session, mapper) -> mapper.deleteWhere(MEMBER, MEMBER_ID, id));
    }

    public static void deleteByPlayerUuid(UUID playerUUID) throws SQLException {
        sql((session, mapper) -> mapper.deleteWhere(MEMBER, MEMBER_PLAYER_UUID, playerUUID.toString()));
    }

    public static void updateGroupId(Integer id, Integer groupId) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(MEMBER_GROUP_ID, groupId);
            return mapper.updateColumns(MEMBER, MEMBER_ID, id, values);
        });
    }

    public static void updateFlag(Integer id, PriFlag flag, Boolean value) throws SQLException {
        sql((session, mapper) -> {
            updateFlag(mapper, MEMBER, MEMBER_ID, id, flag, value);
            return 0;
        });
    }

    public static void updateFlags(Integer id, Map<PriFlag, Boolean> flags) throws SQLException {
        sql((session, mapper) -> {
            updatePriFlags(mapper, MEMBER, MEMBER_ID, id, flags);
            return 0;
        });
    }

    private static MemberRow row(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return null;
        return row(rows.get(0));
    }

    private static List<MemberRow> rows(List<Map<String, Object>> rows) {
        return rows.stream().map(MemberRepository::row).toList();
    }

    private static MemberRow row(Map<String, Object> row) {
        return new MemberRow(
                integer(row, MEMBER_ID),
                UUID.fromString(string(row, MEMBER_PLAYER_UUID)),
                integer(row, MEMBER_DOM_ID),
                readPriFlags(row),
                integer(row, MEMBER_GROUP_ID)
        );
    }
}
