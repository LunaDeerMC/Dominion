package cn.lunadeer.dominion.utils.scui;

import cn.lunadeer.dominion.utils.ColorParser;
import cn.lunadeer.dominion.utils.Notification;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.lunadeer.dominion.managers.HooksManager.setPlaceholder;
import static cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager.attachTag;
import static cn.lunadeer.dominion.utils.scui.ChestUserInterfaceManager.hasTag;

public class ChestView {

    private final Player viewOwner;
    private String title;
    protected final Map<Integer, ChestButton> buttons = new HashMap<>();
    private String layout = "";

    public ChestView(@NotNull Player viewOwner) {
        this.viewOwner = viewOwner;
        this.title = "Default Title"; // Set a default title
        ChestUserInterfaceManager.getInstance().registerView(this);
    }

    /**
     * Sets the layout of the chest view.
     * <p>
     * The layout is a list of strings, each representing a row in the chest GUI.
     * Each string must have exactly 9 characters, and the list can have up to 6 rows.
     *
     * @param layout a list of strings representing the layout rows
     * @return the current ChestView instance for method chaining
     * @throws IllegalArgumentException if the layout is empty, has more than 6 rows,
     *                                  or any row does not have exactly 9 characters
     */
    public ChestView setLayout(@NotNull List<String> layout) {
        StringBuilder builder = new StringBuilder();
        if (layout.isEmpty()) {
            throw new IllegalArgumentException("Layout cannot be empty.");
        }
        for (String row : layout) {
            builder.append(row);
        }
        return setLayout(builder.toString());
    }

    /**
     * Sets the layout of the chest view using an array of strings.
     * <p>
     * Each string in the array represents a row in the chest GUI.
     * Each string must have exactly 9 characters, and the array can have up to 6 rows.
     *
     * @param layout an array of strings representing the layout rows
     * @return the current ChestView instance for method chaining
     * @throws IllegalArgumentException if the layout is empty, has more than 6 rows,
     *                                  or any row does not have exactly 9 characters
     */
    public ChestView setLayout(@NotNull String... layout) {
        return setLayout(String.join("", layout));
    }

    /**
     * Sets the layout of the chest view using a single string.
     * <p>
     * The layout string must have a length that is a multiple of 9 (each 9 characters represent a row),
     * and can have up to 6 rows (maximum 54 characters).
     * Each character in the string represents a slot in the chest GUI.
     *
     * @param layout a string representing the layout rows, must be a multiple of 9 characters, up to 54
     * @return the current ChestView instance for method chaining
     * @throws IllegalArgumentException if the layout is empty, not a multiple of 9, or has more than 6 rows
     */
    public ChestView setLayout(@NotNull String layout) {
        if (layout.isEmpty()) {
            throw new IllegalArgumentException("Layout cannot be empty.");
        }
        if (layout.length() % 9 != 0) {
            throw new IllegalArgumentException("Layout must be a multiple of 9 characters.");
        }
        if (layout.length() / 9 > 6) {
            throw new IllegalArgumentException("Layout cannot have more than 6 rows.");
        }
        this.layout = layout;
        return this;
    }

