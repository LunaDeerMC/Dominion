package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.storage.repository.TemplateRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TemplateDOO {

    private Integer id;
    private UUID creator;
    private String name;
    private final Map<PriFlag, Boolean> flags;

    private static TemplateDOO parse(TemplateRepository.TemplateRow row) {
        if (row == null) return null;
        return new TemplateDOO(row.id(), row.creator(), row.name(), row.flags());
    }

    public static TemplateDOO create(UUID creator, String name) throws SQLException {
        TemplateDOO result = parse(TemplateRepository.create(creator, name));
        if (result == null) {
            throw new SQLException("Failed to create template.");
        }
        return result;
    }

    public static TemplateDOO select(UUID creator, String name) throws SQLException {
        return parse(TemplateRepository.select(creator, name));
    }

    public static List<TemplateDOO> selectAll(UUID creator) throws SQLException {
        return TemplateRepository.selectAll(creator).stream().map(TemplateDOO::parse).toList();
    }

    public static void delete(UUID creator, String name) throws SQLException {
        TemplateRepository.delete(creator, name);
    }

    private TemplateDOO(Integer id, UUID creator, String name, Map<PriFlag, Boolean> flags) {
        this.id = id;
        this.creator = creator;
        this.name = name;
        this.flags = flags;
    }

    public Integer getId() {
        return id;
    }

    public UUID getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    public Boolean getFlagValue(PriFlag flag) {
        if (!flags.containsKey(flag)) return flag.getDefaultValue();
        return flags.get(flag);
    }

    public TemplateDOO setFlagValue(PriFlag flag, Boolean value) throws SQLException {
        flags.put(flag, value);
        TemplateRepository.updateFlag(id, flag, value);
        return this;
    }

    public TemplateDOO setName(String name) throws SQLException {
        this.name = name;
        TemplateRepository.updateName(id, name);
        return this;
    }
}
