package cn.lunadeer.dominion.uis.dominion.manage.member;

import cn.lunadeer.dominion.commands.TemplateCommand;
import cn.lunadeer.dominion.configuration.uis.ChestUserInterface;
import cn.lunadeer.dominion.doos.TemplateDOO;
import cn.lunadeer.dominion.uis.AbstractUI;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scui.ChestButton;
import cn.lunadeer.dominion.utils.scui.ChestListView;
import cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager;
import cn.lunadeer.dominion.utils.scui.configuration.ButtonConfiguration;
import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.toIntegrity;
import static cn.lunadeer.dominion.utils.Misc.formatString;

public class SelectTemplate extends AbstractUI {

    public static void show(CommandSender sender, String dominionName, String playerName, String pageStr) {
        new SelectTemplate().displayByPreference(sender, dominionName, playerName, pageStr);
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ CUI ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public static class SelectTemplateCui extends ConfigurationPart {
        public String title = "§6✦ §5§lSelect Template for {0} §6✦";
        public ListViewConfiguration listConfiguration = new ListViewConfiguration(
                'i',
                List.of(
                        "<########",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "#iiiiiii#",
                        "p#######n"
                )
        );

        public ButtonConfiguration backButton = ButtonConfiguration.createMaterial(
                '<', Material.BARRIER,
                "§c« Back to Member Settings",
                List.of(
                        "§7Return to the member settings",
                        "§7without applying a template.",
                        "",
                        "§e▶ Click to go back"
                )
        );

        public ButtonConfiguration templateButton = ButtonConfiguration.createMaterial(
                'i', Material.WRITABLE_BOOK,
                "§a⚙ §2{0}",
                List.of(
                        "§7Apply this permission template",
                        "§7to member {0}.",
                        "",
                        "§7Template: §e{1}",
                        "",
                        "§2▶ Click to apply template",
                        "",
                        "§8This will override member's",
                        "§8current permissions."
                )
        );
    }

    @Override
    protected void showCUI(Player player, String... args) throws Exception {
        String dominionName = args[0];
        String playerName = args[1];

        ChestListView view = ChestUserInterfaceManager.getInstance().getListViewOf(player);
        view.setTitle(formatString(ChestUserInterface.selectTemplateCui.title, playerName));
        view.applyListConfiguration(ChestUserInterface.selectTemplateCui.listConfiguration, toIntegrity(args[2]));

        view.setButton(ChestUserInterface.selectTemplateCui.backButton.getSymbol(),
                new ChestButton(ChestUserInterface.selectTemplateCui.backButton) {
                    @Override
                    public void onClick(ClickType type) {
                        MemberFlags.show(player, dominionName, playerName, "1");
                    }
                }
        );

        List<TemplateDOO> templates = TemplateDOO.selectAll(player.getUniqueId());
        for (TemplateDOO template : templates) {
            ChestButton templateChest = new ChestButton(ChestUserInterface.selectTemplateCui.templateButton) {
                @Override
                public void onClick(ClickType type) {
                    TemplateCommand.memberApplyTemplate(player, dominionName, playerName, template.getName());
                    MemberFlags.show(player, dominionName, playerName, "1");
                }
            }.setDisplayNameArgs(template.getName()).setLoreArgs(playerName, template.getName());
            view.addItem(templateChest);
        }

        view.open();
    }

    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ CUI ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ Console ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    @Override
    protected void showConsole(CommandSender sender, String... args) throws Exception {
    }
}
