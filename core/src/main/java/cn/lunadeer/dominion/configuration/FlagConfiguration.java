package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlagGroup;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroup;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlagGroup;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Owns the flags.yml lifecycle. General plugin configuration only delegates
 * its loading entry to this class.
 */
public final class FlagConfiguration {

    private static final int SCHEMA_VERSION = 2;
    private static final Map<String, List<String>> unresolvedEnvironmentGroupFlags = new HashMap<>();
    private static final Map<String, List<String>> unresolvedPrivilegeGroupFlags = new HashMap<>();

    private FlagConfiguration() {
    }

    public static synchronized void load() throws IOException {
        XLogger.info(Language.configurationText.loadingFlag);
        File yamlFile = new File(Dominion.instance.getDataFolder(), "flags.yml");
        boolean existed = yamlFile.exists();
        if (!existed) {
            yamlFile.createNewFile();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
        int schemaVersion = yaml.getInt("schema-version", existed ? 1 : SCHEMA_VERSION);
        boolean migrateSplitFlags = existed && schemaVersion < SCHEMA_VERSION;
        reconcileFlagDefinitions(yaml, migrateSplitFlags);
        if (!existed || schemaVersion < SCHEMA_VERSION) {
            FlagGroups.replaceConfiguredGroups(
                    FlagGroups.defaultEnvironmentGroups(),
                    FlagGroups.defaultPrivilegeGroups()
            );
            unresolvedEnvironmentGroupFlags.clear();
            unresolvedPrivilegeGroupFlags.clear();
        } else {
            loadConfiguredFlagGroups(yaml);
        }
        Language.loadFlagGroupTexts();
        yaml.set("schema-version", SCHEMA_VERSION);
        writeConfiguredFlagGroups(yaml);
        yaml.save(yamlFile);
        XLogger.info(Language.configurationText.loadFlagSuccess);
    }

    /**
     * Persists the current flag/group registry without reloading unrelated
     * Dominion configuration.
     */
    public static synchronized void saveRuntimeConfiguration() throws IOException {
        File yamlFile = new File(Dominion.instance.getDataFolder(), "flags.yml");
        if (!yamlFile.exists()) {
            yamlFile.createNewFile();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
        reconcileFlagDefinitions(yaml, false);
        yaml.set("schema-version", SCHEMA_VERSION);
        writeConfiguredFlagGroups(yaml);
        yaml.save(yamlFile);
    }

    /**
     * Resolves names that were kept in YAML before an add-on registered its
     * custom flags. This is invoked immediately before an apply pass.
     */
    static synchronized void resolveAvailableGroupFlags() {
        resolveEnvironmentFlags();
        resolvePrivilegeFlags();
    }

    private static void resolveEnvironmentFlags() {
        Iterator<Map.Entry<String, List<String>>> groups = unresolvedEnvironmentGroupFlags.entrySet().iterator();
        while (groups.hasNext()) {
            Map.Entry<String, List<String>> entry = groups.next();
            EnvFlagGroup group = FlagGroups.getEnvFlagGroup(entry.getKey());
            if (group == null) {
                continue;
            }
            List<String> remaining = new ArrayList<>();
            for (String name : entry.getValue()) {
                EnvFlag flag = Flags.getEnvFlag(name);
                if (flag == null) {
                    remaining.add(name);
                } else {
                    group.addFlag(flag);
                }
            }
            if (remaining.isEmpty()) {
                groups.remove();
            } else {
                entry.setValue(List.copyOf(remaining));
            }
        }
    }

    private static void resolvePrivilegeFlags() {
        Iterator<Map.Entry<String, List<String>>> groups = unresolvedPrivilegeGroupFlags.entrySet().iterator();
        while (groups.hasNext()) {
            Map.Entry<String, List<String>> entry = groups.next();
            PriFlagGroup group = FlagGroups.getPriFlagGroup(entry.getKey());
            if (group == null) {
                continue;
            }
            List<String> remaining = new ArrayList<>();
            for (String name : entry.getValue()) {
                PriFlag flag = Flags.getPreFlag(name);
                if (flag == null) {
                    remaining.add(name);
                } else {
                    group.addFlag(flag);
                }
            }
            if (remaining.isEmpty()) {
                groups.remove();
            } else {
                entry.setValue(List.copyOf(remaining));
            }
        }
    }

    static void reconcileFlagDefinitions(YamlConfiguration yaml, boolean migrateSplitFlags) {
        for (Flag flag : Flags.getAllFlags()) {
            if (yaml.contains(flag.getConfigurationDefaultKey())) {
                flag.setDefaultValue(yaml.getBoolean(flag.getConfigurationDefaultKey()));
            } else {
                Flag source = migrateSplitFlags ? Flags.getLegacySource(flag) : null;
                boolean value = source == null
                        ? flag.getDefaultValue()
                        : yaml.getBoolean(source.getConfigurationDefaultKey(), source.getDefaultValue());
                flag.setDefaultValue(value);
                yaml.set(flag.getConfigurationDefaultKey(), value);
            }
            if (yaml.contains(flag.getConfigurationEnableKey())) {
                flag.setEnable(yaml.getBoolean(flag.getConfigurationEnableKey()));
            } else {
                Flag source = migrateSplitFlags ? Flags.getLegacySource(flag) : null;
                boolean value = source == null
                        ? flag.getEnable()
                        : yaml.getBoolean(source.getConfigurationEnableKey(), source.getEnable());
                flag.setEnable(value);
                yaml.set(flag.getConfigurationEnableKey(), value);
            }
            if (yaml.contains(flag.getConfigurationMaterialKey())) {
                flag.setMaterial(yaml.getString(flag.getConfigurationMaterialKey()));
            } else {
                yaml.set(flag.getConfigurationMaterialKey(), flag.getMaterial().name());
            }
            yaml.setInlineComments(
                    flag.getConfigurationNameKey(),
                    Collections.singletonList(flag.getDisplayName() + "-" + flag.getDescription())
            );
        }
    }

    private static void loadConfiguredFlagGroups(YamlConfiguration yaml) {
        unresolvedEnvironmentGroupFlags.clear();
        unresolvedPrivilegeGroupFlags.clear();
        List<EnvFlagGroup> environment = loadEnvironmentGroups(
                yaml.getConfigurationSection("groups.environment"));
        List<PriFlagGroup> privilege = loadPrivilegeGroups(
                yaml.getConfigurationSection("groups.privilege"));
        FlagGroups.replaceConfiguredGroups(environment, privilege);
    }

    private static List<EnvFlagGroup> loadEnvironmentGroups(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<EnvFlagGroup> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            if (!validGroupId(id)) {
                continue;
            }
            ConfigurationSection group = section.getConfigurationSection(id);
            if (group == null) {
                continue;
            }
            List<EnvFlag> flags = new ArrayList<>();
            List<String> unresolved = new ArrayList<>();
            for (String name : group.getStringList("flags")) {
                EnvFlag flag = Flags.getEnvFlag(name);
                if (flag == null) {
                    unresolved.add(name);
                    XLogger.warn("Unknown or non-environment flag {0} in environment group {1}.", name, id);
                } else if (!flags.contains(flag)) {
                    flags.add(flag);
                }
            }
            if (!unresolved.isEmpty()) {
                unresolvedEnvironmentGroupFlags.put(id, List.copyOf(unresolved));
            }
            result.add(new EnvFlagGroup(
                    id,
                    id,
                    "",
                    groupMaterial(group.getString("material")),
                    flags
            ));
        }
        return result;
    }

    private static List<PriFlagGroup> loadPrivilegeGroups(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<PriFlagGroup> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            if (!validGroupId(id)) {
                continue;
            }
            ConfigurationSection group = section.getConfigurationSection(id);
            if (group == null) {
                continue;
            }
            List<PriFlag> flags = new ArrayList<>();
            List<String> unresolved = new ArrayList<>();
            for (String name : group.getStringList("flags")) {
                PriFlag flag = Flags.getPreFlag(name);
                if (flag == null) {
                    unresolved.add(name);
                    XLogger.warn("Unknown or non-privilege flag {0} in privilege group {1}.", name, id);
                } else if (!flags.contains(flag)) {
                    flags.add(flag);
                }
            }
            if (!unresolved.isEmpty()) {
                unresolvedPrivilegeGroupFlags.put(id, List.copyOf(unresolved));
            }
            result.add(new PriFlagGroup(
                    id,
                    id,
                    "",
                    groupMaterial(group.getString("material")),
                    flags
            ));
        }
        return result;
    }

