package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroup;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.commands.*;
import cn.lunadeer.dominion.handler.DominionProviderHandler;
import cn.lunadeer.dominion.handler.GroupProviderHandler;
import cn.lunadeer.dominion.handler.MemberProviderHandler;
import cn.lunadeer.dominion.handler.SelectPointEventsHandler;
import cn.lunadeer.dominion.managers.DatabaseBackupManager;
import cn.lunadeer.dominion.managers.MultiServerManager;
import cn.lunadeer.dominion.managers.TeleportManager;
import cn.lunadeer.dominion.misc.Asserts;
import cn.lunadeer.dominion.misc.Converts;
import cn.lunadeer.dominion.misc.Others;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.VaultConnect.VaultConnect;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.command.InvalidArgumentException;
import cn.lunadeer.dominion.utils.command.NoPermissionException;
import cn.lunadeer.dominion.utils.configuration.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Headers({
        "Language file for Dominion plugin",
        "If you want to help translate this file, please refer to:",
        "https://dominion.lunadeer.cn/en/notes/doc/owner/config-ref/languages",
        "for more instructions.",
        "",
        "Most of the text support color codes,",
        "you can use §0-§9 for colors, §l for bold, §o for italic, §n for underline, §m for strikethrough, and §k for magic.",
        "Also support '&' as an alternative for '§'.",
})
public class Language extends ConfigurationFile {

    // languages file name list here will be saved to plugin data folder
    @HandleManually
    public enum LanguageCode {
        en_us,
        zh_cn,
        jp_jp,
        zh_tw,
    }

    public static void loadLanguageFiles(CommandSender sender, JavaPlugin plugin, String code) {
        try {
            // save default language files to the languages folder
            File languagesFolder = new File(Dominion.instance.getDataFolder(), "languages");
            for (LanguageCode languageCode : LanguageCode.values()) {
                updateLanguageFiles(plugin, languageCode.name(), false);
            }
            Notification.info(sender != null ? sender : Dominion.instance.getServer().getConsoleSender(), Language.configurationText.loadingLanguage, code);
            ConfigurationManager.load(Language.class, new File(languagesFolder, code + ".yml"));
            Notification.info(sender != null ? sender : Dominion.instance.getServer().getConsoleSender(), Language.configurationText.loadLanguageSuccess, code);
        } catch (Exception e) {
            Notification.error(sender != null ? sender : Dominion.instance.getServer().getConsoleSender(), Language.configurationText.loadLanguageFail, code, e.getMessage());
        }
    }

    public static void updateLanguageFiles(JavaPlugin plugin, String code, boolean overwrite) {
        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        if (!languagesFolder.exists()) {
            languagesFolder.mkdir();
        }
        if (!new File(languagesFolder, code + ".yml").exists()) try {
            Dominion.instance.saveResource("languages/" + code + ".yml", overwrite);
        } catch (Exception e) {
            XLogger.warn("Failed to save language file for {0}, This language may not in official repo : {1}.", code, e.getMessage());
            XLogger.warn("See https://dominion.lunadeer.cn/en/notes/doc/owner/config-ref/languages , If you want to help us to add this language.");
        }
    }

