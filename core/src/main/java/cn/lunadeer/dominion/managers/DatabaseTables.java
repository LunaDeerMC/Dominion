package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.databse.DatabaseManager;
import cn.lunadeer.dominion.utils.databse.FIelds.*;
import cn.lunadeer.dominion.utils.databse.syntax.Alter.Alter;
import cn.lunadeer.dominion.utils.databse.syntax.Insert;
import cn.lunadeer.dominion.utils.databse.syntax.Show.Show;
import cn.lunadeer.dominion.utils.databse.syntax.Table.Column;
import cn.lunadeer.dominion.utils.databse.syntax.Table.Create;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static cn.lunadeer.dominion.utils.databse.Backup.exportCsv;
import static cn.lunadeer.dominion.utils.databse.Backup.importCsv;

public class DatabaseTables {
    public static void migrate() throws Exception {
        // player name
        Column player_name_id = Column.of(new FieldInteger("id")).primary().serial().notNull().unique();
        Column player_name_uuid = Column.of(new FieldString("uuid")).notNull().unique();
        Column player_name_last_known_name = Column.of(new FieldString("last_known_name")).notNull().defaultSqlVal("'unknown'");
        Column player_name_last_join_at = Column.of(new FieldTimestamp("last_join_at")).notNull().defaultSqlVal("'1970-01-01 00:00:00'");
        Create.create().table("player_name")
                .column(player_name_id)
                .column(player_name_uuid)
                .column(player_name_last_known_name)
                .column(player_name_last_join_at)
                .execute();


        // dominion table
        Column dominion_id = Column.of(new FieldInteger("id")).primary().serial().notNull().unique();
        Column dominion_owner = Column.of(new FieldString("owner")).notNull().foreign("player_name", new FieldString("uuid"));
        Column dominion_name = Column.of(new FieldString("name")).notNull().defaultSqlVal("'Unnamed'");
        Column dominion_world = Column.of(new FieldString("world")).notNull().defaultSqlVal("'world'");
        Column dominion_x1 = Column.of(new FieldInteger("x1")).notNull().defaultSqlVal("0");
        Column dominion_y1 = Column.of(new FieldInteger("y1")).notNull().defaultSqlVal("0");
        Column dominion_z1 = Column.of(new FieldInteger("z1")).notNull().defaultSqlVal("0");
        Column dominion_x2 = Column.of(new FieldInteger("x2")).notNull().defaultSqlVal("0");
        Column dominion_y2 = Column.of(new FieldInteger("y2")).notNull().defaultSqlVal("0");
        Column dominion_z2 = Column.of(new FieldInteger("z2")).notNull().defaultSqlVal("0");
        Column dominion_parent_dom_id = Column.of(new FieldInteger("parent_dom_id")).notNull().defaultSqlVal("-1").foreign("dominion", new FieldInteger("id"));
        Column dominion_join_message = Column.of(new FieldString("join_message")).notNull().defaultSqlVal("'&3{OWNER}: Welcome to {DOM}!'");
        Column dominion_leave_message = Column.of(new FieldString("leave_message")).notNull().defaultSqlVal("'&3{OWNER}: Leaving {DOM}...'");
        Create.create().table("dominion")
                .column(dominion_id)
                .column(dominion_owner)
                .column(dominion_name)
                .column(dominion_world)
                .column(dominion_x1)
                .column(dominion_y1)
                .column(dominion_z1)
                .column(dominion_x2)
                .column(dominion_y2)
                .column(dominion_z2)
                .column(dominion_parent_dom_id)
                .column(dominion_join_message)
                .column(dominion_leave_message)
                .execute();


        for (Flag flag : Flags.getAllFlags()) {
            Column column = Column.of(new FieldBoolean(flag.getFlagName())).notNull().defaultSqlVal(flag.getDefaultValue().toString());
            Alter.alter().table("dominion").add().column(column).execute();
        }

        // player privilege
        if (!Show.show().tables().execute().contains("dominion_member")) {
            Column player_privilege_id = Column.of(new FieldInteger("id")).primary().serial().notNull().unique();
            Column player_privilege_player_uuid = Column.of(new FieldString("player_uuid")).notNull().foreign("player_name", new FieldString("uuid"));
            Column player_privilege_dom_id = Column.of(new FieldInteger("dom_id")).notNull().foreign("dominion", new FieldInteger("id"));
            Column player_privilege_admin = Column.of(new FieldBoolean("admin")).notNull().defaultSqlVal("false");
            Create.create().table("player_privilege")
                    .column(player_privilege_id)
                    .column(player_privilege_player_uuid)
                    .column(player_privilege_dom_id)
                    .column(player_privilege_admin)
                    .execute();
            for (Flag flag : Flags.getAllPriFlags()) {
                Column column = Column.of(new FieldBoolean(flag.getFlagName())).notNull().defaultSqlVal(flag.getDefaultValue().toString());
                Alter.alter().table("player_privilege").add().column(column).execute();
            }
        }

        // server root player name
        FieldInteger server_player_name_id_field = new FieldInteger("id", -1);
        FieldString server_player_name_uuid_field = new FieldString("uuid", "00000000-0000-0000-0000-000000000000");
        FieldString server_player_name_last_known_name_field = new FieldString("last_known_name", "server");
        Insert.insert().into("player_name")
                .values(server_player_name_id_field,
                        server_player_name_uuid_field,
                        server_player_name_last_known_name_field)
                .onConflict(server_player_name_id_field.getName()).doNothing().execute();

        // server root dominion
        FieldInteger server_dom_id_field = new FieldInteger("id", -1);
        FieldString server_dom_owner_field = new FieldString("owner", "00000000-0000-0000-0000-000000000000");
        FieldString server_dom_name_field = new FieldString("name", "根领地");
        FieldString server_dom_world_field = new FieldString("world", "all");
        if (Show.show().columns().from("dominion").execute().containsKey("world_uid")) {
            server_dom_world_field = new FieldString("world_uid", "00000000-0000-0000-0000-000000000000");
        }
        FieldInteger server_dom_x1_field = new FieldInteger("x1", Integer.MIN_VALUE);
        FieldInteger server_dom_y1_field = new FieldInteger("y1", Integer.MIN_VALUE);
        FieldInteger server_dom_z1_field = new FieldInteger("z1", Integer.MIN_VALUE);
        FieldInteger server_dom_x2_field = new FieldInteger("x2", Integer.MAX_VALUE);
        FieldInteger server_dom_y2_field = new FieldInteger("y2", Integer.MAX_VALUE);
        FieldInteger server_dom_z2_field = new FieldInteger("z2", Integer.MAX_VALUE);
        FieldInteger server_dom_parent_dom_id_field = new FieldInteger("parent_dom_id", -1);
        FieldString server_dom_join_message_field = new FieldString("join_message", "'&3{OWNER}: Welcome to {DOM}!'");
        FieldString server_dom_leave_message_field = new FieldString("leave_message", "'&3{OWNER}: Leaving {DOM}...'");
        Insert.insert().into("dominion")
                .values(server_dom_id_field,
                        server_dom_owner_field,
                        server_dom_name_field,
                        server_dom_world_field,
                        server_dom_x1_field,
                        server_dom_y1_field,
                        server_dom_z1_field,
                        server_dom_x2_field,
                        server_dom_y2_field,
                        server_dom_z2_field,
                        server_dom_parent_dom_id_field,
                        server_dom_join_message_field,
                        server_dom_leave_message_field)
                .onConflict(server_dom_id_field.getName()).doNothing().execute();

        // 1.18.0   dominion add tp_location
        Column dominion_tp_location = Column.of(new FieldString("tp_location")).notNull().defaultSqlVal("'default'");
        Alter.alter().table("dominion").add().column(dominion_tp_location).execute();

        // 1.31.0   add privilege_template
        Column privilege_template_id = Column.of(new FieldInteger("id")).primary().serial().notNull().unique();
        Column privilege_template_creator = Column.of(new FieldString("creator")).notNull().foreign("player_name", new FieldString("uuid"));
        Column privilege_template_name = Column.of(new FieldString("name")).notNull().defaultSqlVal("'Unnamed'");
        Column privilege_template_admin = Column.of(new FieldBoolean("admin")).notNull().defaultSqlVal("false");
        Create.create().table("privilege_template")
                .column(privilege_template_id)
                .column(privilege_template_creator)
                .column(privilege_template_name)
                .column(privilege_template_admin)
                .execute();


        for (Flag flag : Flags.getAllPriFlags()) {
            Column column = Column.of(new FieldBoolean(flag.getFlagName())).notNull().defaultSqlVal(flag.getDefaultValue().toString());
            Alter.alter().table("privilege_template").add().column(column).execute();
        }

        // 1.31.6   dominion add blue-map tile color
        Column dominion_color = Column.of(new FieldString("color")).notNull().defaultSqlVal("'#00BFFF'");
        Alter.alter().table("dominion").add().column(dominion_color).execute();

        // 1.34.0   add dominion_group
        if (!Show.show().tables().execute().contains("dominion_member")) {
            Column player_privilege_group_id = Column.of(new FieldInteger("group_id")).notNull().defaultSqlVal("-1");
            Alter.alter().table("player_privilege").add().column(player_privilege_group_id).execute();
        }

        Column dominion_group_id = Column.of(new FieldInteger("id")).primary().serial().notNull().unique();
        Column dominion_group_dom_id = Column.of(new FieldInteger("dom_id")).notNull().foreign("dominion", new FieldInteger("id"));
        Column dominion_group_name = Column.of(new FieldString("name")).notNull().defaultSqlVal("'Unnamed'");
        Column dominion_group_admin = Column.of(new FieldBoolean("admin")).notNull().defaultSqlVal("false");
        Create.create().table("dominion_group")
                .column(dominion_group_id)
                .column(dominion_group_dom_id)
                .column(dominion_group_name)
                .column(dominion_group_admin)
                .execute();

        for (Flag flag : Flags.getAllPriFlags()) {
            Column column = Column.of(new FieldBoolean(flag.getFlagName())).notNull().defaultSqlVal(flag.getDefaultValue().toString());
            Alter.alter().table("dominion_group").add().column(column).execute();
        }

        // 1.35.0 migrate player_privilege -> dominion_member
        Column dominion_member_id = Column.of(new FieldInteger("id")).primary().serial().notNull().unique();
        Column dominion_member_player_uuid = Column.of(new FieldString("player_uuid")).notNull().foreign("player_name", new FieldString("uuid"));
        Column dominion_member_dom_id = Column.of(new FieldInteger("dom_id")).notNull().foreign("dominion", new FieldInteger("id"));
        Column dominion_member_admin = Column.of(new FieldBoolean("admin")).notNull().defaultSqlVal("false");
        Column dominion_member_group_id = Column.of(new FieldInteger("group_id")).notNull().defaultSqlVal("-1");
        Create.create().table("dominion_member")
                .column(dominion_member_id)
                .column(dominion_member_player_uuid)
                .column(dominion_member_dom_id)
                .column(dominion_member_admin)
                .column(dominion_member_group_id)
                .execute();
        for (Flag flag : Flags.getAllPriFlags()) {
            Column column = Column.of(new FieldBoolean(flag.getFlagName())).notNull().defaultSqlVal(flag.getDefaultValue().toString());
            Alter.alter().table("dominion_member").add().column(column).execute();
        }

        if (Show.show().tables().execute().contains("player_privilege")) {
            String sql = "SELECT * FROM player_privilege;";
            try (Connection conn = DatabaseManager.instance.getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery(sql);
                while (rs.next()) {
                    Field<?>[] fields = new Field<?>[Flags.getAllPriFlags().size() + 4];
                    fields[0] = (new FieldString("player_uuid", rs.getString("player_uuid")));
                    fields[1] = (new FieldInteger("dom_id", rs.getInt("dom_id")));
                    fields[2] = (new FieldInteger("group_id", rs.getInt("group_id")));
                    fields[4] = (new FieldInteger("id", rs.getInt("id")));
                    for (int i = 0; i < Flags.getAllPriFlags().size(); i++) {
                        fields[i + 4] = new FieldBoolean(Flags.getAllPriFlags().get(i).getFlagName(), rs.getBoolean(Flags.getAllPriFlags().get(i).getFlagName()));
                    }
                    Insert.insert().into("dominion_member").values(fields).onConflict("id").doNothing().execute();
                }
                sql = "DROP TABLE player_privilege;";
                conn.createStatement().execute(sql);
            } catch (Exception ignored) {
            }
        }

        // 2.1.0-beta add group name colored
        if (!Show.show().columns().from("dominion_group").execute().containsKey("name_colored")) {
            Column dominion_group_name_colored = Column.of(new FieldString("name_colored")).notNull().defaultSqlVal("'Unnamed'");
            Alter.alter().table("dominion_group").add().column(dominion_group_name_colored).execute();
            String copy_sql = "UPDATE dominion_group SET name_colored = name;";
            try (Connection conn = DatabaseManager.instance.getConnection()) {
                conn.createStatement().executeUpdate(copy_sql);
            } catch (Exception ignored) {
            }

            Column player_name_using_group_title_id = Column.of(new FieldInteger("using_group_title_id")).notNull().defaultSqlVal("-1");
            Alter.alter().table("player_name").add().column(player_name_using_group_title_id).execute();
        }


        // 2.3.0 change world name to world uid
        if (!Show.show().columns().from("dominion").execute().containsKey("world_uid")) {
            Column dominion_world_uid = Column.of(new FieldString("world_uid")).notNull().defaultSqlVal("'00000000-0000-0000-0000-000000000000'");
            Alter.alter().table("dominion").add().column(dominion_world_uid).execute();
            try (Connection conn = DatabaseManager.instance.getConnection()) {
                String sql = "SELECT * FROM dominion;";
                ResultSet rs = conn.createStatement().executeQuery(sql);
                while (rs.next()) {
                    String world_name = rs.getString("world");
                    String world_uid = Dominion.instance.getServer().getWorld(world_name).getUID().toString();
                    sql = String.format("UPDATE dominion SET world_uid = '%s' WHERE world = '%s';", world_uid, world_name);
                    conn.createStatement().executeUpdate(sql);
                }
                sql = "UPDATE dominion SET world_uid = '00000000-0000-0000-0000-000000000000' WHERE world = 'all';";
                conn.createStatement().executeUpdate(sql);
            } catch (Exception ignored) {
            }
            Alter.alter().table("dominion").drop().column(new FieldString("world")).execute();
        }

        // 4.0.0-alpha add serverId to dominion
        if (!Show.show().columns().from("dominion").execute().containsKey("server_id")) {
            Column dominion_server_id = Column.of(new FieldInteger("server_id")).notNull().defaultSqlVal(String.valueOf(Configuration.multiServer.serverId));
            Alter.alter().table("dominion").add().column(dominion_server_id).execute();
            try (Connection conn = DatabaseManager.instance.getConnection()) {
                String sql = "UPDATE dominion SET server_id = -1 WHERE id = -1;";   // server root dominion's server id is -1
                conn.createStatement().executeUpdate(sql);
            } catch (Exception ignored) {
            }
        }

        // 4.0.0-alpha remove player_damage because of impl pvp
        if (Show.show().columns().from("dominion").execute().containsKey("player_damage")) {
            Alter.alter().table("dominion").drop().column(new FieldString("player_damage")).execute();
            Alter.alter().table("dominion_member").drop().column(new FieldString("player_damage")).execute();
            Alter.alter().table("dominion_group").drop().column(new FieldString("player_damage")).execute();
            Alter.alter().table("privilege_template").drop().column(new FieldString("player_damage")).execute();
        }

        // 4.4.0-beta add skin url cache for cui
        if (!Show.show().columns().from("player_name").execute().containsKey("skin_url")) {
            Column skin_url = Column.of(new FieldString("skin_url")).notNull()
                    .defaultSqlVal("'http://textures.minecraft.net/texture/613ba1403f98221fab6f4ae0f9e5298068262258966e8f9e53cdedd97aa45ef1'");
            Alter.alter().table("player_name").add().column(skin_url).execute();
        }
        if (!Show.show().columns().from("player_name").execute().containsKey("ui_preference")) {
            Column ui_preference = Column.of(new FieldString("ui_preference")).notNull()
                    .defaultSqlVal("'TUI'");
            Alter.alter().table("player_name").add().column(ui_preference).execute();

            try (Connection conn = DatabaseManager.instance.getConnection()) {
                // update ui_preference to CUI if player_name UUID starts with 00000000
                String sql = "UPDATE player_name SET ui_preference = 'CUI' WHERE uuid LIKE '00000000%';";
                conn.createStatement().executeUpdate(sql);
            } catch (Exception ignored) {
            }
        }

        // 4. add monster_spawn_settings for detailed spawn reason control
        if (!Show.show().columns().from("dominion").execute().containsKey("monster_spawn_settings")) {
            Column monster_spawn_settings = Column.of(new FieldString("monster_spawn_settings")).notNull()
                    .defaultSqlVal("''");
            Alter.alter().table("dominion").add().column(monster_spawn_settings).execute();
        }

        // 4.4.0 add multiserver support
        if (!Show.show().tables().execute().contains("server_info")) {
            Column server_info_id = Column.of(new FieldInteger("id")).primary().serial().notNull().unique();
            Column server_info_name = Column.of(new FieldString("name")).notNull().defaultSqlVal("'Unnamed'");
            Create.create().table("server_info")
                    .column(server_info_id)
                    .column(server_info_name)
                    .execute();
        }
        if (!Show.show().tables().execute().contains("tp_cache")) {
            Column tp_cache_uuid = Column.of(new FieldString("uuid")).primary().notNull();
            Column tp_cache_dom_id = Column.of(new FieldInteger("dom_id")).notNull();
            Create.create().table("tp_cache")
                    .column(tp_cache_uuid)
                    .column(tp_cache_dom_id)
                    .execute();
        }
    }

