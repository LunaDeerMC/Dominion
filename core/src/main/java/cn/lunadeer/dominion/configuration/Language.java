package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.commands.*;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.handler.DominionProviderHandler;
import cn.lunadeer.dominion.handler.GroupProviderHandler;
import cn.lunadeer.dominion.handler.MemberProviderHandler;
import cn.lunadeer.dominion.handler.SelectPointEventsHandler;
import cn.lunadeer.dominion.inputters.*;
import cn.lunadeer.dominion.managers.DatabaseTables;
import cn.lunadeer.dominion.managers.MultiServerManager;
import cn.lunadeer.dominion.managers.TeleportManager;
import cn.lunadeer.dominion.misc.Asserts;
import cn.lunadeer.dominion.misc.Converts;
import cn.lunadeer.dominion.misc.Others;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.VaultConnect.VaultConnect;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.command.InvalidArgumentException;
import cn.lunadeer.dominion.utils.command.NoPermissionException;
import cn.lunadeer.dominion.utils.configuration.*;
import cn.lunadeer.dominion.utils.stui.inputter.InputterRunner;
import org.bukkit.command.CommandSender;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

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
            File cuiFolder = new File(languagesFolder, "cui");
            File tuiFolder = new File(languagesFolder, "tui");
            for (LanguageCode languageCode : LanguageCode.values()) {
                updateLanguageFiles(plugin, languageCode.name(), false);
            }
            Notification.info(sender != null ? sender : Dominion.instance.getServer().getConsoleSender(), Language.configurationText.loadingLanguage, code);
            ConfigurationManager.load(Language.class, new File(languagesFolder, code + ".yml"));
            ConfigurationManager.load(ChestUserInterface.class, new File(cuiFolder, code + ".yml"));
            ConfigurationManager.load(TextUserInterface.class, new File(tuiFolder, code + ".yml"));
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
        File cuiFolder = new File(languagesFolder, "cui");
        if (!cuiFolder.exists()) {
            cuiFolder.mkdir();
        }
        File tuiFolder = new File(languagesFolder, "tui");
        if (!tuiFolder.exists()) {
            tuiFolder.mkdir();
        }
        if (!new File(languagesFolder, code + ".yml").exists()) try {
            Dominion.instance.saveResource("languages/" + code + ".yml", overwrite);
        } catch (Exception e) {
            XLogger.warn("Failed to save language file for {0}, This language may not in official repo : {1}.", code, e.getMessage());
            XLogger.warn("See https://dominion.lunadeer.cn/en/notes/doc/owner/config-ref/languages , If you want to help us to add this language.");
        }
        if (!new File(cuiFolder, code + ".yml").exists()) try {
            Dominion.instance.saveResource("languages/cui/" + code + ".yml", overwrite);
        } catch (Exception e) {
            XLogger.warn("Failed to save CUI language file for {0}, This language may not in official repo : {1}.", code, e.getMessage());
            XLogger.warn("See https://dominion.lunadeer.cn/en/notes/doc/owner/config-ref/languages , If you want to help us to add this language.");
        }
        if (!new File(tuiFolder, code + ".yml").exists()) try {
            Dominion.instance.saveResource("languages/tui/" + code + ".yml", overwrite);
        } catch (Exception e) {
            XLogger.warn("Failed to save TUI language file for {0}, This language may not in official repo : {1}.", code, e.getMessage());
            XLogger.warn("See https://dominion.lunadeer.cn/en/notes/doc/owner/config-ref/languages , If you want to help us to add this language.");
        }
    }

    public static Dominion.DominionText dominionText = new Dominion.DominionText();
    public static AbstractUI.ConsoleText consoleText = new AbstractUI.ConsoleText();
    public static AbstractUI.UiCommandsDescription uiCommandsDescription = new AbstractUI.UiCommandsDescription();

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

    // Inputter
    public static CreateDominionInputter.CreateDominionInputterText createDominionInputterText = new CreateDominionInputter.CreateDominionInputterText();
    public static CreateGroupInputter.CreateGroupInputterText createGroupInputterText = new CreateGroupInputter.CreateGroupInputterText();
    public static RenameDominionInputter.RenameDominionInputterText renameDominionInputterText = new RenameDominionInputter.RenameDominionInputterText();
    public static EditMessageInputter.EditMessageInputterText editMessageInputterText = new EditMessageInputter.EditMessageInputterText();
    public static CreateTemplateInputter.CreateTemplateInputterText createTemplateInputterText = new CreateTemplateInputter.CreateTemplateInputterText();
    public static RenameGroupInputter.RenameGroupInputterText renameGroupInputterText = new RenameGroupInputter.RenameGroupInputterText();
    public static ResizeDominionInputter.ResizeDominionInputterText resizeDominionInputterText = new ResizeDominionInputter.ResizeDominionInputterText();
    public static SearchPlayerInputter.SearchPlayerInputterText searchPlayerInputterText = new SearchPlayerInputter.SearchPlayerInputterText();
    public static SetMapColorInputter.SetMapColorInputterText setMapColorInputterText = new SetMapColorInputter.SetMapColorInputterText();
    public static RenameTemplateInputter.RenameTemplateInputterText renameTemplateInputterText = new RenameTemplateInputter.RenameTemplateInputterText();

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

    public static DatabaseTables.DatabaseManagerText databaseManagerText = new DatabaseTables.DatabaseManagerText();

    public static TeleportManager.TeleportManagerText teleportManagerText = new TeleportManager.TeleportManagerText();

    public static CommandExceptionText commandExceptionText = new CommandExceptionText();

    public static class CommandExceptionText extends ConfigurationPart {
        public String noPermission = "You do not have permission {0} to do this.";
        public String invalidArguments = "Invalid arguments, usage e.g. {0}.";
    }

    public static InputterText inputterText = new InputterText();

    public static class InputterText extends ConfigurationPart {
        public String onlyPlayer = "TUI inputter can only be used by a player.";
        public String cancel = " [Send 'C' to cancel the inputter.]";
        public String inputterCancelled = "Inputter cancelled.";
    }

    public static SpawnReasonText spawnReasonText = new SpawnReasonText();
    
    public static class SpawnReasonText extends ConfigurationPart {
        // Natural spawns
        public String naturalDisplayName = "Natural Spawn";
        public String naturalDescription = "Monsters spawn naturally in darkness";
        public String spawnerDisplayName = "Spawner";
        public String spawnerDescription = "Monsters spawn from monster spawner blocks";
        public String chunkGenDisplayName = "Chunk Generation";
        public String chunkGenDescription = "Monsters spawn during world generation";
        public String jockeyDisplayName = "Jockey";
        public String jockeyDescription = "Spider jockeys, chicken jockeys, etc.";
        public String patrolDisplayName = "Patrol";
        public String patrolDescription = "Pillager patrol spawning";
        public String raidDisplayName = "Raid";
        public String raidDescription = "Raid event spawning";
        public String reinforcementsDisplayName = "Reinforcements";
        public String reinforcementsDescription = "Zombie reinforcements when attacked";
        public String villageInvasionDisplayName = "Village Invasion";
        public String villageInvasionDescription = "Zombie siege on villages";
        public String netherPortalDisplayName = "Nether Portal";
        public String netherPortalDescription = "Zombified piglin from nether portal";
        public String silverfishBlockDisplayName = "Silverfish Block";
        public String silverfishBlockDescription = "Silverfish from infested blocks";
        public String trapDisplayName = "Trap";
        public String trapDescription = "Skeleton horse trap from lightning";
        public String enderPearlDisplayName = "Ender Pearl";
        public String enderPearlDescription = "Endermite from ender pearl";
        public String spellDisplayName = "Spell";
        public String spellDescription = "Evoker summoning vex";

        // Player-initiated spawns
        public String spawnerEggDisplayName = "Spawn Egg";
        public String spawnerEggDescription = "Player using spawn egg";
        public String dispenseEggDisplayName = "Dispenser Egg";
        public String dispenseEggDescription = "Dispenser spawning from egg";
        public String commandDisplayName = "Command";
        public String commandDescription = "Spawned by command";
        public String customDisplayName = "Custom/Plugin";
        public String customDescription = "Spawned by plugin";

        // Conversion spawns
        public String infectionDisplayName = "Infection";
        public String infectionDescription = "Villager converting to zombie villager";
        public String drownedDisplayName = "Drowned Conversion";
        public String drownedDescription = "Zombie converting to drowned";
        public String lightningDisplayName = "Lightning";
        public String lightningDescription = "Pig converting to zombified piglin";
        public String curedDisplayName = "Cured";
        public String curedDescription = "Zombie villager being cured";

        public String getDisplayName(CreatureSpawnEvent.SpawnReason reason) {
            switch (reason) {
                case NATURAL: return naturalDisplayName;
                case SPAWNER: return spawnerDisplayName;
                case CHUNK_GEN: return chunkGenDisplayName;
                case JOCKEY: return jockeyDisplayName;
                case PATROL: return patrolDisplayName;
                case RAID: return raidDisplayName;
                case REINFORCEMENTS: return reinforcementsDisplayName;
                case VILLAGE_INVASION: return villageInvasionDisplayName;
                case NETHER_PORTAL: return netherPortalDisplayName;
                case SILVERFISH_BLOCK: return silverfishBlockDisplayName;
                case TRAP: return trapDisplayName;
                case ENDER_PEARL: return enderPearlDisplayName;
                case SPELL: return spellDisplayName;
                case SPAWNER_EGG: return spawnerEggDisplayName;
                case DISPENSE_EGG: return dispenseEggDisplayName;
                case COMMAND: return commandDisplayName;
                case CUSTOM: return customDisplayName;
                case INFECTION: return infectionDisplayName;
                case DROWNED: return drownedDisplayName;
                case LIGHTNING: return lightningDisplayName;
                case CURED: return curedDisplayName;
                default: return reason.name();
            }
        }

        public String getDescription(CreatureSpawnEvent.SpawnReason reason) {
            switch (reason) {
                case NATURAL: return naturalDescription;
                case SPAWNER: return spawnerDescription;
                case CHUNK_GEN: return chunkGenDescription;
                case JOCKEY: return jockeyDescription;
                case PATROL: return patrolDescription;
                case RAID: return raidDescription;
                case REINFORCEMENTS: return reinforcementsDescription;
                case VILLAGE_INVASION: return villageInvasionDescription;
                case NETHER_PORTAL: return netherPortalDescription;
                case SILVERFISH_BLOCK: return silverfishBlockDescription;
                case TRAP: return trapDescription;
                case ENDER_PEARL: return enderPearlDescription;
                case SPELL: return spellDescription;
                case SPAWNER_EGG: return spawnerEggDescription;
                case DISPENSE_EGG: return dispenseEggDescription;
                case COMMAND: return commandDescription;
                case CUSTOM: return customDescription;
                case INFECTION: return infectionDescription;
                case DROWNED: return drownedDescription;
                case LIGHTNING: return lightningDescription;
                case CURED: return curedDescription;
                default: return "";
            }
        }
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

        InputterRunner.ONLY_PLAYER = inputterText.onlyPlayer;
        InputterRunner.CANCEL = inputterText.cancel;
        InputterRunner.INPUTTER_CANCELLED = inputterText.inputterCancelled;
    }

}
