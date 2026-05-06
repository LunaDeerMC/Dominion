package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import org.jooq.Record;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class TemplateRepository extends RepositorySupport {
    public record TemplateRow(Integer id, UUID creator, String name, Map<PriFlag, Boolean> flags) {
    }

    public static TemplateRow create(UUID creator, String name) throws SQLException {
        return sql(() -> {
            Map<org.jooq.Field<?>, Object> values = new LinkedHashMap<>();
            values.put(TEMPLATE_CREATOR, creator.toString());
            values.put(TEMPLATE_NAME, name);
            putPriFlags(values, defaultPriFlags());
            Integer id = db().insertInto(TEMPLATE)
                    .set(values)
                    .returningResult(TEMPLATE_ID)
                    .fetchOne(TEMPLATE_ID);
            return select(id);
        });
    }

    public static TemplateRow select(Integer id) throws SQLException {
        return sql(() -> {
            Record record = db().select()
                    .from(TEMPLATE).where(TEMPLATE_ID.eq(id)).fetchOne();
            if (record == null) return null;
            return rows(List.of(record)).get(0);
        });
    }

    public static TemplateRow select(UUID creator, String name) throws SQLException {
        return sql(() -> {
            Record record = db().select()
                    .from(TEMPLATE).where(TEMPLATE_CREATOR.eq(creator.toString()).and(TEMPLATE_NAME.eq(name))).fetchOne();
            if (record == null) return null;
            return rows(List.of(record)).get(0);
        });
    }

    public static List<TemplateRow> selectAll(UUID creator) throws SQLException {
        return sql(() -> rows(db().select()
                .from(TEMPLATE).where(TEMPLATE_CREATOR.eq(creator.toString())).fetch()));
    }

    public static void delete(UUID creator, String name) throws SQLException {
        sql(() -> db().deleteFrom(TEMPLATE)
                .where(TEMPLATE_CREATOR.eq(creator.toString()).and(TEMPLATE_NAME.eq(name)))
                .execute());
    }

    public static void updateFlag(Integer id, PriFlag flag, Boolean value) throws SQLException {
        sql(() -> {
            updateFlag(TEMPLATE, TEMPLATE_ID, id, flag, value);
            return 0;
        });
    }

    public static void updateName(Integer id, String name) throws SQLException {
        sql(() -> db().update(TEMPLATE).set(TEMPLATE_NAME, name).where(TEMPLATE_ID.eq(id)).execute());
    }

    private static List<TemplateRow> rows(Collection<? extends Record> records) {
        List<TemplateRow> rows = new ArrayList<>();
        for (Record record : records) {
            rows.add(new TemplateRow(record.get(TEMPLATE_ID), UUID.fromString(record.get(TEMPLATE_CREATOR)),
                    record.get(TEMPLATE_NAME), readPriFlags(record)));
        }
        return rows;
    }
}