    public static class DatabaseManagerText extends ConfigurationPart {
        public String exportingDatabaseTables = "Exporting database tables...";
        public String exportTableFail = "Export table failed, reason: {0}";
        public String exportWorldMappingFail = "Export world uid mapping failed, reason: {0}";
        public String exportDatabaseSuccess = "Export database to {0} successfully.";

        public String fileNotFound = "Database table file path {0} not found.";
        public String importingDatabase = "Importing database...";
        public String fileCorrupted = "Some database table file is missing, please re-export the database tables.";
        public String convertWorldFailed = "The old world {0}({1}) is unable to find in current save, please make sure the world name is exist in current save.";
        public String importDatabaseFail = "Import database failed, reason: {0}";
        public String importDatabaseSuccess = "Import database successfully.";
    }

    private static final File export_path = new File(Dominion.instance.getDataFolder(), "backup");

    public static void exportTables(CommandSender sender) {
        Scheduler.runTaskAsync(() -> {
            Notification.info(sender, Language.databaseManagerText.exportingDatabaseTables);
            if (!export_path.exists()) {
                boolean re = export_path.mkdirs();
            }
            try {
                exportCsv("player_name", new File(export_path, "player_name.csv"), "id");
                exportCsv("privilege_template", new File(export_path, "privilege_template.csv"), "id");
                exportCsv("dominion", new File(export_path, "dominion.csv"), "id");
                exportCsv("dominion_group", new File(export_path, "dominion_group.csv"), "id");
                exportCsv("dominion_member", new File(export_path, "dominion_member.csv"), "id");
            } catch (Exception e) {
                Notification.error(sender, Language.databaseManagerText.exportTableFail, e.getMessage());
                return;
            }
            try {
                Map<String, String> world_uid_map = Dominion.instance.getServer().getWorlds().stream().collect(HashMap::new, (m, w) -> m.put(w.getName(), w.getUID().toString()), HashMap::putAll);
                YamlConfiguration world_uid = new YamlConfiguration();
                for (Map.Entry<String, String> entry : world_uid_map.entrySet()) {
                    world_uid.set(entry.getKey(), entry.getValue());
                }
                world_uid.save(new File(export_path, "world_uid_mapping.yml"));
            } catch (Exception e) {
                Notification.error(sender, Language.databaseManagerText.exportWorldMappingFail, e.getMessage());
                return;
            }
            Notification.info(sender, Language.databaseManagerText.exportDatabaseSuccess, export_path.getAbsolutePath());
        });
    }

