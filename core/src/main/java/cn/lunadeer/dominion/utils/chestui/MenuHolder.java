package cn.lunadeer.dominion.utils.chestui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

final class MenuHolder implements InventoryHolder {
    private final UUID playerId;
    private final long revision;
    private Inventory inventory;

    MenuHolder(UUID playerId, long revision) {
        this.playerId = playerId;
        this.revision = revision;
    }

    UUID playerId() { return playerId; }
    long revision() { return revision; }
    void inventory(Inventory value) { inventory = value; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}
