package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.utils.databse.FIelds.Field;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldBoolean;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldInteger;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldString;
import cn.lunadeer.dominion.utils.databse.syntax.Delete;
import cn.lunadeer.dominion.utils.databse.syntax.Insert;
import cn.lunadeer.dominion.utils.databse.syntax.Select;
import cn.lunadeer.dominion.utils.databse.syntax.Update;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

public class MemberDOO implements MemberDTO {

    private final FieldInteger id = new FieldInteger("id");
    private final FieldString playerUUID = new FieldString("player_uuid");
    private final FieldInteger domID = new FieldInteger("dom_id");
    private final FieldInteger groupId = new FieldInteger("group_id");
    private final Map<PriFlag, Boolean> flags = new HashMap<>();

    private static Field<?>[] fields() {
        Field<?>[] fields = new Field<?>[Flags.getAllPriFlagsEnable().size() + 4];
        fields[0] = new FieldInteger("id");
        fields[1] = new FieldString("player_uuid");
        fields[2] = new FieldInteger("dom_id");
        fields[3] = new FieldInteger("group_id");
        int i = 4;
        for (PriFlag f : Flags.getAllPriFlagsEnable()) {
            fields[i] = new FieldBoolean(f.getFlagName());
            i++;
        }
        return fields;
    }

    private static MemberDOO parse(Map<String, Field<?>> map) {
        Map<PriFlag, Boolean> flags = new HashMap<>();
        for (PriFlag f : Flags.getAllPriFlagsEnable()) {
            flags.put(f, (Boolean) map.get(f.getFlagName()).getValue());
        }
        return new MemberDOO((Integer) map.get("id").getValue(),
                UUID.fromString((String) map.get("player_uuid").getValue()),
                (Integer) map.get("dom_id").getValue(),
                flags,
                (Integer) map.get("group_id").getValue());
    }

    public static MemberDOO insert(MemberDOO player) throws SQLException {
        Map<String, Field<?>> res = Insert.insert()
                .into("dominion_member")
                .values(player.playerUUID, player.domID)
                .returning(fields())
                .execute();
        MemberDOO inserted = parse(res);
        CacheManager.instance.getCache().getMemberCache().load(inserted.getId());
        return inserted;
    }

    public static List<MemberDOO> select() throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion_member")
                .execute();
        return res.stream().map(MemberDOO::parse).toList();
    }

    public static MemberDOO select(Integer id) throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion_member")
                .where("id = ?", id)
                .execute();
        if (res.isEmpty()) return null;
        return parse(res.get(0));
    }

    public static List<MemberDOO> selectByDominionId(Integer dom_id) throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion_member")
                .where("dom_id = ?", dom_id)
                .execute();
        return res.stream().map(MemberDOO::parse).toList();
    }

    public static void deleteById(Integer id) throws SQLException {
        Delete.delete().from("dominion_member").where("id = ?;", id).execute();
        CacheManager.instance.getCache().getMemberCache().delete(id);
    }

    public static List<MemberDOO> selectByGroupId(Integer groupId) throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion_member")
                .where("group_id = ?", groupId)
                .execute();
        return res.stream().map(MemberDOO::parse).toList();
    }

    @Override
    public Integer getId() {
        return id.getValue();
    }

    @Override
    public UUID getPlayerUUID() {
        return UUID.fromString(playerUUID.getValue());
    }

    @Override
    public Integer getDomID() {
        return domID.getValue();
    }

    @Override
    public Integer getGroupId() {
        return groupId.getValue();
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
        FieldBoolean flagField = new FieldBoolean(flag.getFlagName(), value);
        Update.update("dominion_member")
                .set(flagField)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    @Override
    public @NotNull PlayerDTO getPlayer() {
        return Objects.requireNonNull(CacheManager.instance.getPlayer(getPlayerUUID()));
    }

    public MemberDOO setGroupId(Integer groupId) throws SQLException {
        this.groupId.setValue(groupId);
        Update.update("dominion_member")
                .set(this.groupId)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    public void applyTemplate(TemplateDOO template) throws SQLException {
        FieldBoolean[] updateFields = new FieldBoolean[Flags.getAllPriFlagsEnable().size()];
        for (int i = 0; i < Flags.getAllPriFlagsEnable().size(); i++) {
            PriFlag flag = Flags.getAllPriFlagsEnable().get(i);
            updateFields[i] = new FieldBoolean(flag.getFlagName(), template.getFlagValue(flag));
            this.flags.put(flag, template.getFlagValue(flag));
        }
        Update.update("dominion_member")
                .set(updateFields)
                .where("id = ?", id.getValue())
                .execute();
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
        Delete.delete().from("dominion_member").where("player_uuid = ?", playerUUID.toString()).execute();
    }

    private MemberDOO(Integer id, UUID playerUUID, Integer domID, Map<PriFlag, Boolean> flags, Integer groupId) {
        this.id.setValue(id);
        this.playerUUID.setValue(playerUUID.toString());
        this.domID.setValue(domID);
        this.groupId.setValue(groupId);
        this.flags.putAll(flags);
    }

    public MemberDOO(UUID playerUUID, DominionDTO dom) {
        this.playerUUID.setValue(playerUUID.toString());
        this.domID.setValue(dom.getId());
        this.flags.putAll(dom.getGuestPrivilegeFlagValue());
    }

}
