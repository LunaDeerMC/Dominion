package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.configuration.Configuration;
import org.jooq.Record;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

public class PlayerRepository extends RepositorySupport {

    public record PlayerRow(Integer id, UUID uuid, String lastKnownName, LocalDateTime lastJoinAt,
                            Integer usingGroupTitleId, String skinUrl, String uiPreference) {
    }

    public static List<PlayerRow> all() throws SQLException {
        return sql(() -> db().select(PLAYER_ID, PLAYER_UUID, PLAYER_LAST_KNOWN_NAME, PLAYER_LAST_JOIN_AT,
                        PLAYER_USING_GROUP_TITLE_ID, PLAYER_SKIN_URL, PLAYER_UI_PREFERENCE)
                .from(PLAYER_NAME)
                .where(PLAYER_ID.gt(0))
                .fetch(PlayerRepository::row));
    }

    public static PlayerRow selectById(Integer id) throws SQLException {
        return sql(() -> db().select(PLAYER_ID, PLAYER_UUID, PLAYER_LAST_KNOWN_NAME, PLAYER_LAST_JOIN_AT,
                        PLAYER_USING_GROUP_TITLE_ID, PLAYER_SKIN_URL, PLAYER_UI_PREFERENCE)
                .from(PLAYER_NAME)
                .where(PLAYER_ID.eq(id))
                .fetchOne(PlayerRepository::row));
    }

    public static PlayerRow selectByUuid(UUID uuid) throws SQLException {
        return sql(() -> db().select(PLAYER_ID, PLAYER_UUID, PLAYER_LAST_KNOWN_NAME, PLAYER_LAST_JOIN_AT,
                        PLAYER_USING_GROUP_TITLE_ID, PLAYER_SKIN_URL, PLAYER_UI_PREFERENCE)
                .from(PLAYER_NAME)
                .where(PLAYER_UUID.eq(uuid.toString()))
                .fetchOne(PlayerRepository::row));
    }

    public static PlayerRow createOrUpdate(UUID uuid, String name) throws SQLException {
        return sql(() -> {
            PlayerRow existing = selectByUuid(uuid);
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                String uiPreference = Configuration.defaultUiType;
                if (uuid.toString().startsWith("00000000") && PlayerUiType.TUI.name().equals(uiPreference)) {
                    uiPreference = PlayerUiType.CUI.name();
                }
                Integer id = db().insertInto(PLAYER_NAME)
                        .set(PLAYER_UUID, uuid.toString())
                        .set(PLAYER_LAST_KNOWN_NAME, name)
                        .set(PLAYER_LAST_JOIN_AT, Timestamp.valueOf(now))
                        .set(PLAYER_UI_PREFERENCE, uiPreference)
                        .returningResult(PLAYER_ID)
                        .fetchOne(PLAYER_ID);
                return selectById(id);
            }
            db().update(PLAYER_NAME)
                    .set(PLAYER_LAST_KNOWN_NAME, name)
                    .set(PLAYER_LAST_JOIN_AT, Timestamp.valueOf(now))
                    .where(PLAYER_UUID.eq(uuid.toString()))
                    .execute();
            return selectById(existing.id());
        });
    }

    public static void delete(Integer id) throws SQLException {
        sql(() -> db().deleteFrom(PLAYER_NAME).where(PLAYER_ID.eq(id)).execute());
    }

    public static void updateProfile(UUID uuid, String name, String skinUrl, LocalDateTime lastJoinAt) throws SQLException {
        sql(() -> db().update(PLAYER_NAME)
                .set(PLAYER_LAST_KNOWN_NAME, name)
                .set(PLAYER_SKIN_URL, skinUrl)
                .set(PLAYER_LAST_JOIN_AT, Timestamp.valueOf(lastJoinAt))
                .where(PLAYER_UUID.eq(uuid.toString()))
                .execute());
    }

    public static void updateUiPreference(UUID uuid, String uiPreference) throws SQLException {
        sql(() -> db().update(PLAYER_NAME)
                .set(PLAYER_UI_PREFERENCE, uiPreference)
                .where(PLAYER_UUID.eq(uuid.toString()))
                .execute());
    }

    public static void updateUsingGroupTitle(Integer id, Integer groupTitleId) throws SQLException {
        sql(() -> db().update(PLAYER_NAME)
                .set(PLAYER_USING_GROUP_TITLE_ID, groupTitleId)
                .where(PLAYER_ID.eq(id))
                .execute());
    }

    private static PlayerRow row(Record record) {
        return new PlayerRow(
                record.get(PLAYER_ID),
                UUID.fromString(record.get(PLAYER_UUID)),
                record.get(PLAYER_LAST_KNOWN_NAME),
                toLocalDateTime(record.get(PLAYER_LAST_JOIN_AT)),
                record.get(PLAYER_USING_GROUP_TITLE_ID),
                record.get(PLAYER_SKIN_URL),
                record.get(PLAYER_UI_PREFERENCE)
        );
    }

    private enum PlayerUiType {
        CUI, TUI
    }
}
