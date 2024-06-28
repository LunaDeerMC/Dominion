package cn.lunadeer.dominion.tuis;

import cn.lunadeer.dominion.Cache;
import cn.lunadeer.dominion.dtos.*;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.stui.ListView;
import cn.lunadeer.minecraftpluginutils.stui.components.Button;
import cn.lunadeer.minecraftpluginutils.stui.components.Line;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static cn.lunadeer.dominion.commands.Apis.playerOnly;
import static cn.lunadeer.dominion.tuis.Apis.getDominionNameArg_1;
import static cn.lunadeer.dominion.tuis.Apis.noAuthToManage;

public class DominionPrivilegeList {

    public static void show(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length == 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (Exception ignored) {
            }
        }
        Player player = playerOnly(sender);
        if (player == null) return;
        DominionDTO dominion = getDominionNameArg_1(player, args);
        if (dominion == null) {
            Notification.error(sender, "你不在任何领地内，请指定领地名称 /dominion privilege_list <领地名称>");
            return;
        }
        ListView view = ListView.create(10, "/dominion privilege_list " + dominion.getName());
        if (noAuthToManage(player, dominion)) return;
        List<PlayerPrivilegeDTO> privileges = PlayerPrivilegeDTO.select(dominion.getId());
        view.title("领地 " + dominion.getName() + " 成员列表");
        view.navigator(
                Line.create()
                        .append(Button.create("主菜单").setExecuteCommand("/dominion menu").build())
                        .append(Button.create("我的领地").setExecuteCommand("/dominion list").build())
                        .append(Button.create("管理界面").setExecuteCommand("/dominion manage " + dominion.getName()).build())
                        .append("成员列表")
        );
        view.add(Line.create().append(Button.create("添加成员").setExecuteCommand("/dominion select_player_create_privilege " + dominion.getName()).build()));
        for (PlayerPrivilegeDTO privilege : privileges) {
            PlayerDTO p_player = PlayerDTO.select(privilege.getPlayerUUID());
            if (p_player == null) continue;
            GroupDTO group = Cache.instance.getGroup(privilege.getGroupId());
            Line line = Line.create();

            if (group != null) {
                line.append(groupTag);
            } else if (privilege.getAdmin()) {
                line.append(adminTag);
            } else {
                if (!privilege.getFlagValue(Flag.MOVE)) {
                    line.append(banTag);
                } else {
                    line.append(normalTag);
                }
            }

            Button prev = Button.createGreen("权限")
                    .setHoverText("配置成员权限")
                    .setExecuteCommand("/dominion privilege_info " + p_player.getLastKnownName() + " " + dominion.getName());
            Button remove = Button.createRed("移除")
                    .setHoverText("将此成员移出（变为访客）")
                    .setExecuteCommand("/dominion clear_privilege " + p_player.getLastKnownName() + " " + dominion.getName() + " b");

            if (!player.getUniqueId().equals(dominion.getOwner())) {
                boolean disable = false;
                if (group == null) {
                    if (privilege.getAdmin()) {
                        disable = true;
                    }
                } else {
                    if (group.getAdmin()) {
                        disable = true;
                    }
                }
                if (disable) {
                    prev.setDisabled("你不是领地主人，无法编辑管理员权限");
                    remove.setDisabled("你不是领地主人，无法移除管理员");
                }
            }
            if (group != null) {
                prev.setDisabled(String.format("此成员属于权限组 %s 无法单独编辑权限", group.getName()));
            }
            line.append(remove.build());
            line.append(prev.build());
            line.append(p_player.getLastKnownName());
            view.add(line);
        }
        view.showOn(player, page);
    }

    private static final TextComponent adminTag = Component.text("[A]", Style.style(TextColor.color(97, 97, 210)))
            .hoverEvent(Component.text("这是一个管理员"));
    private static final TextComponent normalTag = Component.text("[N]", Style.style(TextColor.color(255, 255, 255)))
            .hoverEvent(Component.text("这是一个普通成员"));
    private static final TextComponent banTag = Component.text("[B]", Style.style(TextColor.color(255, 67, 0)))
            .hoverEvent(Component.text("这是一个黑名单成员"));
    private static final TextComponent groupTag = Component.text("[G]", Style.style(TextColor.color(0, 185, 153)))
            .hoverEvent(Component.text("这个成员在一个权限组里"));
}
