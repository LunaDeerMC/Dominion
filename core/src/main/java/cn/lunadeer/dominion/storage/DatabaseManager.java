package cn.lunadeer.dominion.storage;

import cn.lunadeer.dominion.storage.migration.V1__LegacySchema;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;
import org.flywaydb.core.Flyway;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    public static DatabaseManager instance;

    private final JavaPlugin plugin;
    private final HikariConfig config = new HikariConfig();
    private DatabaseType type;
    private HikariDataSource dataSource;
    private DSLContext dsl;

    public DatabaseManager(JavaPlugin plugin, String type, String host, String port, String name, String user, String pass) {
        instance = this;
        this.plugin = plugin;
        set(type, host, port, name, user, pass, 10);
    }

    public void set(String type, String host, String port, String name, String user, String pass, int poolSize) {
        try {
            this.type = DatabaseType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported database type: " + type, e);
        }

        config.setPoolName("Dominion-" + this.type.name().toLowerCase());
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(Math.min(2, poolSize));
        config.setIdleTimeout(60000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(1800000);

        switch (this.type) {
            case PGSQL -> {
                config.setDriverClassName("org.postgresql.Driver");
                config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
            }
            case SQLITE -> {
                config.setDriverClassName("org.sqlite.JDBC");
                config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/" + name + ".db");
                config.setMaximumPoolSize(1);
                config.setMinimumIdle(1);
                config.addDataSourceProperty("foreign_keys", "true");
            }
            case MYSQL -> {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true");
            }
        }
    }

    public void reconnect() {
        close();
        dataSource = new HikariDataSource(config);
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");
        dsl = DSL.using(dataSource, dialect());
    }

    public void migrate() {
        Flyway.configure(plugin.getClass().getClassLoader())
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .javaMigrations(new V1__LegacySchema(type))
                .load()
                .migrate();
        FlagReconciler.SyncResult result = new FlagReconciler(dsl(), type).reconcile();
        if (result.changedEntries() > 0) {
            plugin.getLogger().info("Reconciled " + result.changedEntries() + " flag columns/values.");
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            reconnect();
        }
        return dataSource.getConnection();
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public DSLContext dsl() {
        if (dsl == null) {
            reconnect();
        }
        return dsl;
    }

    public <T> T transactionResult(org.jooq.TransactionalCallable<T> callable) {
        return dsl().transactionResult(callable);
    }

    public DSLContext dsl(Configuration configuration) {
        return DSL.using(configuration);
    }

    public void close() {
        dsl = null;
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    public DatabaseType getType() {
        return type;
    }

    public SQLDialect dialect() {
        return switch (type) {
            case PGSQL -> SQLDialect.POSTGRES;
            case SQLITE -> SQLDialect.SQLITE;
            case MYSQL -> SQLDialect.MYSQL;
        };
    }
}
