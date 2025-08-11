package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.databse.FIelds.Field;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldInteger;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldString;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldTimestamp;
import cn.lunadeer.dominion.utils.databse.syntax.Delete;
import cn.lunadeer.dominion.utils.databse.syntax.Insert;
import cn.lunadeer.dominion.utils.databse.syntax.Select;
import cn.lunadeer.dominion.utils.databse.syntax.Update;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerDOO implements PlayerDTO {

    private final FieldInteger id = new FieldInteger("id");
    private final FieldString uuid = new FieldString("uuid");
    private final FieldString lastKnownName = new FieldString("last_known_name");
    private final FieldTimestamp lastJoinAt = new FieldTimestamp("last_join_at");
    private final FieldInteger using_group_title_id = new FieldInteger("using_group_title_id");
    private final FieldString skinUrl = new FieldString("skin_url");
    private final FieldString ui_preference = new FieldString("ui_preference");

    private static Field<?>[] fields() {
        return new Field<?>[]{
                new FieldInteger("id"),
                new FieldString("uuid"),
                new FieldString("last_known_name"),
                new FieldTimestamp("last_join_at"),
                new FieldInteger("using_group_title_id"),
                new FieldString("skin_url"),
                new FieldString("ui_preference")
        };
    }

    private static PlayerDOO parse(Map<String, Field<?>> map) {
        return new PlayerDOO(
                (Integer) map.get("id").getValue(),
                UUID.fromString((String) map.get("uuid").getValue()),
                (String) map.get("last_known_name").getValue(),
                ((Timestamp) (map.get("last_join_at").getValue())).toLocalDateTime(),
                (Integer) map.get("using_group_title_id").getValue(),
                (String) map.get("skin_url").getValue(),
                (String) map.get("ui_preference").getValue()
        );
    }

    public static List<PlayerDTO> all() throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("player_name")
                .where("id > 0")
                .execute();
        return res.stream().map(PlayerDOO::parse).collect(Collectors.toList());
    }

    public static PlayerDOO selectById(Integer id) throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("player_name")
                .where("id = ?", id)
                .execute();
        if (res.isEmpty()) return null;
        return parse(res.get(0));
    }

    public static void delete(PlayerDOO player) throws SQLException {
        Delete.delete().from("player_name").where("id = ?", player.getId()).execute();
        CacheManager.instance.getPlayerCache().delete(player.getId());
    }

    public static PlayerDOO create(Player player) throws SQLException {
        return create(player.getUniqueId(), player.getName());
    }

    public static PlayerDOO create(UUID playerUid, String playerName) throws SQLException {
        FieldString uuid = new FieldString("uuid", playerUid.toString());
        FieldString lastKnownName = new FieldString("last_known_name", playerName);
        FieldTimestamp lastJoinAt = new FieldTimestamp("last_join_at", Timestamp.valueOf(LocalDateTime.now()));
        FieldString uiPreference = new FieldString("ui_preference", Configuration.defaultUiType);
        Map<String, Field<?>> p;
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("player_name")
                .where("uuid = ?", uuid.getValue())
                .execute();
        if (res.isEmpty()) {
            if (playerUid.toString().startsWith("00000000") && uiPreference.getValue().equals(UI_TYPE.TUI.name())) {
                // If the UUID starts with "00000000", it's a bedrock player, so we set CUI as default
                // this is a workaround for bedrock players who cannot use TUI.
                uiPreference.setValue(UI_TYPE.CUI.name());
            }
            p = Insert.insert().into("player_name")
                    .values(uuid, lastKnownName, lastJoinAt, uiPreference)
                    .returning(fields())
                    .execute();
            if (p.isEmpty()) {
                throw new SQLException("Create player failed");
            }
        } else {
            p = res.get(0);
            Update.update("player_name")
                    .set(lastKnownName, lastJoinAt)
                    .where("uuid = ?", uuid.getValue())
                    .execute();
        }
        PlayerDOO player = parse(p);
        CacheManager.instance.getPlayerCache().load(player.getId());
        return player;
    }

    private PlayerDOO(Integer id, UUID uuid, String lastKnownName, LocalDateTime lastJoinAt, Integer using_group_title_id, String skinUrl, String uiPreference) {
        this.id.setValue(id);
        this.uuid.setValue(uuid.toString());
        this.lastKnownName.setValue(lastKnownName);
        this.lastJoinAt.setValue(Timestamp.valueOf(lastJoinAt));
        this.using_group_title_id.setValue(using_group_title_id);
        this.skinUrl.setValue(skinUrl);
        this.ui_preference.setValue(uiPreference);
    }

    @Override
    public Integer getId() {
        return id.getValue();
    }

    @Override
    public UUID getUuid() {
        return UUID.fromString(uuid.getValue());
    }

    @Override
    public String getLastKnownName() {
        return lastKnownName.getValue();
    }

    @Override
    public PlayerDTO updateLastKnownName(@NotNull String name, @Nullable URL skinUrl) throws SQLException, MalformedURLException {
        this.setLastKnownName(name);
        if (skinUrl == null) {
            // Default skin URL if none provided
            skinUrl = new URL("http://textures.minecraft.net/texture/613ba1403f98221fab6f4ae0f9e5298068262258966e8f9e53cdedd97aa45ef1");
        }
        this.setSkinUrl(skinUrl);
        this.setLastJoinAt(LocalDateTime.now());
        Update.update("player_name")
                .set(this.lastKnownName, this.skinUrl, this.lastJoinAt)
                .where("uuid = ?", this.getUuid().toString())
                .execute();
        CacheManager.instance.getPlayerCache().load(this.getId());
        return this;
    }

    public Long getLastJoinAt() {
        return lastJoinAt.getValue().getTime();
    }

    public void setId(Integer id) {
        this.id.setValue(id);
    }

    public void setUuid(UUID uuid) {
        this.uuid.setValue(uuid.toString());
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName.setValue(lastKnownName);
    }

    public void setSkinUrl(@Nullable URL skinUrl) {
        if (skinUrl == null) {
            return;
        }
        this.skinUrl.setValue(skinUrl.toString());
    }

    public void setLastJoinAt(LocalDateTime lastJoinAt) {
        this.lastJoinAt.setValue(Timestamp.valueOf(lastJoinAt));
    }

    @Override
    public void setUiPreference(UI_TYPE uiType) throws SQLException {
        this.ui_preference.setValue(uiType.name());
        Update.update("player_name")
                .set(this.ui_preference)
                .where("uuid = ?", this.getUuid().toString())
                .execute();
    }

    @Override
    public Integer getUsingGroupTitleID() {
        return using_group_title_id.getValue();
    }

    @Override
    public @NotNull URL getSkinUrl() throws MalformedURLException {
        String skinUrlValue = skinUrl.getValue();
        if (skinUrlValue == null || skinUrlValue.isEmpty()) {
            return new URL("http://textures.minecraft.net/texture/613ba1403f98221fab6f4ae0f9e5298068262258966e8f9e53cdedd97aa45ef1");
        }
        return new URL(skinUrlValue);
    }

    @Override
    public UI_TYPE getUiPreference() {
        String uiPreferenceValue = ui_preference.getValue();
        if (uiPreferenceValue == null || uiPreferenceValue.isEmpty()) {
            return UI_TYPE.AUTO; // Default to TUI if not set
        }
        return UI_TYPE.valueOf(uiPreferenceValue);
    }

    public void setUsingGroupTitleID(Integer usingGroupTitleID) throws SQLException {
        this.using_group_title_id.setValue(usingGroupTitleID);
        Update.update("player_name")
                .set(this.using_group_title_id)
                .where("id = ?", this.getId())
                .execute();
        CacheManager.instance.getPlayerCache().load(this.getId());
    }
}
