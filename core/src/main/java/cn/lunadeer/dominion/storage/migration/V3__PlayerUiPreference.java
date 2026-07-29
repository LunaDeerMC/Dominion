package cn.lunadeer.dominion.storage.migration;

import cn.lunadeer.dominion.storage.DatabaseType;
import org.flywaydb.core.api.migration.Context;

/**
 * Stores a player's UI override independently from the public PlayerDTO.
 */
public final class V3__PlayerUiPreference extends AbstractJavaMigration {
    public V3__PlayerUiPreference(DatabaseType type) {
        super(type);
    }

    @Override
    public void migrate(Context context) throws Exception {
        addColumnIfMissing(context.getConnection(), "player_name",
                "ui_preference " + text() + " NOT NULL DEFAULT 'DEFAULT'");
    }
}