    public static void importTables(CommandSender sender) {
        Scheduler.runTaskAsync(() -> {
            if (!export_path.exists()) {
                Notification.error(sender, Language.databaseManagerText.fileNotFound, export_path.getAbsolutePath());
                return;
            }
            Notification.info(sender, Language.databaseManagerText.importingDatabase);
            Map<String, String> world_uid_map = Dominion.instance.getServer().getWorlds().stream().collect(HashMap::new, (m, w) -> m.put(w.getName(), w.getUID().toString()), HashMap::putAll);
            File player_name_csv = new File(export_path, "player_name.csv");
            File privilege_template_csv = new File(export_path, "privilege_template.csv");
            File dominion_csv = new File(export_path, "dominion.csv");
            File world_uid_mapping = new File(export_path, "world_uid_mapping.yml");
            File dominion_group_csv = new File(export_path, "dominion_group.csv");
            File dominion_member_csv = new File(export_path, "dominion_member.csv");
            if (!player_name_csv.exists() || !privilege_template_csv.exists() || !dominion_csv.exists() || !world_uid_mapping.exists() || !dominion_group_csv.exists() || !dominion_member_csv.exists()) {
                Notification.error(sender, Language.databaseManagerText.fileCorrupted);
                return;
            }
            try {
                importCsv("player_name", player_name_csv, "id");
                importCsv("privilege_template", privilege_template_csv, "id");
                importCsv("dominion", dominion_csv, "id");
                importCsv("dominion_group", dominion_group_csv, "id");
                importCsv("dominion_member", dominion_member_csv, "id");

                YamlConfiguration world_uid = YamlConfiguration.loadConfiguration(world_uid_mapping);
                for (String key : world_uid.getKeys(false)) {
                    if (world_uid_map.containsKey(key)) {
                        String old_uid = world_uid.getString(key);
                        World newWorld = Dominion.instance.getServer().getWorld(key);
                        if (newWorld == null) {
                            Notification.warn(sender, Language.databaseManagerText.convertWorldFailed, key, old_uid);
                            continue;
                        }
                        String new_uid = newWorld.getUID().toString();
                        if (new_uid == null) {
                            Notification.warn(sender, Language.databaseManagerText.convertWorldFailed, key, old_uid);
                            continue;
                        }
                        String sql = String.format("UPDATE dominion SET world_uid = '%s' WHERE world_uid = '%s';", new_uid, old_uid);
                        try (Connection conn = DatabaseManager.instance.getConnection()) {
                            conn.createStatement().executeUpdate(sql);
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                Notification.error(sender, Language.databaseManagerText.importDatabaseFail, e.getMessage());
                XLogger.error(e);
                return;
            }
            Notification.info(sender, Language.databaseManagerText.importDatabaseSuccess);
            CacheManager.instance.reloadCache();
        });
    }
}