    private static boolean validGroupId(String id) {
        if (id.equals("ungrouped")) {
            XLogger.warn("Flag group id 'ungrouped' is reserved for the dynamic fallback group.");
            return false;
        }
        if (id.matches("[a-z0-9_-]+")) {
            return true;
        }
        XLogger.warn("Invalid flag group id {0}; expected [a-z0-9_-]+.", id);
        return false;
    }

    private static Material groupMaterial(String value) {
        Material material = value == null ? null : Material.matchMaterial(value);
        if (material == null) {
            if (value != null) {
                XLogger.warn("Invalid flag group material {0}; using BUNDLE.", value);
            }
            return Material.BUNDLE;
        }
        return material;
    }

    static void writeConfiguredFlagGroups(YamlConfiguration yaml) {
        yaml.set("groups", null);
        yaml.createSection("groups");
        yaml.createSection("groups.environment");
        yaml.createSection("groups.privilege");
        for (EnvFlagGroup group : FlagGroups.getEnvFlagGroups()) {
            writeGroup(
                    yaml,
                    "groups.environment." + group.getId(),
                    group,
                    unresolvedEnvironmentGroupFlags.getOrDefault(group.getId(), List.of())
            );
        }
        for (PriFlagGroup group : FlagGroups.getPriFlagGroups()) {
            writeGroup(
                    yaml,
                    "groups.privilege." + group.getId(),
                    group,
                    unresolvedPrivilegeGroupFlags.getOrDefault(group.getId(), List.of())
            );
        }
    }

    private static void writeGroup(YamlConfiguration yaml,
                                   String path,
                                   FlagGroup<?> group,
                                   List<String> unresolved) {
        yaml.set(path + ".material", group.getMaterial().name());
        LinkedHashSet<String> names = new LinkedHashSet<>();
        group.getFlags().forEach(flag -> names.add(flag.getFlagName()));
        names.addAll(unresolved);
        yaml.set(path + ".flags", new ArrayList<>(names));
    }
}