    public static synchronized void reconcileFlagTexts() throws IOException {
        File file = activeLanguageFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Flag flag : Flags.getAllFlags()) {
            if (!yaml.contains(flag.getDisplayNameKey())) {
                yaml.set(flag.getDisplayNameKey(), flag.getDisplayName());
            }
            if (!yaml.contains(flag.getDescriptionKey())) {
                yaml.set(flag.getDescriptionKey(), flag.getDescription());
            }
        }
        yaml.save(file);
    }

    /**
     * Loads group display text from the active language and adds missing keys
     * using the group's API defaults.
     */
    public static synchronized void loadFlagGroupTexts() throws IOException {
        File file = activeLanguageFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean changed = loadFlagGroupTexts(yaml);
        if (changed) {
            yaml.save(file);
        }
    }

    static boolean loadFlagGroupTexts(YamlConfiguration yaml) {
        boolean changed = false;
        for (FlagGroup<?> group : configuredFlagGroups()) {
            if (yaml.isString(group.getDisplayNameKey())) {
                String displayName = yaml.getString(group.getDisplayNameKey(), group.getDisplayName());
                if (!displayName.equals(group.getDisplayName())) {
                    group.setDisplayName(displayName);
                }
            } else {
                yaml.set(group.getDisplayNameKey(), group.getDisplayName());
                changed = true;
            }
            if (yaml.isString(group.getDescriptionKey())) {
                String description = yaml.getString(group.getDescriptionKey(), group.getDescription());
                if (!description.equals(group.getDescription())) {
                    group.setDescription(description);
                }
            } else {
                yaml.set(group.getDescriptionKey(), group.getDescription());
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Persists runtime group metadata to the active language file.
     */
    public static synchronized void saveFlagGroupTexts() throws IOException {
        File file = activeLanguageFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (FlagGroup<?> group : configuredFlagGroups()) {
            yaml.set(group.getDisplayNameKey(), group.getDisplayName());
            yaml.set(group.getDescriptionKey(), group.getDescription());
        }
        yaml.save(file);
    }

    private static File activeLanguageFile() {
        File file = new File(Dominion.instance.getDataFolder(), "languages/" + Configuration.language + ".yml");
        if (!file.isFile()) {
            file = new File(Dominion.instance.getDataFolder(), "languages/en_us.yml");
        }
        return file;
    }

    private static List<FlagGroup<?>> configuredFlagGroups() {
        List<FlagGroup<?>> groups = new ArrayList<>();
        groups.addAll(FlagGroups.getEnvFlagGroups());
        groups.addAll(FlagGroups.getPriFlagGroups());
        return groups;
    }

    public static Dominion.DominionText dominionText = new Dominion.DominionText();

    public static MultiServerManager.MultiServerManagerText multiServerManagerText = new MultiServerManager.MultiServerManagerText();

    public static Asserts.AssertsText assertsText = new Asserts.AssertsText();
    public static Converts.ConvertsText convertsText = new Converts.ConvertsText();
    public static Others.OthersText othersText = new Others.OthersText();

    public static VaultConnect.VaultConnectText vaultConnectText = new VaultConnect.VaultConnectText();

    // Event Handler
    public static DominionProviderHandler.DominionProviderHandlerText dominionProviderHandlerText = new DominionProviderHandler.DominionProviderHandlerText();
    public static MemberProviderHandler.MemberProviderHandlerText memberProviderHandlerText = new MemberProviderHandler.MemberProviderHandlerText();
    public static GroupProviderHandler.GroupProviderHandlerText groupProviderHandlerText = new GroupProviderHandler.GroupProviderHandlerText();
    public static SelectPointEventsHandler.SelectPointEventsHandlerText selectPointEventsHandlerText = new SelectPointEventsHandler.SelectPointEventsHandlerText();

    // Commands
    public static AdministratorCommand.AdministratorCommandText administratorCommandText = new AdministratorCommand.AdministratorCommandText();
    public static MigrationCommand.MigrationCommandText migrationCommandText = new MigrationCommand.MigrationCommandText();
    public static TemplateCommand.TemplateCommandText templateCommandText = new TemplateCommand.TemplateCommandText();
    public static GroupTitleCommand.GroupTitleCommandText groupTitleCommandText = new GroupTitleCommand.GroupTitleCommandText();
    public static CopyCommand.CopyCommandText copyCommandText = new CopyCommand.CopyCommandText();
    public static DominionOperateCommand.DominionOperateCommandText dominionOperateCommandText = new DominionOperateCommand.DominionOperateCommandText();
    public static DominionCreateCommand.DominionCreateCommandText dominionCreateCommandText = new DominionCreateCommand.DominionCreateCommandText();
    public static DominionFlagCommand.DominionFlagCommandText dominionFlagCommandText = new DominionFlagCommand.DominionFlagCommandText();
    public static GroupCommand.GroupCommandText groupCommandText = new GroupCommand.GroupCommandText();
    public static MemberCommand.MemberCommandText memberCommandText = new MemberCommand.MemberCommandText();

    public static Configuration.ConfigurationText configurationText = new Configuration.ConfigurationText();

    public static Limitation.LimitationText limitationText = new Limitation.LimitationText();

    public static DatabaseBackupManager.DatabaseManagerText databaseManagerText = new DatabaseBackupManager.DatabaseManagerText();

    public static TeleportManager.TeleportManagerText teleportManagerText = new TeleportManager.TeleportManagerText();

    public static CommandExceptionText commandExceptionText = new CommandExceptionText();

    public static class CommandExceptionText extends ConfigurationPart {
        public String noPermission = "You do not have permission {0} to do this.";
        public String invalidArguments = "Invalid arguments, usage e.g. {0}.";
    }

    @PreProcess
    public void loadFlagsText() {
        for (Flag flag : Flags.getAllFlags()) {
            if (getYaml().contains(flag.getDisplayNameKey())) {
                flag.setDisplayName(getYaml().getString(flag.getDisplayNameKey()));
            } else {
                getYaml().set(flag.getDisplayNameKey(), flag.getDisplayName());
            }
            if (getYaml().contains(flag.getDescriptionKey())) {
                flag.setDescription(getYaml().getString(flag.getDescriptionKey()));
            } else {
                getYaml().set(flag.getDescriptionKey(), flag.getDescription());
            }
        }
    }

    @PostProcess
    public static void setOtherStaticText() {
        // cn.lunadeer.dominion.utils.command
        InvalidArgumentException.MSG = commandExceptionText.invalidArguments;
        NoPermissionException.MSG = commandExceptionText.noPermission;

    }

}
