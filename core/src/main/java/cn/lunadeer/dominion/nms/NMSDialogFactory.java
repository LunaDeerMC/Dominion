package cn.lunadeer.dominion.nms;

import cn.lunadeer.dominion.utils.dialogui.DialogEncodingResult;
import cn.lunadeer.dominion.utils.dialogui.DialogSessionContext;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.entity.Player;

/**
 * Version-specific encoder and packet sender for Minecraft dialogs.
 */
public interface NMSDialogFactory {
    boolean isSupported();

    DialogEncodingResult validate(DialogSpec dialog);

    DialogEncodingResult show(Player player, DialogSpec dialog, DialogSessionContext context);

    void close(Player player);
}
