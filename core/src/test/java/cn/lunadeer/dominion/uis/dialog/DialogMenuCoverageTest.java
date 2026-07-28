package cn.lunadeer.dominion.uis.dialog;

import cn.lunadeer.dominion.uis.dialog.pages.DialogMenuId;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogMenuCoverageTest {
    @Test
    void dialogOwnsEveryRequiredBusinessRouteWithoutChestDependencies() throws Exception {
        Set<String> requiredRoutes = Set.of(
                "MAIN", "DOMINION_LIST", "ALL_DOMINIONS", "CHILD_LIST", "DASHBOARD",
                "AREA", "PERMISSIONS", "PEOPLE", "APPEARANCE", "OWNERSHIP", "RESIZE",
                "ENV_FLAGS", "GUEST_FLAGS", "FLAG_LIST", "MEMBER_LIST", "MEMBER_DETAIL",
                "MEMBER_FLAGS", "GROUP_LIST", "GROUP_DETAIL", "GROUP_FLAGS",
                "GROUP_MEMBER_PICKER", "PLAYER_PICKER", "TRANSFER_PICKER", "TEMPLATE_LIST",
                "TEMPLATE_DETAIL", "TEMPLATE_FLAGS", "TEMPLATE_PICKER", "TITLE_LIST",
                "COPY_SOURCE", "COPY_TYPE", "ADMIN_PLAYER_DOMINIONS", "PLAYER_DOMINIONS",
                "CONFIRM"
        );
        Set<String> dialogRoutes = java.util.Arrays.stream(DialogMenuId.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(requiredRoutes, dialogRoutes);

        String controller = Files.readString(Path.of(
                "src/main/java/cn/lunadeer/dominion/uis/dialog/pages/DialogMenuController.java"));
        for (DialogMenuId id : DialogMenuId.values()) {
            assertTrue(controller.contains(id.name()), "Dialog controller does not route " + id);
        }
    }
}
