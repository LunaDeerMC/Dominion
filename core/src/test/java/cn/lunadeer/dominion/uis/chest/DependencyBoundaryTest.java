package cn.lunadeer.dominion.uis.chest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyBoundaryTest {
    @Test
    void chestControllersAndBusinessCommandsStayDecoupled() throws Exception {
        Path root = Path.of("src/main/java/cn/lunadeer/dominion");
        String chestSources;
        try (var files = Files.walk(root.resolve("uis/chest"))) {
            chestSources = files.filter(path -> path.toString().endsWith(".java"))
                    .map(this::read).reduce("", String::concat);
        }
        for (String forbidden : new String[]{".commands.", ".doos.", ".cache.", ".managers.", ".utils.scui."}) {
            assertFalse(chestSources.contains(forbidden), "forbidden chest dependency: " + forbidden);
        }
        String frameworkSources;
        try (var files = Files.walk(root.resolve("utils/chestui"))) {
            frameworkSources = files.filter(path -> path.toString().endsWith(".java"))
                    .map(this::read).reduce("", String::concat);
        }
        for (String forbidden : new String[]{"cn.lunadeer.dominion.uis.", "cn.lunadeer.dominion.api.",
                "cn.lunadeer.dominion.providers.", "cn.lunadeer.dominion.configuration.Configuration"}) {
            assertFalse(frameworkSources.contains(forbidden), "framework depends on business layer: " + forbidden);
        }
        try (var files = Files.walk(root.resolve("commands"))) {
            String commandSources = files.filter(path -> path.toString().endsWith(".java"))
                    .map(this::read).reduce("", String::concat);
            assertFalse(commandSources.contains("cn.lunadeer.dominion.uis"));
        }
        assertTrue(Files.isRegularFile(Path.of("../languages/chest-ui/layout.yml")));
        assertTrue(Files.isRegularFile(Path.of("../languages/chest-ui/texts/en_us.yml")));
        Path oldResources = Path.of("src/main/resources/chest-ui");
        if (Files.exists(oldResources)) {
            try (var files = Files.walk(oldResources)) {
                assertFalse(files.anyMatch(Files::isRegularFile));
            }
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
