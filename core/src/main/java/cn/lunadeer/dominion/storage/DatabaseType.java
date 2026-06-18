package cn.lunadeer.dominion.storage;

public enum DatabaseType {
    PGSQL, SQLITE, MYSQL, MARIADB;

    public boolean isMySqlFamily() {
        return this == MYSQL || this == MARIADB;
    }
}
