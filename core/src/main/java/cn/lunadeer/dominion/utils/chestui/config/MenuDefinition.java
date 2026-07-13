package cn.lunadeer.dominion.utils.chestui.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public final class MenuDefinition {
    private final int rows;
    private final Map<String, List<Integer>> slots;
    private final Map<String, ItemAppearance> appearances;

    private MenuDefinition(int rows, Map<String, List<Integer>> slots,
                           Map<String, ItemAppearance> appearances) {
        this.rows = rows;
        this.slots = slots;
        this.appearances = appearances;
    }

    public int rows() {
        return rows;
    }

    public List<Integer> slots(String element) {
        return slots.getOrDefault(element, List.of());
    }

    public int firstSlot(String element) {
        List<Integer> values = slots(element);
        return values.isEmpty() ? -1 : values.get(0);
    }

    public Set<String> elements() {
        return slots.keySet();
    }

    public ItemAppearance appearance(String element) {
        return appearances.getOrDefault(element,
                appearances.getOrDefault("default", new ItemAppearance(org.bukkit.Material.STONE, 1, null, false, false)));
    }

    static MenuDefinition read(ConfigurationSection section) {
        if (section == null) throw new IllegalArgumentException("missing menu section");
        List<String> layout = section.getStringList("layout");
        if (layout.isEmpty() || layout.size() > 6) throw new IllegalArgumentException("layout must contain 1-6 rows");
        Map<Character, String> symbols = new HashMap<>();
        ConfigurationSection symbolSection = section.getConfigurationSection("symbols");
        if (symbolSection == null) throw new IllegalArgumentException("missing symbols section");
        for (String key : symbolSection.getKeys(false)) {
            if (key.length() != 1) throw new IllegalArgumentException("symbol must be one character: " + key);
            symbols.put(key.charAt(0), symbolSection.getString(key, ""));
        }
        Map<String, List<Integer>> slots = new LinkedHashMap<>();
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            if (line.length() != 9) throw new IllegalArgumentException("layout row " + (row + 1) + " must contain 9 characters");
            for (int column = 0; column < 9; column++) {
                char symbol = line.charAt(column);
                if (symbol == ' ') continue;
                String element = symbols.get(symbol);
                if (element == null || element.isBlank()) {
                    throw new IllegalArgumentException("unmapped symbol: " + symbol);
                }
                slots.computeIfAbsent(element, ignored -> new ArrayList<>()).add(row * 9 + column);
            }
        }
        for (String required : section.getStringList("required-elements")) {
            if (slots.getOrDefault(required, List.of()).isEmpty()) {
                throw new IllegalArgumentException("missing required element: " + required);
            }
        }
        Map<String, ItemAppearance> appearances = new HashMap<>();
        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                appearances.put(key, ItemAppearance.read(items.getConfigurationSection(key)));
            }
        }
        return new MenuDefinition(layout.size(), Map.copyOf(slots), Map.copyOf(appearances));
    }
}
