package cn.lunadeer.dominion.commands;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.TemplateDTO;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.misc.CommandArguments;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.providers.TemplateProvider;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.command.Argument;
import cn.lunadeer.dominion.utils.command.SecondaryCommand;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.*;


public class TemplateCommand {

    public static class TemplateCommandText extends ConfigurationPart {
        public String nameNotValid = "Template name cannot contain space";
        public String templateNameExist = "Template {0} already exists";
        public String createTemplateFail = "Failed to create template, reason: {0}";
        public String createTemplateSuccess = "Successfully created template {0}";

        public String templateNotExist = "Template {0} does not exist";

        public String deleteTemplateSuccess = "Successfully deleted template {0}";
        public String deleteTemplateFail = "Failed to delete template, reason: {0}";

        public String applyTemplateSuccess = "Successfully applied template {0} to {1}";
        public String applyTemplateFail = "Failed to apply template, reason: {0}";

        public String setFlagSuccess = "Successfully set {0} flag of template {1} to {2}";
        public String setFlagFail = "Failed to set flag, reason: {0}";

        public String renameTemplateSuccess = "Successfully renamed template to {0}";
        public String renameTemplateFail = "Failed to rename template, reason: {0}";

        public String createTemplateDescription = "Create a new privilege template.";
        public String deleteTemplateDescription = "Delete an existing privilege template.";
        public String setTemplateFlagDescription = "Set a privilege flag in a template.";
        public String memberApplyTemplateDescription = "Apply a privilege template to a member.";
        public String renameTemplateDescription = "Rename an existing privilege template.";
    }

    /**
     * Command to create a new template.
     * This command requires the default permission to execute.
     */
    public static SecondaryCommand createTemplate = new SecondaryCommand("template_create", List.of(
            new Argument("template_name", true)
    ), Language.templateCommandText.createTemplateDescription) {
        /**
         * Executes the create template command.
         *
         * @param sender The command sender.
         */
        @Override
        public void executeHandler(CommandSender sender) {
            createTemplate(sender, getArgumentValue(0));
        }
    }.needPermission(defaultPermission).register();

    /**
     * Creates a new template.
     *
     * @param sender       The command sender.
     * @param templateName The name of the template to be created.
     */
    public static void createTemplate(CommandSender sender, String templateName) {
        try {
            Player player = toPlayer(sender);
            TemplateProvider.getInstance().createTemplate(player, templateName);
        } catch (Exception e) {
            Notification.error(sender, Language.templateCommandText.createTemplateFail, e.getMessage());
        }
    }

    /**
     * Command to delete an existing template.
     * This command requires the default permission to execute.
     */
    public static SecondaryCommand deleteTemplate = new SecondaryCommand("template_delete", List.of(
            new CommandArguments.RequiredTemplateArgument()
    ), Language.templateCommandText.deleteTemplateDescription) {
        /**
         * Executes the delete template command.
         *
         * @param sender The command sender.
         */
        @Override
        public void executeHandler(CommandSender sender) {
            deleteTemplate(sender, getArgumentValue(0));
        }
    }.needPermission(defaultPermission).register();

    /**
     * Deletes an existing template.
     *
     * @param sender       The command sender.
     * @param templateName The name of the template to be deleted.
     */
    public static void deleteTemplate(CommandSender sender, String templateName) {
        try {
            Player player = toPlayer(sender);
            TemplateDTO template = TemplateProvider.getInstance().getTemplate(player.getUniqueId(), templateName);
            if (template == null) {
                throw new DominionException(Language.templateCommandText.templateNotExist, templateName);
            }
            TemplateProvider.getInstance().deleteTemplate(player, template);
        } catch (Exception e) {
            Notification.error(sender, Language.templateCommandText.deleteTemplateFail, e.getMessage());
        }
    }

    public static SecondaryCommand setTemplateFlag = new SecondaryCommand("template_set_flag", List.of(
            new CommandArguments.RequiredTemplateArgument(),
            new CommandArguments.PriFlagArgument(),
            new CommandArguments.BollenOption()
    ), Language.templateCommandText.setTemplateFlagDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            setTemplateFlag(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2));
        }
    }.needPermission(defaultPermission).register();

    public static void setTemplateFlag(CommandSender sender, String templateName, String flagName, String valueStr) {
        try {
            Player player = toPlayer(sender);
            boolean value = toBoolean(valueStr);
            PriFlag flag = toPriFlag(flagName);
            TemplateDTO template = TemplateProvider.getInstance().getTemplate(player.getUniqueId(), templateName);
            if (template == null) {
                throw new DominionException(Language.templateCommandText.templateNotExist, templateName);
            }
            TemplateProvider.getInstance().setTemplateFlag(player, template, flag, value);
        } catch (Exception e) {
            Notification.error(sender, Language.templateCommandText.setFlagFail, e.getMessage());
        }
    }

    public static SecondaryCommand memberApplyTemplate = new SecondaryCommand("member_apply_template", List.of(
            new CommandArguments.RequiredDominionArgument(),
            new CommandArguments.RequiredMemberArgument(0),
            new CommandArguments.RequiredTemplateArgument()
    ), Language.templateCommandText.memberApplyTemplateDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            memberApplyTemplate(sender, getArgumentValue(0), getArgumentValue(1), getArgumentValue(2));
        }
    }.needPermission(defaultPermission).register();

    public static void memberApplyTemplate(CommandSender sender, String dominionName, String playerName, String templateName) {
        try {
            Player player = toPlayer(sender);
            TemplateDTO template = TemplateProvider.getInstance().getTemplate(player.getUniqueId(), templateName);
            if (template == null) {
                throw new DominionException(Language.templateCommandText.templateNotExist, templateName);
            }
            DominionDTO dominion = toDominionDTO(dominionName);
            MemberDTO member = toMemberDTO(dominion, playerName);
            TemplateProvider.getInstance().applyTemplate(player, dominion, member, template);
        } catch (Exception e) {
            Notification.error(sender, Language.templateCommandText.applyTemplateFail, e.getMessage());
        }
    }

    public static SecondaryCommand renameTemplate = new SecondaryCommand("template_rename", List.of(
            new CommandArguments.RequiredTemplateArgument(),
            new Argument("new_name", true)
    ), Language.templateCommandText.renameTemplateDescription) {
        @Override
        public void executeHandler(CommandSender sender) {
            renameTemplate(sender, getArgumentValue(0), getArgumentValue(1));
        }
    }.needPermission(defaultPermission).register();

    public static void renameTemplate(CommandSender sender, String templateName, String newTemplateName) {
        try {
            Player player = toPlayer(sender);
            TemplateDTO template = TemplateProvider.getInstance().getTemplate(player.getUniqueId(), templateName);
            if (template == null) {
                throw new DominionException(Language.templateCommandText.templateNotExist, templateName);
            }
            TemplateProvider.getInstance().renameTemplate(player, template, newTemplateName);
        } catch (Exception e) {
            Notification.error(sender, Language.templateCommandText.renameTemplateFail, e.getMessage());
        }
    }

}