    /**
     * Sets the title of the chest view.
     *
     * @param title the new title for the chest view, must not be empty
     * @return the current ChestView instance for method chaining
     * @throws IllegalArgumentException if the title is null or empty
     */
    public ChestView setTitle(@NotNull String title) {
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty.");
        }
        this.title = title;
        return this;
    }

    /**
     * Clears all buttons from the chest view.
     *
     * @return the current ChestView instance for method chaining
     */
    public ChestView clearButtons() {
        buttons.clear();
        return this;
    }

    /**
     * Clears the layout of the chest view.
     *
     * @return the current ChestView instance for method chaining
     */
    public ChestView clearLayout() {
        layout = "";
        return this;
    }

    /**
     * Sets a button in the chest view by searching for the first occurrence of the given symbol in the layout.
     * <p>
     * The method looks for the specified symbol in the layout rows. If found, it calculates the corresponding slot index
     * and sets the button at that slot. If the symbol is not found, an exception is thrown.
     *
     * @param symbol the character symbol to search for in the layout
     * @param button the ChestButton to set at the found slot
     * @return the current ChestView instance for method chaining
     * @throws IllegalStateException if the layout has not been set
     */
    public ChestView setButton(char symbol, @NotNull ChestButton button) {
        if (layout.isEmpty()) {
            throw new IllegalStateException("Layout must be set before adding buttons with symbols.");
        }
        for (int i = 0; i < layout.length(); i++) {
            if (layout.charAt(i) == symbol) {
                setButton(i, button);
            }
        }
        return this;
    }

    /**
     * Sets a button at the specified slot index in the chest view.
     *
     * @param slot   The slot index (0-53) where the button will be placed.
     * @param button The ChestButton to set at the specified slot.
     * @return The current ChestView instance for method chaining.
     * @throws IllegalArgumentException if the slot is out of bounds (not between 0 and 53).
     */
    public ChestView setButton(int slot, @NotNull ChestButton button) {
        if (slot < 0 || slot >= 54) {
            throw new IllegalArgumentException("Slot must be between 0 and 53.");
        }
        buttons.put(slot, button);
        return this;
    }

    /**
     * Sets a button at the specified row and column in the chest view.
     *
     * @param row    Row index (0-5)
     * @param column Column index (0-8)
     * @param button The ChestButton to set at the specified position.
     * @return The current ChestView instance for method chaining.
     */
    public ChestView setButton(int row, int column, @NotNull ChestButton button) {
        if (row < 0 || row >= 6 || column < 0 || column >= 9) {
            throw new IllegalArgumentException("Row must be between 0 and 5 and column must be between 0 and 8.");
        }
        return setButton(row * 9 + column, button);
    }

    /**
     * Sets multiple buttons in the chest view using a map of slot indices to ChestButton instances.
     *
     * @param buttons a map where the key is the slot index (0-53) and the value is the ChestButton to set
     * @return the current ChestView instance for method chaining
     */
    public ChestView setButtons(@NotNull Map<Integer, ChestButton> buttons) {
        for (Map.Entry<Integer, ChestButton> entry : buttons.entrySet()) {
            setButton(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public ChestView removeButton(int slot) {
        buttons.remove(slot);
        return this;
    }

    /**
     * Opens the chest view for the player. If the current inventory already has the custom tag,
     * it refreshes the view; otherwise, it creates a new inventory.
     */
    public void open() {
        ItemStack firstItem = viewOwner.getOpenInventory().getItem(0);
        if (hasTag(firstItem) && viewOwner.getOpenInventory().countSlots() == (layout.isEmpty() ? 54 : layout.length())) {
            refresh(viewOwner.getOpenInventory());
        } else {
            create();
        }
    }

    /**
     * Closes the chest view for the player if the current inventory has the custom tag.
     */
    public void close() {
        ItemStack firstItem = viewOwner.getOpenInventory().getItem(0);
        if (hasTag(firstItem)) {
            viewOwner.getOpenInventory().close();
        }
    }

    protected void create() {
        Inventory view = Bukkit.createInventory(viewOwner, layout.isEmpty() ? 54 : layout.length(), "");
        InventoryView inventoryView = viewOwner.openInventory(view);
        if (inventoryView == null) {
            throw new IllegalStateException("Failed to open inventory for player: " + viewOwner.getName());
        }
        refresh(inventoryView);
    }

    protected void refresh(@NotNull InventoryView view) {
        try {
            title = setPlaceholder(viewOwner, title);
            title = ColorParser.getBukkitType(title);
            view.setTitle(title);
            for (Map.Entry<Integer, ChestButton> entry : buttons.entrySet()) {
                int slot = entry.getKey();
                ChestButton button = entry.getValue();
                if (button != null) {
                    ItemStack item = attachTag(viewOwner.getUniqueId(), button.build(viewOwner));
                    view.setItem(slot, item);
                } else {
                    view.setItem(slot, null);
                }
            }
            // Fill empty slots with placeholder items
            int totalSlots = layout.isEmpty() ? 54 : layout.length();
            for (int i = 0; i < totalSlots; i++) {
                if (view.getItem(i) != null) continue; // Skip if item is already set
                view.setItem(i, ChestUserInterfaceManager.PLACE_HOLDER_ITEM);
            }
        } catch (Exception e) {
            view.close();
            Notification.error(viewOwner, e);
        }
    }

    public UUID getViewId() {
        return viewOwner.getUniqueId();
    }

    public Player getViewOwner() {
        return viewOwner;
    }

    public String getTitle() {
        return title;
    }

    public String getLayout() {
        return layout;
    }

    public void handleClick(Integer slot, ClickType type) {
        ChestButton button = buttons.get(slot);
        if (button != null) {
            button.onClick(type);
        } else {
            viewOwner.sendMessage("§cNo action assigned to this slot.");
        }
    }


}
