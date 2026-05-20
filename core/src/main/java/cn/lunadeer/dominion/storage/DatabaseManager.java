package cn.lunadeer.dominion.storage;

import cn.lunadeer.dominion.storage.mapper.GenericMapper;
import cn.lunadeer.dominion.storage.migration.V1__LegacySchema;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;
import org.flywaydb.core.Flyway;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    public static DatabaseManager instance;

    private final JavaPlugin plugin;
    private final HikariConfig config = new HikariConfig();
    private DatabaseType type;
    private HikariDataSource dataSource;
    private SqlSessionFactory sqlSessionFactory;

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
            case MARIADB -> {
                config.setDriverClassName("org.mariadb.jdbc.Driver");
                config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + name + "?useUnicode=true&characterEncoding=utf8&useSSL=false");
            }
        }
    }

    public void reconnect() {
        close();
        dataSource = new HikariDataSource(config);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setEnvironment(new Environment("Dominion", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(GenericMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    public void migrate() {
        Flyway.configure(plugin.getClass().getClassLoader())
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .javaMigrations(new V1__LegacySchema(type))
                .load()
                .migrate();
        FlagReconciler.SyncResult result = new FlagReconciler(dataSource(), type).reconcile();
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
        if (dataSource == null) {
            reconnect();
        }
        return dataSource;
    }

    public SqlSessionFactory sqlSessionFactory() {
        if (sqlSessionFactory == null) {
            reconnect();
        }
        return sqlSessionFactory;
    }

    public SqlSession openSession() {
        return sqlSessionFactory().openSession(false);
    }

    public void close() {
        sqlSessionFactory = null;
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    public DatabaseType getType() {
        return type;
    }
}
