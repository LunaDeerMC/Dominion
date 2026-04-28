package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.storage.repository.GroupRepository;
import cn.lunadeer.dominion.utils.ColorParser;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

public class GroupDOO implements GroupDTO {

    private Integer id;
    private Integer dom_id;
    private String name_raw;
    private String name_color;
    private final Map<PriFlag, Boolean> flags = new HashMap<>();

    private static GroupDOO parse(GroupRepository.GroupRow row) {
        if (row == null) return null;
        return new GroupDOO(row.id(), row.domID(), row.namePlain(), row.flags(), row.nameColored());
    }

    @Override
    public @NotNull Integer getId() {
        return id;
    }

    @Override
    public @NotNull Integer getDomID() {
        return dom_id;
    }

    @Override
    public @NotNull String getNameRaw() {
        return name_color;
    }

    @Override
    public @NotNull String getNamePlain() {
        return name_raw;
    }

    @Override
    public @NotNull Component getNameColoredComponent() {
        String with_pre_suf = "&#ffffff" +
                Configuration.groupTitle.prefix +
                name_color +
                "&#ffffff" +
                Configuration.groupTitle.suffix;
        return ColorParser.getComponentType(with_pre_suf);
    }

    @Override
    public @NotNull String getNameColoredBukkit() {
        String with_pre_suf = "&#ffffff" +
                Configuration.groupTitle.prefix +
                name_color +
                "&#ffffff" +
                Configuration.groupTitle.suffix;
        return ColorParser.getBukkitType(with_pre_suf);
    }

    @Override
    public @NotNull Boolean getFlagValue(@NotNull PriFlag flag) {
        return flags.getOrDefault(flag, flag.getDefaultValue());
    }

    @Override
    public @NotNull Map<PriFlag, Boolean> getFlagsValue() {
        return flags;
    }

    @Override
    public @NotNull GroupDOO setName(@NotNull String name) throws SQLException {
        this.name_color = name;
        this.name_raw = ColorParser.getPlainText(name);
        GroupRepository.updateName(id, name_raw, name_color);
        return this;
    }

    @Override
    public @NotNull GroupDOO setFlagValue(@NotNull PriFlag flag, @NotNull Boolean value) throws SQLException {
        flags.put(flag, value);
        GroupRepository.updateFlag(id, flag, value);
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
        GroupDOO inserted = parse(GroupRepository.create(group.dom_id, group.name_raw, group.name_color, group.flags));
        if (inserted == null) {
            throw new SQLException("Failed to insert dominion.");
        }
        CacheManager.instance.getCache().getGroupCache().load(inserted.getId());
        return inserted;
    }

    public static void deleteById(Integer id) throws SQLException {
        GroupRepository.deleteById(id);
        CacheManager.instance.getCache().getGroupCache().delete(id);
        List<MemberDOO> players = MemberDOO.selectByGroupId(id);
        for (MemberDOO player : players) {
            player.setGroupId(-1);
        }
        CacheManager.instance.getCache().getMemberCache().load();
    }

    public static List<GroupDOO> select() throws SQLException {
        return GroupRepository.select().stream().map(GroupDOO::parse).toList();
    }

    public static GroupDOO select(Integer id) throws SQLException {
        return parse(GroupRepository.select(id));
    }

    public static List<GroupDOO> selectByDominionId(Integer domID) throws SQLException {
        return GroupRepository.selectByDominionId(domID).stream().map(GroupDOO::parse).toList();
    }

    private GroupDOO(String name, Integer domID) {
        this.dom_id = domID;
        this.name_raw = ColorParser.getPlainText(name);
        this.name_color = name;
        for (PriFlag f : Flags.getAllPriFlagsEnable()) {
            flags.put(f, f.getDefaultValue());
        }
    }

    private GroupDOO(Integer id, Integer domID, String name, Map<PriFlag, Boolean> flags, String nameColored) {
        this.id = id;
        this.dom_id = domID;
        this.name_raw = name;
        this.flags.putAll(flags);
        this.name_color = nameColored;
    }
}
