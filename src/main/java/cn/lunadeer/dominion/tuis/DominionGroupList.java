package cn.lunadeer.dominion.tuis;

import cn.lunadeer.dominion.dtos.DominionDTO;
import cn.lunadeer.dominion.dtos.GroupDTO;
import cn.lunadeer.dominion.dtos.PlayerDTO;
import cn.lunadeer.dominion.dtos.PlayerPrivilegeDTO;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.stui.ListView;
import cn.lunadeer.minecraftpluginutils.stui.components.Button;
import cn.lunadeer.minecraftpluginutils.stui.components.Line;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static cn.lunadeer.dominion.commands.Apis.playerOnly;
import static cn.lunadeer.dominion.tuis.Apis.getDominionNameArg_1;
import static cn.lunadeer.dominion.tuis.Apis.noAuthToManage;

public class DominionGroupList {

    public static void show(CommandSender sender, String[] args) {
        Player player = playerOnly(sender);
        if (player == null) return;
        DominionDTO dominion = getDominionNameArg_1(player, args);
        if (dominion == null) {
            Notification.error(sender, "你不在任何领地内，请指定领地名称 /dominion group_list <领地名称>");
            return;
        }
        if (noAuthToManage(player, dominion)) return;
        int page = Apis.getPage(args, 2);
        List<GroupDTO> groups = GroupDTO.selectByDominionId(dominion.getId());
        ListView view = ListView.create(10, "/dominion group_list " + dominion.getName());
        view.title("权限组列表");
        view.navigator(
                Line.create()
                        .append(Button.create("主菜单").setExecuteCommand("/dominion menu").build())
                        .append(Button.create("我的领地").setExecuteCommand("/dominion list").build())
                        .append(Button.create("管理界面").setExecuteCommand("/dominion manage " + dominion.getName()).build())
                        .append("权限组列表")
        );

        Button create_btn = Button.createGreen("创建权限组")
                .setHoverText("创建一个新的权限组")
                .setExecuteCommand("/dominion cui_create_group " + dominion.getName());
        view.add(new Line().append(create_btn.build()));

        for (GroupDTO group : groups) {
            Line line = new Line();
            Button del = Button.createRed("删除")
                    .setHoverText("删除权限组 " + group.getName())
                    .setExecuteCommand("/dominion delete_group " + dominion.getName() + " " + group.getName());
            Button edit = Button.create("编辑")
                    .setHoverText("编辑权限组 " + group.getName())
                    .setExecuteCommand("/dominion group_manage " + dominion.getName() + " " + group.getName());
            Button add = Button.createGreen("+")
                    .setHoverText("添加成员到权限组 " + group.getName())
                    .setExecuteCommand("/dominion select_member_add_group " + dominion.getName() + " " + group.getName() + " " + page);
            line.append(del.build()).append(edit.build()).append(group.getName()).append(add.build());
            view.add(line);
            List<PlayerPrivilegeDTO> players = PlayerPrivilegeDTO.selectByGroupId(group.getId());
            for (PlayerPrivilegeDTO playerPrivilege : players) {
                PlayerDTO p = PlayerDTO.select(playerPrivilege.getPlayerUUID());
                if (p == null) continue;
                Button remove = Button.createRed("移出权限组")
                        .setHoverText("把 " + p.getLastKnownName() + " 移出权限组 " + group.getName())
                        .setExecuteCommand("/dominion group_remove_member " + dominion.getName() + " " + group.getName() + " " + p.getLastKnownName() + " " + page);
                Line playerLine = new Line();
                playerLine.append(remove.build()).append(" | " + p.getLastKnownName());
            }
        }
        view.showOn(player, page);
    }

}
