package cn.lunadeer.dominion.uis.tuis.template;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.dtos.TemplateDTO;
import cn.lunadeer.dominion.managers.Translation;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.stui.ListView;
import cn.lunadeer.minecraftpluginutils.stui.components.Button;
import cn.lunadeer.minecraftpluginutils.stui.components.Line;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static cn.lunadeer.dominion.utils.CommandUtils.playerOnly;
import static cn.lunadeer.dominion.utils.TuiUtils.getPage;

public class TemplateSetting {

    // /dominion template setting <模板名称> [页码]
    public static void show(CommandSender sender, String templateName, String pageStr) {
        Player player = playerOnly(sender);
        if (player == null) return;
        int page = getPage(args, 3);
        TemplateDTO template = TemplateDTO.select(player.getUniqueId(), args[2]);
        if (template == null) {
            Notification.error(sender, Translation.Messages_TemplateNotExist, args[2]);
            return;
        }

        ListView view = ListView.create(10, "/dominion template setting " + template.getName());
        view.title(String.format(Translation.TUI_TemplateSetting_Title.trans(), args[1]));
        view.navigator(Line.create()
                .append(Button.create(Translation.TUI_Navigation_Menu).setExecuteCommand("/dominion menu").build())
                .append(Button.create(Translation.TUI_Navigation_TemplateList).setExecuteCommand("/dominion template list").build())
                .append(Translation.TUI_Navigation_TemplateSetting)
        );

        // /dominion template_set_flag <模板名称> <权限名称> <true/false> [页码]
        for (PriFlag flag : Flags.getAllPriFlagsEnable()) {
            view.add(createOption(flag, template.getFlagValue(flag), template.getName(), page));
        }
        view.showOn(player, page);
    }

    private static Line createOption(PriFlag flag, boolean value, String templateName, int page) {
        if (value) {
            return Line.create()
                    .append(Button.createGreen("☑").setExecuteCommand("/dominion template set_flag " + templateName + " " + flag.getFlagName() + " false " + page).build())
                    .append(Component.text(flag.getDisplayName()).hoverEvent(Component.text(flag.getDescription())));
        } else {
            return Line.create()
                    .append(Button.createRed("☐").setExecuteCommand("/dominion template set_flag " + templateName + " " + flag.getFlagName() + " true " + page).build())
                    .append(Component.text(flag.getDisplayName()).hoverEvent(Component.text(flag.getDescription())));
        }
    }
}
