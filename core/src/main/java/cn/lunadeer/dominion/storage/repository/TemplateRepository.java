package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.PriFlag;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class TemplateRepository extends RepositorySupport {
    public record TemplateRow(Integer id, UUID creator, String name, Map<PriFlag, Boolean> flags) {
    }

    public static TemplateRow create(UUID creator, String name) throws SQLException {
        return sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(TEMPLATE_CREATOR, creator.toString());
            values.put(TEMPLATE_NAME, name);
            putPriFlags(values, defaultPriFlags());
            mapper.insert(TEMPLATE, values);
            Integer id = toInteger(values.get(TEMPLATE_ID));
            if (id != null) {
                return row(mapper.selectWhere(TEMPLATE, TEMPLATE_ID, id));
            }
            return row(mapper.selectWhereAll(TEMPLATE, templateKey(creator, name)));
        });
    }

    public static TemplateRow select(Integer id) throws SQLException {
        return sql((session, mapper) -> row(mapper.selectWhere(TEMPLATE, TEMPLATE_ID, id)));
    }

    public static TemplateRow select(UUID creator, String name) throws SQLException {
        return sql((session, mapper) -> row(mapper.selectWhereAll(TEMPLATE, templateKey(creator, name))));
    }

    public static List<TemplateRow> selectAll(UUID creator) throws SQLException {
        return sql((session, mapper) -> rows(mapper.selectWhere(TEMPLATE, TEMPLATE_CREATOR, creator.toString())));
    }

    public static void delete(UUID creator, String name) throws SQLException {
        sql((session, mapper) -> mapper.deleteWhereAll(TEMPLATE, templateKey(creator, name)));
    }

    public static void updateFlag(Integer id, PriFlag flag, Boolean value) throws SQLException {
        sql((session, mapper) -> {
            updateFlag(mapper, TEMPLATE, TEMPLATE_ID, id, flag, value);
            return 0;
        });
    }

    public static void updateName(Integer id, String name) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(TEMPLATE_NAME, name);
            return mapper.updateColumns(TEMPLATE, TEMPLATE_ID, id, values);
        });
    }

    private static Map<String, Object> templateKey(UUID creator, String name) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(TEMPLATE_CREATOR, creator.toString());
        values.put(TEMPLATE_NAME, name);
        return values;
    }

    private static TemplateRow row(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return null;
        return row(rows.get(0));
    }

    private static List<TemplateRow> rows(List<Map<String, Object>> rows) {
        return rows.stream().map(TemplateRepository::row).toList();
    }

    private static TemplateRow row(Map<String, Object> row) {
        return new TemplateRow(integer(row, TEMPLATE_ID), UUID.fromString(string(row, TEMPLATE_CREATOR)),
                string(row, TEMPLATE_NAME), readPriFlags(row));
    }
}
