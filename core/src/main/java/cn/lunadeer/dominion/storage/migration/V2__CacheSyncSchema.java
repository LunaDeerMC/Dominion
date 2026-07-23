package cn.lunadeer.dominion.storage.migration;

import cn.lunadeer.dominion.storage.DatabaseType;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Creates the cache update log and acknowledgement tables used for cross-server cache synchronization.
 */
public class V2__CacheSyncSchema extends AbstractJavaMigration {

    public V2__CacheSyncSchema(DatabaseType type) {
        super(type);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        createUpdateLogTable(connection);
        createUpdateAckTable(connection);
    }

    private void createUpdateLogTable(Connection connection) throws SQLException {
        execute(connection, "CREATE TABLE IF NOT EXISTS cache_update_log (" +
                "id " + autoId() + ", " +
                "entity_type VARCHAR(16) NOT NULL, " +
                "target_id INT NOT NULL, " +
                "server_id INT NOT NULL, " +
                "action VARCHAR(16) NOT NULL DEFAULT 'UPSERT', " +
                "created_at " + timestamp() + " NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")");
    }

    private void createUpdateAckTable(Connection connection) throws SQLException {
        String primaryKeyDef = type.isMySqlFamily()
                ? "PRIMARY KEY (log_id, server_id)"
                : "CONSTRAINT " + "cache_update_ack_pk" + " PRIMARY KEY (log_id, server_id)";
        execute(connection, "CREATE TABLE IF NOT EXISTS cache_update_ack (" +
                "log_id BIGINT NOT NULL, " +
                "server_id INT NOT NULL, " +
                "consumed_at " + timestamp() + " NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                primaryKeyDef +
                ")");
    }
}
