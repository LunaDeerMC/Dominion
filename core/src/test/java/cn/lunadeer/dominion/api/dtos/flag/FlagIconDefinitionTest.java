package cn.lunadeer.dominion.api.dtos.flag;

import cn.lunadeer.dominion.utils.dialogui.DialogSpritePath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagIconDefinitionTest {
    @Test
    void builtInFlagsUseValidVanillaDialogSprites() {
        Flags.getAllFlags().forEach(flag -> {
            assertTrue(flag.getIcon().startsWith("minecraft:"), flag.getFlagName());
            assertTrue(DialogSpritePath.isValid(flag.getIcon()), flag.getFlagName());
        });
    }

    @Test
    void builtInFlagGroupsUseValidVanillaDialogSprites() {
        FlagGroups.defaultEnvironmentGroups().forEach(this::assertVanillaIcon);
        FlagGroups.defaultPrivilegeGroups().forEach(this::assertVanillaIcon);
    }

    private void assertVanillaIcon(FlagGroup<?> group) {
        assertTrue(group.getIcon().startsWith("minecraft:"), group.getId());
        assertTrue(DialogSpritePath.isValid(group.getIcon()), group.getId());
    }
}
