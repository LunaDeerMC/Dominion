package cn.lunadeer.dominion.storage;

import java.util.Set;
import java.util.regex.Pattern;

public final class DatabaseSchema {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private DatabaseSchema() {
    }

    public static final String PLAYER_NAME = "player_name";
    public static final String PLAYER_ID = "id";
    public static final String PLAYER_UUID = "uuid";
    public static final String PLAYER_LAST_KNOWN_NAME = "last_known_name";
    public static final String PLAYER_LAST_JOIN_AT = "last_join_at";
    public static final String PLAYER_USING_GROUP_TITLE_ID = "using_group_title_id";
    public static final String PLAYER_SKIN_URL = "skin_url";
    public static final String PLAYER_UI_PREFERENCE = "ui_preference";

    public static final String DOMINION = "dominion";
    public static final String DOM_ID = "id";
    public static final String DOM_OWNER = "owner";
    public static final String DOM_NAME = "name";
    public static final String DOM_WORLD_UID = "world_uid";
    public static final String DOM_X1 = "x1";
    public static final String DOM_Y1 = "y1";
    public static final String DOM_Z1 = "z1";
    public static final String DOM_X2 = "x2";
    public static final String DOM_Y2 = "y2";
    public static final String DOM_Z2 = "z2";
    public static final String DOM_PARENT_DOM_ID = "parent_dom_id";
    public static final String DOM_JOIN_MESSAGE = "join_message";
    public static final String DOM_LEAVE_MESSAGE = "leave_message";
    public static final String DOM_TP_LOCATION = "tp_location";
    public static final String DOM_COLOR = "color";
    public static final String DOM_SERVER_ID = "server_id";
    public static final String DOM_OWNER_GLOW = "owner_glow";

    public static final String MEMBER = "dominion_member";
    public static final String MEMBER_ID = "id";
    public static final String MEMBER_PLAYER_UUID = "player_uuid";
    public static final String MEMBER_DOM_ID = "dom_id";
    public static final String MEMBER_GROUP_ID = "group_id";

    public static final String GROUP = "dominion_group";
    public static final String GROUP_ID = "id";
    public static final String GROUP_DOM_ID = "dom_id";
    public static final String GROUP_NAME = "name";
    public static final String GROUP_NAME_COLORED = "name_colored";

    public static final String TEMPLATE = "privilege_template";
    public static final String TEMPLATE_ID = "id";
    public static final String TEMPLATE_CREATOR = "creator";
    public static final String TEMPLATE_NAME = "name";

    public static final String SERVER_INFO = "server_info";
    public static final String SERVER_ID = "id";
    public static final String SERVER_NAME = "name";

    public static final String TP_CACHE = "tp_cache";
    public static final String TP_UUID = "uuid";
    public static final String TP_DOM_ID = "dom_id";

    public static final Set<String> TABLES = Set.of(
            PLAYER_NAME,
            DOMINION,
            MEMBER,
            GROUP,
            TEMPLATE,
            SERVER_INFO,
            TP_CACHE
    );

    public static String identifier(String identifier) {
        if (identifier == null || !IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid database identifier: " + identifier);
        }
        return identifier;
    }

    public static String table(String table) {
        if (!TABLES.contains(table)) {
            throw new IllegalArgumentException("Unknown database table: " + table);
        }
        return identifier(table);
    }
}
