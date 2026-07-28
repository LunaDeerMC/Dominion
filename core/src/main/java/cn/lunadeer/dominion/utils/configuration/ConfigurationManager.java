package cn.lunadeer.dominion.utils.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility class for loading and saving configuration files.
 * <p>
 * This class uses reflection to read and write configuration files. Capable of reading and writing nested configuration parts.
 */
public class ConfigurationManager {

    /**
     * Load the configuration file.
     *
     * @param clazz The configuration file class. The class should extend {@link ConfigurationFile}.
     * @param file  The file to load.
     * @throws Exception If failed to load the file.
     */
    public static ConfigurationFile load(Class<? extends ConfigurationFile> clazz, File file) throws Exception {
        if (!file.exists()) {
            return saveDefault(clazz, file);
        }
        ConfigurationFile instance = readConfigurationFile(YamlConfiguration.loadConfiguration(file), clazz);
        instance.save(file);
        return instance;
    }

    /**
     * Load the configuration file and update the version field if needed.
     *
     * @param clazz            The configuration file class. The class should extend {@link ConfigurationFile}.
     * @param file             The file to load.
     * @param versionFieldName The name of the version field.
     * @throws Exception If failed to load the file.
     */
    public static ConfigurationFile load(Class<? extends ConfigurationFile> clazz, File file, String versionFieldName) throws Exception {
        Field versionField = clazz.getField(versionFieldName);
        int currentVersion = versionField.getInt(null);
        ConfigurationFile instance = load(clazz, file);
        if (versionField.getInt(null) != currentVersion) {
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            if (backup.exists() && !backup.delete()) {
                throw new Exception("Failed to delete the backup configuration file.");
            }
            if (!file.renameTo(backup)) {
                throw new Exception("Failed to backup the configuration file.");
            }
            clazz.getField(versionFieldName).set(null, currentVersion);
            return saveDefault(clazz, file);
        }
        return instance;
    }

    /**
     * Save the configuration file.
     *
     * @param clazz The configuration file class. The class should extend {@link ConfigurationFile}.
     * @param file  The file to save.
     * @throws Exception If failed to save the file.
     */
    public static ConfigurationFile saveDefault(Class<? extends ConfigurationFile> clazz, File file) throws Exception {
        createIfNotExist(file);
        YamlConfiguration yaml = writeConfigurationFile(clazz);
        yaml.options().width(250);
        yaml.save(file);
        return load(clazz, file);
    }

