package cn.lunadeer.dominion.utils.scui;

import cn.lunadeer.dominion.utils.scui.configuration.ListViewConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class ChestListView extends ChestView {

    public ChestListView(@NotNull Player viewOwner) {
        super(viewOwner);
    }

    private int pageSize = 45; // Default page size for a chest view
    private int currentPage = 1;
    private char itemSymbol = 'i';
    private boolean layoutSet = false;
    private ListViewConfiguration configCopy;
    private final List<ChestButton> items = new ArrayList<>();

    public ChestListView applyListConfiguration(@NotNull ListViewConfiguration config, int currentPage) {
        this.configCopy = config;
        // Validate the current page number
        if (currentPage < 1) {
            throw new IllegalArgumentException("Page number must be greater than 0.");
        }
        this.currentPage = currentPage;
        // Validate the layout and symbols
        super.setLayout(config.layout);
        this.itemSymbol = config.itemSymbol.charAt(0);
        if (!this.getLayout().contains(String.valueOf(itemSymbol))) {
            throw new IllegalArgumentException("Layout must contain the item symbol: " + itemSymbol);
        }
        if (!this.getLayout().contains(String.valueOf(config.previewButton.getSymbol()))) {
            throw new IllegalArgumentException("Layout must contain the preview button symbol: " + config.previewButton.getSymbol());
        }
        if (!this.getLayout().contains(String.valueOf(config.nextButton.getSymbol()))) {
            throw new IllegalArgumentException("Layout must contain the next button symbol: " + config.nextButton.getSymbol());
        }
        this.pageSize = (int) this.getLayout().chars().filter(ch -> ch == itemSymbol).count();
        this.layoutSet = true;
        return this;
    }

    public ChestListView addItem(@NotNull ChestButton item) {
        items.add(item);
        return this;
    }

    public void clearCurrentItemButtons() {
        for (int i = 0; i < this.getLayout().length(); i++) {
            if (this.getLayout().charAt(i) == itemSymbol) {
                this.removeButton(i);
            }
        }
    }

    @Override
    public void open() {
        if (!layoutSet) {
            throw new IllegalStateException("List layout must be set before opening the view.");
        }
        this.clearCurrentItemButtons();
        int itemSymbolPosition = -1; // Reset itemSymbolPosition for the new page
        // setButton(int slot, @NotNull ChestButton button) from the first available itemSymbolPosition
        for (int idx = 0; idx < items.size(); idx++) {
            if (items.get(idx).getSymbol() != itemSymbol) continue; // Skip items that do not match the itemSymbol
            // skip not this page items
            if (idx < (currentPage - 1) * pageSize) {
                continue;
            }
            if (idx >= currentPage * pageSize) {
                break; // No more items for this page
            }
            // find next available itemSymbolPosition
            itemSymbolPosition = this.getLayout().indexOf(itemSymbol, itemSymbolPosition + 1);
            if (itemSymbolPosition >= this.getLayout().length()) {
                throw new IndexOutOfBoundsException("Not enough space in the layout for items.");
            }
            // set button at itemSymbolPosition
            this.setButton(itemSymbolPosition, items.get(idx));
        }
        int totalPages = (int) Math.ceil((double) items.size() / pageSize);
        // Set Preview Button
        this.setButton(configCopy.previewButton.getSymbol(),
                new ChestButton(configCopy.previewButton) {
                    @Override
                    public void onClick(ClickType type) {
                        if (currentPage == 1) {
                            return; // No previous page
                        }
                        applyListConfiguration(configCopy, currentPage - 1);
                        open();
                    }
                }.setLoreArgs(currentPage, totalPages));
        // Set Next Button
        this.setButton(configCopy.nextButton.getSymbol(),
                new ChestButton(configCopy.nextButton) {
                    @Override
                    public void onClick(ClickType type) {
                        if (currentPage * pageSize >= items.size()) {
                            return; // No next page
                        }
                        applyListConfiguration(configCopy, currentPage + 1);
                        open();
                    }
                }.setLoreArgs(currentPage, totalPages));
        super.open();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

}
