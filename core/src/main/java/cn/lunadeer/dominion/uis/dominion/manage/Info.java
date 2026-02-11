package cn.lunadeer.dominion.uis.dominion.manage;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.uis.TextUserInterface;
import cn.lunadeer.dominion.uis.MainMenu;
import cn.lunadeer.dominion.uis.dominion.DominionList;
import cn.lunadeer.dominion.uis.dominion.DominionManage;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.BorderRenderUtil;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.stui.ListView;
import cn.lunadeer.dominion.utils.stui.components.Line;
import cn.lunadeer.dominion.utils.stui.components.buttons.ListViewButton;
import org.bukkit.entity.Player;

import static cn.lunadeer.dominion.Dominion.defaultPermission;
import static cn.lunadeer.dominion.misc.Converts.toDominionDTO;
import static cn.lunadeer.dominion.utils.Misc.formatString;

public class Info {
    public static class SizeInfoTuiText extends ConfigurationPart {
        public String title = "{0} Size Info";
        public String button = "SIZE INFO";
        public String description = "Show dominion size information";
        public String ownerName = "This dominion is owned by {0}.";

        public String infoLWH = "Size: {0} x {1} x {2}";
        public String infoHeight = "Height: {0} ~ {1}";
        public String square = "Square: {0}";
        public String volume = "Volume: {0}";
    }

    public static ListViewButton button(Player player, String dominionName) {
        return (ListViewButton) new ListViewButton(TextUserInterface.sizeInfoTuiText.button) {
            @Override
            public void function(String pageStr) {
                show(player, dominionName);
            }
        }.needPermission(defaultPermission);
    }

    public static void show(Player player, String dominionName) {
        try {
            DominionDTO dominion = toDominionDTO(dominionName);
            String ownerName = CacheManager.instance.getPlayerName(dominion.getOwner());
            ListView view = ListView.create(10, button(player, dominionName));
            view.title(formatString(TextUserInterface.sizeInfoTuiText.title, dominion.getName()));

            boolean isOwner = false;
            if (player.getUniqueId().equals(dominion.getOwner())) {
                isOwner = true;
                BorderRenderUtil.showBorder(player, dominion);
            }

            if (!isOwner) {
                view.subtitle(Line.create().append(formatString(TextUserInterface.sizeInfoTuiText.ownerName, ownerName)));
            } else {
                view.subtitle(Line.create()
                        .append(MainMenu.button(player).build())
                        .append(DominionList.button(player).build())
                        .append(DominionManage.button(player, dominionName).build())
                        .append(TextUserInterface.sizeInfoTuiText.button).build()
                );
            }
            CuboidDTO cuboid = dominion.getCuboid();
            view.add(Line.create().append(SetSize.button(player, dominionName).build()))
                    .add(Line.create().append(formatString(TextUserInterface.sizeInfoTuiText.infoLWH, cuboid.xLength(), cuboid.yLength(), cuboid.zLength())))
                    .add(Line.create().append(formatString(TextUserInterface.sizeInfoTuiText.infoHeight, cuboid.y1(), cuboid.y2())))
                    .add(Line.create().append(formatString(TextUserInterface.sizeInfoTuiText.square, cuboid.getSquare())))
                    .add(Line.create().append(formatString(TextUserInterface.sizeInfoTuiText.volume, cuboid.getVolume())))
                    .showOn(player, 1);
        } catch (Exception e) {
            Notification.error(player, e.getMessage());
        }
    }
}
