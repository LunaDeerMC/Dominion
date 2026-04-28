package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.storage.repository.PlayerRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerDOO implements PlayerDTO {

    private Integer id;
    private UUID uuid;
    private String lastKnownName;
    private LocalDateTime lastJoinAt;
    private Integer using_group_title_id;
    private String skinUrl;
    private String ui_preference;

    private static PlayerDOO parse(PlayerRepository.PlayerRow row) {
        if (row == null) return null;
        return new PlayerDOO(
                row.id(),
                row.uuid(),
                row.lastKnownName(),
                row.lastJoinAt(),
                row.usingGroupTitleId(),
                row.skinUrl(),
                row.uiPreference()
        );
    }

    public static List<PlayerDTO> all() throws SQLException {
        return PlayerRepository.all().stream().map(PlayerDOO::parse).collect(Collectors.toList());
    }

    public static PlayerDOO selectById(Integer id) throws SQLException {
        return parse(PlayerRepository.selectById(id));
    }

    public static void delete(PlayerDOO player) throws SQLException {
        PlayerRepository.delete(player.getId());
        CacheManager.instance.getPlayerCache().delete(player.getId());
    }

    public static PlayerDOO create(Player player) throws SQLException {
        return create(player.getUniqueId(), player.getName());
    }

    public static PlayerDOO create(UUID playerUid, String playerName) throws SQLException {
        PlayerDOO player = parse(PlayerRepository.createOrUpdate(playerUid, playerName));
        if (player == null) throw new SQLException("Create player failed");
        CacheManager.instance.getPlayerCache().load(player.getId());
        return player;
    }

    private PlayerDOO(Integer id, UUID uuid, String lastKnownName, LocalDateTime lastJoinAt, Integer using_group_title_id, String skinUrl, String uiPreference) {
        this.id = id;
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.lastJoinAt = lastJoinAt;
        this.using_group_title_id = using_group_title_id;
        this.skinUrl = skinUrl;
        this.ui_preference = uiPreference;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getLastKnownName() {
        return lastKnownName;
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
        PlayerRepository.updateProfile(this.getUuid(), this.lastKnownName, this.skinUrl, this.lastJoinAt);
        CacheManager.instance.getPlayerCache().load(this.getId());
        return this;
    }

    public Long getLastJoinAt() {
        return java.sql.Timestamp.valueOf(lastJoinAt).getTime();
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public void setSkinUrl(@Nullable URL skinUrl) {
        if (skinUrl == null) {
            return;
        }
        this.skinUrl = skinUrl.toString();
    }

    public void setLastJoinAt(LocalDateTime lastJoinAt) {
        this.lastJoinAt = lastJoinAt;
    }

    @Override
    public void setUiPreference(UI_TYPE uiType) throws SQLException {
        this.ui_preference = uiType.name();
        PlayerRepository.updateUiPreference(this.getUuid(), this.ui_preference);
    }

    @Override
    public Integer getUsingGroupTitleID() {
        return using_group_title_id;
    }

    @Override
    public @NotNull URL getSkinUrl() throws MalformedURLException {
        String skinUrlValue = skinUrl;
        if (skinUrlValue == null || skinUrlValue.isEmpty()) {
            return new URL("http://textures.minecraft.net/texture/613ba1403f98221fab6f4ae0f9e5298068262258966e8f9e53cdedd97aa45ef1");
        }
        return new URL(skinUrlValue);
    }

    @Override
    public @NotNull UI_TYPE getUiPreference() {
        String uiPreferenceValue = ui_preference;
        if (uiPreferenceValue == null || uiPreferenceValue.isEmpty()) {
            return UI_TYPE.TUI; // Default to TUI if not set
        }
        try {
            UI_TYPE type = UI_TYPE.valueOf(uiPreferenceValue);
            if (type.equals(UI_TYPE.BY_PLAYER)) {
                setUiPreference(UI_TYPE.CUI); // Convert BY_PLAYER to CUI
                return getUiPreference(); // Re-fetch after conversion
            }
            return type;
        } catch (Exception e) {
            try {
                setUiPreference(UI_TYPE.CUI);
            } // Convert BY_PLAYER to CUI
            catch (SQLException ignored) {
            }
            return UI_TYPE.CUI; // Fallback to CUI if the value is invalid
        }
    }

    public void setUsingGroupTitleID(Integer usingGroupTitleID) throws SQLException {
        this.using_group_title_id = usingGroupTitleID;
        PlayerRepository.updateUsingGroupTitle(this.getId(), usingGroupTitleID);
        CacheManager.instance.getPlayerCache().load(this.getId());
    }
}
