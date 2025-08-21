package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.ColorParser;
import cn.lunadeer.dominion.utils.databse.FIelds.Field;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldBoolean;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldInteger;
import cn.lunadeer.dominion.utils.databse.FIelds.FieldString;
import cn.lunadeer.dominion.utils.databse.syntax.Delete;
import cn.lunadeer.dominion.utils.databse.syntax.Insert;
import cn.lunadeer.dominion.utils.databse.syntax.Select;
import cn.lunadeer.dominion.utils.databse.syntax.Update;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

public class GroupDOO implements GroupDTO {

    private final FieldInteger id = new FieldInteger("id");
    private final FieldInteger dom_id = new FieldInteger("dom_id");
    private final FieldString name_raw = new FieldString("name");
    private final FieldString name_color = new FieldString("name_colored");
    private final Map<PriFlag, Boolean> flags = new HashMap<>();

    private static Field<?>[] fields() {
        Field<?>[] fields = new Field<?>[Flags.getAllPriFlagsEnable().size() + 4];
        fields[0] = new FieldInteger("id");
        fields[1] = new FieldInteger("dom_id");
        fields[2] = new FieldString("name");
        fields[3] = new FieldString("name_colored");
        int i = 4;
        for (PriFlag f : Flags.getAllPriFlagsEnable()) {
            fields[i] = new FieldBoolean(f.getFlagName());
            i++;
        }
        return fields;
    }

    private static GroupDOO parse(Map<String, Field<?>> map) {
        Map<PriFlag, Boolean> flags = new HashMap<>();
        for (PriFlag f : Flags.getAllPriFlagsEnable()) {
            flags.put(f, (Boolean) map.get(f.getFlagName()).getValue());
        }
        return new GroupDOO((Integer) map.get("id").getValue(),
                (Integer) map.get("dom_id").getValue(),
                (String) map.get("name").getValue(),
                flags,
                (String) map.get("name_colored").getValue());
    }

    @Override
    public @NotNull Integer getId() {
        return id.getValue();
    }

    @Override
    public @NotNull Integer getDomID() {
        return dom_id.getValue();
    }

    @Override
    public @NotNull String getNameRaw() {
        return name_color.getValue();
    }

    @Override
    public @NotNull String getNamePlain() {
        return name_raw.getValue();
    }

    @Override
    public @NotNull Component getNameColoredComponent() {
        String with_pre_suf = "&#ffffff" +
                Configuration.groupTitle.prefix +
                name_color.getValue() +
                "&#ffffff" +
                Configuration.groupTitle.suffix;
        return ColorParser.getComponentType(with_pre_suf);
    }

    @Override
    public @NotNull String getNameColoredBukkit() {
        String with_pre_suf = "&#ffffff" +
                Configuration.groupTitle.prefix +
                name_color.getValue() +
                "&#ffffff" +
                Configuration.groupTitle.suffix;
        return ColorParser.getBukkitType(with_pre_suf);
    }

    @Override
    public @NotNull Boolean getFlagValue(@NotNull PriFlag flag) {
        return flags.getOrDefault(flag, false);
    }

    @Override
    public @NotNull Map<PriFlag, Boolean> getFlagsValue() {
        return flags;
    }

    @Override
    public @NotNull GroupDOO setName(@NotNull String name) throws SQLException {
        this.name_color.setValue(name);
        this.name_raw.setValue(ColorParser.getPlainText(name));
        Update.update("dominion_group")
                .set(name_color, name_raw)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    @Override
    public @NotNull GroupDOO setFlagValue(@NotNull PriFlag flag, @NotNull Boolean value) throws SQLException {
        flags.put(flag, value);
        FieldBoolean flagField = new FieldBoolean(flag.getFlagName(), value);
        Update.update("dominion_group")
                .set(flagField)
                .where("id = ?", id.getValue())
                .execute();
        return this;
    }

    @Override
    public List<MemberDTO> getMembers() {
        DominionDTO dominion = CacheManager.instance.getDominion(getDomID());
        if (dominion == null) return new ArrayList<>();
        List<MemberDTO> members = dominion.getMembers();
        List<MemberDTO> result = new ArrayList<>();
        for (MemberDTO member : members) {
            if (Objects.equals(member.getGroupId(), getId())) {
                result.add(member);
            }
        }
        return result;
    }

    public static GroupDOO create(String name, DominionDTO dominionDTO) throws SQLException {
        GroupDOO group = new GroupDOO(name, dominionDTO.getId());
        Map<String, Field<?>> res = Insert.insert().into("dominion_group")
                .values(group.dom_id, group.name_raw, group.name_color)
                .returning(fields())
                .execute();
        if (res.isEmpty()) {
            throw new SQLException("Failed to insert dominion.");
        }
        GroupDOO inserted = parse(res);
        CacheManager.instance.getCache().getGroupCache().load(inserted.getId());
        return inserted;
    }

    public static void deleteById(Integer id) throws SQLException {
        Delete.delete().from("dominion_group").where("id = ?", id).execute();
        CacheManager.instance.getCache().getGroupCache().delete(id);
        List<MemberDOO> players = MemberDOO.selectByGroupId(id);
        for (MemberDOO player : players) {
            player.setGroupId(-1);
        }
        CacheManager.instance.getCache().getMemberCache().load();
    }

    public static List<GroupDOO> select() throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion_group")
                .execute();
        return res.stream().map(GroupDOO::parse).toList();
    }

    public static GroupDOO select(Integer id) throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion_group")
                .where("id = ?", id)
                .execute();
        if (res.isEmpty()) return null;
        return parse(res.get(0));
    }

    public static List<GroupDOO> selectByDominionId(Integer domID) throws SQLException {
        List<Map<String, Field<?>>> res = Select.select(fields())
                .from("dominion_group")
                .where("dom_id = ?", domID)
                .execute();
        return res.stream().map(GroupDOO::parse).toList();
    }

    private GroupDOO(String name, Integer domID) {
        this.dom_id.setValue(domID);
        this.name_raw.setValue(ColorParser.getPlainText(name));
        this.name_color.setValue(name);
        for (PriFlag f : Flags.getAllPriFlagsEnable()) {
            flags.put(f, f.getDefaultValue());
        }
    }

    private GroupDOO(Integer id, Integer domID, String name, Map<PriFlag, Boolean> flags, String nameColored) {
        this.id.setValue(id);
        this.dom_id.setValue(domID);
        this.name_raw.setValue(name);
        this.flags.putAll(flags);
        this.name_color.setValue(nameColored);
    }
}
