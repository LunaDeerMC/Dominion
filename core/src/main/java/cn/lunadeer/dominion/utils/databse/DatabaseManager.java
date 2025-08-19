package cn.lunadeer.dominion.utils.databse;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    public static DatabaseManager instance;

    private final JavaPlugin plugin;
    private DatabaseType type;
    private final HikariConfig config = new HikariConfig();
    private DataSource ds;

    /**
     * Constructs a new DatabaseManager instance with the provided parameters.
     *
     * @param plugin the JavaPlugin instance
     * @param type   the type of the database (e.g., PGSQL, MYSQL, SQLITE)
     * @param host   the database host
     * @param port   the database port
     * @param name   the database name
     * @param user   the database user
     * @param pass   the database password
     */
    public DatabaseManager(JavaPlugin plugin, String type, String host, String port, String name, String user, String pass) throws IllegalArgumentException {
        instance = this;
        this.plugin = plugin;
        set(type, host, port, name, user, pass, 10);
    }

    /**
     * Sets the database configuration parameters.
     *
     * @param type the type of the database (e.g., PGSQL, MYSQL, SQLITE)
     * @param host the database host
     * @param port the database port
     * @param name the database name
     * @param user the database user
     * @param pass the database password
     */
    public void set(String type, String host, String port, String name, String user, String pass, int poolSize) throws IllegalArgumentException {
        try {
            this.type = DatabaseType.valueOf(type.toUpperCase());
            if (this.type.equals(DatabaseType.PGSQL)) {
                config.setDriverClassName("org.postgresql.Driver");
                config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
            } else if (this.type.equals(DatabaseType.SQLITE)) {
                config.setDriverClassName("org.sqlite.JDBC");
                config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/" + name + ".db");
            } else if (this.type.equals(DatabaseType.MYSQL)) {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name);
            }
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(2);
            config.setIdleTimeout(60000);
            config.setConnectionTimeout(30000);
            config.setMaxLifetime(1800000);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported database type: " + type, e);
        }
    }

    public void reconnect() {
        if (ds != null) {
            close();
        }
        this.ds = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        if (ds == null) {
            reconnect();
        }
        return ds.getConnection();
    }

    /**
     * Closes the database connection if it is open.
     */
    public void close() {
        this.ds = null;
    }

    /**
     * Returns the type of the database.
     *
     * @return the type of the database
     */
    public DatabaseType getType() {
        return type;
    }
}
