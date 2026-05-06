package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import org.jooq.Record;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class GroupRepository extends RepositorySupport {
    public record GroupRow(Integer id, Integer domID, String namePlain, Map<PriFlag, Boolean> flags, String nameColored) {
    }

    public static GroupRow create(Integer domId, String plainName, String coloredName, Map<PriFlag, Boolean> flags) throws SQLException {
        return sql(() -> {
            Map<org.jooq.Field<?>, Object> values = new LinkedHashMap<>();
            values.put(GROUP_DOM_ID, domId);
            values.put(GROUP_NAME, plainName);
            values.put(GROUP_NAME_COLORED, coloredName);
            putPriFlags(values, flags);
            Integer id = db().insertInto(GROUP)
                    .set(values)
                    .returningResult(GROUP_ID)
                    .fetchOne(GROUP_ID);
            return select(id);
        });
    }

    public static List<GroupRow> select() throws SQLException {
        return sql(() -> rows(db().select().from(GROUP).fetch()));
    }

    public static GroupRow select(Integer id) throws SQLException {
        return sql(() -> {
            Record record = db().select()
                    .from(GROUP).where(GROUP_ID.eq(id)).fetchOne();
            if (record == null) return null;
            return rows(List.of(record)).get(0);
        });
    }

    public static List<GroupRow> selectByDominionId(Integer domId) throws SQLException {
        return sql(() -> rows(db().select()
                .from(GROUP).where(GROUP_DOM_ID.eq(domId)).fetch()));
    }

    public static void deleteById(Integer id) throws SQLException {
        sql(() -> db().deleteFrom(GROUP).where(GROUP_ID.eq(id)).execute());
    }

    public static void updateName(Integer id, String plainName, String coloredName) throws SQLException {
        sql(() -> db().update(GROUP)
                .set(GROUP_NAME, plainName)
                .set(GROUP_NAME_COLORED, coloredName)
                .where(GROUP_ID.eq(id))
                .execute());
    }

    public static void updateFlag(Integer id, PriFlag flag, Boolean value) throws SQLException {
        sql(() -> {
            updateFlag(GROUP, GROUP_ID, id, flag, value);
            return 0;
        });
    }

    private static List<GroupRow> rows(Collection<? extends Record> records) {
        List<GroupRow> rows = new ArrayList<>();
        for (Record record : records) {
            rows.add(new GroupRow(record.get(GROUP_ID), record.get(GROUP_DOM_ID), record.get(GROUP_NAME),
                    readPriFlags(record), record.get(GROUP_NAME_COLORED)));
        }
        return rows;
    }
}
