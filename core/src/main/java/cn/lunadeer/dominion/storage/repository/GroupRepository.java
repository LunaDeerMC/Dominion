package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.PriFlag;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class GroupRepository extends RepositorySupport {
    public record GroupRow(Integer id, Integer domID, String namePlain, Map<PriFlag, Boolean> flags, String nameColored) {
    }

    public static GroupRow create(Integer domId, String plainName, String coloredName, Map<PriFlag, Boolean> flags) throws SQLException {
        return sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(GROUP_DOM_ID, domId);
            values.put(GROUP_NAME, plainName);
            values.put(GROUP_NAME_COLORED, coloredName);
            putPriFlags(values, flags);
            mapper.insert(GROUP, values);
            Integer id = toInteger(values.get(GROUP_ID));
            if (id != null) {
                return row(mapper.selectWhere(GROUP, GROUP_ID, id));
            }
            return rows(mapper.selectWhere(GROUP, GROUP_DOM_ID, domId))
                    .stream().filter(row -> Objects.equals(row.namePlain(), plainName)).findFirst().orElse(null);
        });
    }

    public static List<GroupRow> select() throws SQLException {
        return sql((session, mapper) -> rows(mapper.selectAll(GROUP)));
    }

    public static GroupRow select(Integer id) throws SQLException {
        return sql((session, mapper) -> row(mapper.selectWhere(GROUP, GROUP_ID, id)));
    }

    public static List<GroupRow> selectByDominionId(Integer domId) throws SQLException {
        return sql((session, mapper) -> rows(mapper.selectWhere(GROUP, GROUP_DOM_ID, domId)));
    }

    public static void deleteById(Integer id) throws SQLException {
        sql((session, mapper) -> mapper.deleteWhere(GROUP, GROUP_ID, id));
    }

    public static void updateName(Integer id, String plainName, String coloredName) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(GROUP_NAME, plainName);
            values.put(GROUP_NAME_COLORED, coloredName);
            return mapper.updateColumns(GROUP, GROUP_ID, id, values);
        });
    }

    public static void updateFlag(Integer id, PriFlag flag, Boolean value) throws SQLException {
        sql((session, mapper) -> {
            updateFlag(mapper, GROUP, GROUP_ID, id, flag, value);
            return 0;
        });
    }

    private static GroupRow row(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return null;
        return row(rows.get(0));
    }

    private static List<GroupRow> rows(List<Map<String, Object>> rows) {
        return rows.stream().map(GroupRepository::row).toList();
    }

    private static GroupRow row(Map<String, Object> row) {
        return new GroupRow(integer(row, GROUP_ID), integer(row, GROUP_DOM_ID), string(row, GROUP_NAME),
                readPriFlags(row), string(row, GROUP_NAME_COLORED));
    }
}
