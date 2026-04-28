package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.storage.repository.MemberRepository;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

public class MemberDOO implements MemberDTO {

    private Integer id;
    private UUID playerUUID;
    private Integer domID;
    private Integer groupId;
    private final Map<PriFlag, Boolean> flags = new HashMap<>();

    private static MemberDOO parse(MemberRepository.MemberRow row) {
        if (row == null) return null;
        return new MemberDOO(row.id(), row.playerUUID(), row.domID(), row.flags(), row.groupId());
    }

    public static MemberDOO insert(MemberDOO player) throws SQLException {
        MemberDOO inserted = parse(MemberRepository.insert(player.playerUUID, player.domID, player.flags));
        CacheManager.instance.getCache().getMemberCache().load(inserted.getId());
        return inserted;
    }

    public static List<MemberDOO> select() throws SQLException {
        return MemberRepository.select().stream().map(MemberDOO::parse).toList();
    }

    public static MemberDOO select(Integer id) throws SQLException {
        return parse(MemberRepository.select(id));
    }

    public static List<MemberDOO> selectByDominionId(Integer dom_id) throws SQLException {
        return MemberRepository.selectByDominionId(dom_id).stream().map(MemberDOO::parse).toList();
    }

    public static void deleteById(Integer id) throws SQLException {
        MemberRepository.deleteById(id);
        CacheManager.instance.getCache().getMemberCache().delete(id);
    }

    public static List<MemberDOO> selectByGroupId(Integer groupId) throws SQLException {
        return MemberRepository.selectByGroupId(groupId).stream().map(MemberDOO::parse).toList();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    @Override
    public Integer getDomID() {
        return domID;
    }

    @Override
    public Integer getGroupId() {
        return groupId;
    }


    @Override
    public @NotNull Boolean getFlagValue(PriFlag flag) {
        if (!flags.containsKey(flag)) return flag.getDefaultValue();
        return flags.get(flag);
    }

    @Override
    public @NotNull Map<PriFlag, Boolean> getFlagsValue() {
        return flags;
    }

    @Override
    public MemberDOO setFlagValue(@NotNull PriFlag flag, @NotNull Boolean value) throws SQLException {
        flags.put(flag, value);
        MemberRepository.updateFlag(id, flag, value);
        return this;
    }

    @Override
    public @NotNull PlayerDTO getPlayer() {
        return Objects.requireNonNull(CacheManager.instance.getPlayer(getPlayerUUID()));
    }

    public MemberDOO setGroupId(Integer groupId) throws SQLException {
        this.groupId = groupId;
        MemberRepository.updateGroupId(id, groupId);
        return this;
    }

    public void applyTemplate(TemplateDOO template) throws SQLException {
        for (PriFlag flag : Flags.getAllPriFlagsEnable()) {
            this.flags.put(flag, template.getFlagValue(flag));
        }
        MemberRepository.updateFlags(id, flags);
    }

    /**
     * Delete member by player UUID.
     * <p>
     * THIS SHOULD ONLY BE USED TO CLEAR LEGACY DATA.
     *
     * @param playerUUID the UUID of the player to delete
     * @throws SQLException if a database access error occurs
     */
    public static void deleteByPlayerUuid(UUID playerUUID) throws SQLException {
        MemberRepository.deleteByPlayerUuid(playerUUID);
    }

    private MemberDOO(Integer id, UUID playerUUID, Integer domID, Map<PriFlag, Boolean> flags, Integer groupId) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.domID = domID;
        this.groupId = groupId;
        this.flags.putAll(flags);
    }

    public MemberDOO(UUID playerUUID, DominionDTO dom) {
        this.playerUUID = playerUUID;
        this.domID = dom.getId();
        this.groupId = -1;
        this.flags.putAll(dom.getGuestPrivilegeFlagValue());
    }

}