    private static YamlConfiguration writeConfigurationFile(Class<? extends ConfigurationFile> clazz) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        if (clazz.isAnnotationPresent(Headers.class)) {
            yaml.options().setHeader(List.of(clazz.getAnnotation(Headers.class).value()));
        }
        ConfigurationFile instance = clazz.getConstructor().newInstance();
        writeConfigurationPart(yaml, instance, null);
        return yaml;
    }

    public static void writeConfigurationPart(ConfigurationSection yaml, ConfigurationPart obj, String prefix) throws Exception {
        writeConfigurationPart(yaml, obj, prefix, false);
    }

    public static void writeConfigurationPart(ConfigurationSection yaml, ConfigurationPart obj, String prefix, boolean ignoreComment) throws Exception {
        for (Field field : obj.getClass().getFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(HandleManually.class)) {
                continue;
            }
            String newKey = configurationKey(field);
            if (prefix != null && !prefix.isEmpty()) {
                newKey = prefix + "." + newKey;
            }
            if (ConfigurationPart.class.isAssignableFrom(field.getType())) {
                writeConfigurationPart(yaml, (ConfigurationPart) field.get(obj), newKey);
            } else {
                yaml.set(newKey, field.get(obj));
            }
            if (!ignoreComment) {
                if (field.isAnnotationPresent(Comments.class)) {
                    yaml.setComments(newKey, List.of(field.getAnnotation(Comments.class).value()));
                }
                if (field.isAnnotationPresent(Comment.class)) {
                    yaml.setInlineComments(newKey, List.of(field.getAnnotation(Comment.class).value()));
                }
            }
        }
    }

    private static void createIfNotExist(File file) throws Exception {
        if (file.exists()) return;
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
            throw new Exception("Failed to create %s directory.".formatted(file.getParentFile().getAbsolutePath()));
        if (!file.createNewFile()) throw new Exception("Failed to create %s file.".formatted(file.getAbsolutePath()));
    }

    private static ConfigurationFile readConfigurationFile(YamlConfiguration yaml, Class<? extends ConfigurationFile> clazz) throws Exception {
        ConfigurationFile instance = clazz.getConstructor().newInstance();
        instance.setYaml(yaml);
        PrePostProcessInorder processes = getAndSortPrePostProcess(clazz);
        // execute methods with @PreProcess annotation
        for (Method method : processes.preProcessMethods) {
            method.invoke(instance);
        }
        readConfigurationPart(instance.getYaml(), instance, null);
        // execute methods with @PostProcess annotation
        for (Method method : processes.postProcessMethods) {
            method.invoke(instance);
        }
        return instance;
    }

    public static ConfigurationPart readConfigurationPart(ConfigurationSection yaml, ConfigurationPart obj, String prefix) throws Exception {
        for (Field field : obj.getClass().getFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(HandleManually.class)) {
                continue;
            }
            String key = configurationKey(field);
            if (prefix != null && !prefix.isEmpty()) {
                key = prefix + "." + key;
            }
            boolean missingKey = !yaml.contains(key);
            if (missingKey) {
                yaml.createSection(key);
                if (field.isAnnotationPresent(Comments.class)) {
                    yaml.setComments(key, List.of(field.getAnnotation(Comments.class).value()));
                }
                if (field.isAnnotationPresent(Comment.class)) {
                    yaml.setInlineComments(key, List.of(field.getAnnotation(Comment.class).value()));
                }
            }
            if (ConfigurationPart.class.isAssignableFrom(field.getType())) {
                field.set(obj, readConfigurationPart(yaml, (ConfigurationPart) field.get(obj), key));
            } else {
                if (missingKey) {
                    yaml.set(key, field.get(obj));
                } else {
                    field.set(obj, yaml.get(key));
                }
            }
        }
        return obj;
    }

    /**
     * Converts a camelCase string to kebab-case.
     *
     * @param camel The camelCase string.
     * @return The kebab-case string.
     */
    private static String camelToKebab(String camel) {
        return camel.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
    }

    private static String configurationKey(Field field) {
        if (field.isAnnotationPresent(ConfigurationKey.class)) {
            String value = field.getAnnotation(ConfigurationKey.class).value();
            if (value == null || value.isBlank() || value.contains(".")) {
                throw new IllegalArgumentException("Invalid configuration key override on " + field.getName());
            }
            return value;
        }
        return camelToKebab(field.getName());
    }

    private static class PrePostProcessInorder {
        public List<Method> preProcessMethods = new ArrayList<>();
        public List<Method> postProcessMethods = new ArrayList<>();
    }

    /**
     * Get methods with @PreProcess and @PostProcess annotations and sort them by priority.
     * <p>
     * The methods with higher priority will be executed first.
     *
     * @param clazz The configuration file class.
     * @return A {@link PrePostProcessInorder} object containing the sorted methods.
     */
    private static PrePostProcessInorder getAndSortPrePostProcess(Class<? extends ConfigurationFile> clazz) {
        Map<Method, Integer> preProcessMethodsWithPriority = new HashMap<>();
        Map<Method, Integer> postProcessMethodsWithPriority = new HashMap<>();
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(PreProcess.class)) {
                preProcessMethodsWithPriority.put(method, method.getAnnotation(PreProcess.class).priority());
            }
            if (method.isAnnotationPresent(PostProcess.class)) {
                postProcessMethodsWithPriority.put(method, method.getAnnotation(PostProcess.class).priority());
            }
        }
        PrePostProcessInorder result = new PrePostProcessInorder();
        // sort methods by priority ascending
        result.preProcessMethods = new ArrayList<>(preProcessMethodsWithPriority.keySet());
        result.preProcessMethods.sort(Comparator.comparingInt(preProcessMethodsWithPriority::get));
        result.postProcessMethods = new ArrayList<>(postProcessMethodsWithPriority.keySet());
        result.postProcessMethods.sort(Comparator.comparingInt(postProcessMethodsWithPriority::get));
        return result;
    }

}
