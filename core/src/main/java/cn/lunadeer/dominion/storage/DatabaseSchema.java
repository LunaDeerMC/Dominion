package cn.lunadeer.dominion.storage;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

import java.sql.Timestamp;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

public final class DatabaseSchema {
    private DatabaseSchema() {
    }

    public static final Table<Record> PLAYER_NAME = table(name("player_name"));
    public static final Field<Integer> PLAYER_ID = intField("id");
    public static final Field<String> PLAYER_UUID = strField("uuid");
    public static final Field<String> PLAYER_LAST_KNOWN_NAME = strField("last_known_name");
    public static final Field<Timestamp> PLAYER_LAST_JOIN_AT = field(name("last_join_at"), Timestamp.class);
    public static final Field<Integer> PLAYER_USING_GROUP_TITLE_ID = intField("using_group_title_id");
    public static final Field<String> PLAYER_SKIN_URL = strField("skin_url");
    public static final Field<String> PLAYER_UI_PREFERENCE = strField("ui_preference");

    public static final Table<Record> DOMINION = table(name("dominion"));
    public static final Field<Integer> DOM_ID = intField("id");
    public static final Field<String> DOM_OWNER = strField("owner");
    public static final Field<String> DOM_NAME = strField("name");
    public static final Field<String> DOM_WORLD_UID = strField("world_uid");
    public static final Field<Integer> DOM_X1 = intField("x1");
    public static final Field<Integer> DOM_Y1 = intField("y1");
    public static final Field<Integer> DOM_Z1 = intField("z1");
    public static final Field<Integer> DOM_X2 = intField("x2");
    public static final Field<Integer> DOM_Y2 = intField("y2");
    public static final Field<Integer> DOM_Z2 = intField("z2");
    public static final Field<Integer> DOM_PARENT_DOM_ID = intField("parent_dom_id");
    public static final Field<String> DOM_JOIN_MESSAGE = strField("join_message");
    public static final Field<String> DOM_LEAVE_MESSAGE = strField("leave_message");
    public static final Field<String> DOM_TP_LOCATION = strField("tp_location");
    public static final Field<String> DOM_COLOR = strField("color");
    public static final Field<Integer> DOM_SERVER_ID = intField("server_id");
    public static final Field<Boolean> DOM_OWNER_GLOW = boolField("owner_glow");

    public static final Table<Record> MEMBER = table(name("dominion_member"));
    public static final Field<Integer> MEMBER_ID = intField("id");
    public static final Field<String> MEMBER_PLAYER_UUID = strField("player_uuid");
    public static final Field<Integer> MEMBER_DOM_ID = intField("dom_id");
    public static final Field<Integer> MEMBER_GROUP_ID = intField("group_id");

    public static final Table<Record> GROUP = table(name("dominion_group"));
    public static final Field<Integer> GROUP_ID = intField("id");
    public static final Field<Integer> GROUP_DOM_ID = intField("dom_id");
    public static final Field<String> GROUP_NAME = strField("name");
    public static final Field<String> GROUP_NAME_COLORED = strField("name_colored");

    public static final Table<Record> TEMPLATE = table(name("privilege_template"));
    public static final Field<Integer> TEMPLATE_ID = intField("id");
    public static final Field<String> TEMPLATE_CREATOR = strField("creator");
    public static final Field<String> TEMPLATE_NAME = strField("name");

    public static final Table<Record> SERVER_INFO = table(name("server_info"));
    public static final Field<Integer> SERVER_ID = intField("id");
    public static final Field<String> SERVER_NAME = strField("name");

    public static final Table<Record> TP_CACHE = table(name("tp_cache"));
    public static final Field<String> TP_UUID = strField("uuid");
    public static final Field<Integer> TP_DOM_ID = intField("dom_id");

    public static Field<Integer> intField(String name) {
        return field(name(name), Integer.class);
    }

    public static Field<String> strField(String name) {
        return field(name(name), String.class);
    }

    public static Field<Boolean> boolField(String name) {
        return field(name(name), Boolean.class);
    }

    public static Field<Object> objectField(String name) {
        return field(name(name), SQLDataType.OTHER);
    }

    public static Field<Object> objectField(Name name) {
        return field(name, SQLDataType.OTHER);
    }
}
