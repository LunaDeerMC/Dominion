package cn.lunadeer.dominion.storage;

import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CushionFlagMigrationTest {
    @TempDir Path directory;
    @Test void upgradeDefaultsAreDeniedAcrossClaimsMembersGroupsAndTemplatesAndSurviveReconciliation() throws Exception {
        var ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+directory.resolve("cushions.db"));
        List<Flag> cushion=List.of(Flags.CUSHION_PLACE,Flags.CUSHION_BREAK,
                Flags.CUSHION_MOB_DAMAGE,Flags.CUSHION_ENVIRONMENT_BREAK);
        List<String> tables=List.of("dominion","dominion_member","dominion_group","privilege_template");
        try(var connection=ds.getConnection();var statement=connection.createStatement()) {
            for(String table:tables) {
                List<? extends Flag> flags=table.equals("dominion")?Flags.getActiveFlags():Flags.getActivePriFlags();
                StringBuilder sql=new StringBuilder("CREATE TABLE "+table+" (id INTEGER PRIMARY KEY");
                for(Flag flag:flags)if(!cushion.contains(flag))sql.append(", ").append(flag.getFlagName()).append(" BOOLEAN NOT NULL DEFAULT true");
                statement.execute(sql+")");statement.execute("INSERT INTO "+table+" (id) VALUES (1)");
            }
        }
        var reconciler=new FlagReconciler(ds,DatabaseType.SQLITE);
        assertEquals(10,reconciler.reconcile().changedEntries());
        try(var connection=ds.getConnection();var statement=connection.createStatement()) {
            for(String table:tables) {
                var flags=table.equals("dominion")?cushion:cushion.subList(0,2);
                for(Flag flag:flags) {
                    try(var result=statement.executeQuery("SELECT "+flag.getFlagName()+" FROM "+table+" WHERE id=1")) {
                        assertTrue(result.next());assertFalse(result.getBoolean(1),table+" "+flag.getFlagName());
                    }
                    statement.execute("UPDATE "+table+" SET "+flag.getFlagName()+"=true WHERE id=1");
                }
            }
        }
        assertEquals(0,reconciler.reconcile().changedEntries());
        try(var connection=ds.getConnection();var statement=connection.createStatement()) {
            for(String table:tables)for(Flag flag:table.equals("dominion")?cushion:cushion.subList(0,2)) {
                try(var result=statement.executeQuery("SELECT "+flag.getFlagName()+" FROM "+table+" WHERE id=1")) {
                    assertTrue(result.next());assertTrue(result.getBoolean(1));
                }
            }
        }
    }
}
