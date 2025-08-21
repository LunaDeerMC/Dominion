package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.providers.DominionProvider;
import cn.lunadeer.dominion.uis.MigrateList;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.ResMigration;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.command.Argument;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.toWorld;

public class MigrationCommand {

    public static class MigrationCommandText extends ConfigurationPart {
        public String migrateSuccess = "Migrated residence {0} to dominion successfully.";
        public String migrateFailed = "Failed to migrate residence. Reason: {0}";
        public String missingResidence = "Residence {0} not found.";
        public String notYourResidence = "Residence {0} is not yours.";
        public String migrateDescription = "Migrate a specific residence to dominion.";
        public String migrateAllDescription = "Migrate all residences to dominions.";
    }

    /**
     * Creates a ListViewButton for migration.
     *
     * @param sender        the command sender
     * @param residenceName the name of the residence
     * @return the ListViewButton
     */
    public static ListViewButton button(CommandSender sender, String residenceName) {
        return (ListViewButton) new ListViewButton(TextUserInterface.migrateListTuiText.button) {
            @Override
            public void function(String pageStr) {
                migrate(sender, residenceName, pageStr);
            }
        }.needPermission(defaultPermission);
    }

    /**
     * Secondary command for migration.
     */
    public static SecondaryCommand migrate = new SecondaryCommand("migrate", List.of(
            new Argument("residence_name", true),
            new CommandArguments.OptionalPageArgument()
    ), Language.migrationCommandText.migrateDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            migrate(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    /**
     * Secondary command to migrate all residences.
     * This command will iterate through all residence data and migrate them.
     */
    public static SecondaryCommand migrateAll = new SecondaryCommand("migrate_all", List.of(
    ), Language.migrationCommandText.migrateAllDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            migrateAll(sender);
        }
    }.needPermission(adminPermission).register();

    /**
     * Handles the migration process.
     *
     * @param sender  the command sender
     * @param resName the name of the residence
     * @param pageStr the page string
     */
    public static void migrate(CommandSender sender, String resName, String pageStr) {
        try {
            if (!Configuration.residenceMigration) {
                Notification.error(sender, TextUserInterface.migrateListTuiText.notEnabled);
                return;
            }
            List<ResMigration.ResidenceNode> res_data = CacheManager.instance.getResidenceCache().getResidenceData();
            if (res_data == null) {
                throw new DominionException(TextUserInterface.migrateListTuiText.noData);
            }
            ResMigration.ResidenceNode resNode = res_data.stream().filter(node -> node.name.equals(resName)).findFirst().orElse(null);
            if (resNode == null) {
                throw new DominionException(Language.migrationCommandText.missingResidence, resName);
            }
            if (sender instanceof Player player && !player.hasPermission(adminPermission)) {
                if (!resNode.owner.equals(player.getUniqueId())) {
                    throw new DominionException(Language.migrationCommandText.notYourResidence, resName);
                }
            }
            doMigrateCreate(sender, resNode, null);
            MigrateList.show(sender, pageStr);
        } catch (Exception e) {
            Notification.error(sender, Language.migrationCommandText.migrateFailed, e.getMessage());
        }
    }

    /**
     * Performs the actual migration creation process.
     *
     * @param sender the player
     * @param node   the residence node
     * @param parent the parent dominion DTO
     * @throws Exception if an error occurs during migration
     */
    private static void doMigrateCreate(CommandSender sender, ResMigration.ResidenceNode node, DominionDTO parent) throws Exception {
        PlayerDTO ownerDTO = PlayerDOO.create(node.owner, node.ownerName);
        CuboidDTO cuboidDTO = new CuboidDTO(node.loc1, node.loc2);
        World world = toWorld(node.world.getUID());
        int renameNumber = 0;
        while (DominionDOO.select(renameNumber == 0 ? node.name : node.name + "_" + renameNumber) != null) {
            renameNumber++;
        }
        DominionDTO dominion = DominionProvider.getInstance().createDominion(
                sender,
                renameNumber == 0 ? node.name : node.name + "_" + renameNumber,
                ownerDTO.getUuid(), world, cuboidDTO, parent, true).get();
        if (dominion != null) {
            Notification.info(sender, Language.migrationCommandText.migrateSuccess, node.name);
            postProcessMigration(sender, node, dominion);
        }
    }

    /**
     * Post-processes the migration after a dominion is created.
     * <p>
     * This method updates the created dominion with additional properties from the residence node,
     * such as teleport location, join/leave messages, and recursively migrates any child residences.
     *
     * @param sender          the command sender
     * @param node            the residence node containing migration data
     * @param dominionCreated the DominionDTO that was created
     * @throws Exception if an error occurs during child migration
     */
    private static void postProcessMigration(CommandSender sender, ResMigration.ResidenceNode node, DominionDTO dominionCreated) throws Exception {
        assert dominionCreated != null : "Migrate Dominion created failed, event.getDominion() is null";
        if (node.tpLoc != null) {
            dominionCreated = dominionCreated.setTpLocation(node.tpLoc);
        }
        if (node.joinMessage != null) {
            dominionCreated = dominionCreated.setJoinMessage(node.joinMessage);
        }
        if (node.leaveMessage != null) {
            dominionCreated = dominionCreated.setLeaveMessage(node.leaveMessage);
        }
        if (node.children != null) {
            for (ResMigration.ResidenceNode child : node.children) {
                doMigrateCreate(sender, child, dominionCreated);
            }
        }
    }

    public static void migrateAll(CommandSender sender) {
        List<ResMigration.ResidenceNode> res_data = CacheManager.instance.getResidenceCache().getResidenceData();
        if (res_data == null || res_data.isEmpty()) {
            Notification.error(sender, TextUserInterface.migrateListTuiText.noData);
            return;
        }
        int successCount = 0;
        for (ResMigration.ResidenceNode resNode : res_data) {
            try {
                doMigrateCreate(sender, resNode, null);
                successCount++;
            } catch (Exception e) {
                Notification.error(sender, Language.migrationCommandText.migrateFailed, e.getMessage());
                XLogger.error(e);
            }
        }
        Notification.info(sender, Language.migrationCommandText.migrateSuccess, successCount + "/" + res_data.size());
    }
}
