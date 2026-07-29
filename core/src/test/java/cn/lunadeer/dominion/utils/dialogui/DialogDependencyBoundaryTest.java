package cn.lunadeer.dominion.utils.dialogui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogDependencyBoundaryTest {
    @Test
    void frameworkIsVersionAndBusinessIndependentAndMenusStayInUis() throws Exception {
        Path framework = Path.of("src/main/java/cn/lunadeer/dominion/utils/dialogui");
        assertTrue(Files.isDirectory(framework));
        String sources;
        try (var files = Files.walk(framework)) {
            sources = files.filter(path -> path.toString().endsWith(".java"))
                    .map(this::read).reduce("", String::concat);
        }
        for (String forbidden : new String[]{
                "net.minecraft.", "io.papermc.paper.dialog.", "cn.lunadeer.dominion.uis.",
                "cn.lunadeer.dominion.storage.", "cn.lunadeer.dominion.commands."
        }) {
            assertFalse(sources.contains(forbidden), "dialog framework dependency leaked: " + forbidden);
        }

        Path menus = Path.of("src/main/java/cn/lunadeer/dominion/uis/dialog");
        assertTrue(Files.isRegularFile(menus.resolve("DominionDialogUi.java")));
        assertTrue(Files.isRegularFile(menus.resolve("pages/DialogMenuController.java")));
        String menuSources;
        try (var files = Files.walk(menus)) {
            menuSources = files.filter(path -> path.toString().endsWith(".java"))
                    .map(this::read).reduce("", String::concat);
        }
        assertFalse(menuSources.contains("utils.chestui"));
        assertFalse(menuSources.contains("uis.chest"));
        assertFalse(menuSources.contains("WAIT_FOR_RESPONSE"));
        assertFalse(menuSources.contains("DialogClick"));
        Path oldModelPackage = menus.resolve("model");
        if (Files.exists(oldModelPackage)) {
            try (var files = Files.walk(oldModelPackage)) {
                assertFalse(files.anyMatch(Files::isRegularFile));
            }
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
