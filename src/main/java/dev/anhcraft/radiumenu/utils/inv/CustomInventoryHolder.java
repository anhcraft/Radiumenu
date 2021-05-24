package dev.anhcraft.radiumenu.utils.inv;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CustomInventoryHolder implements InventoryHolder {
    private final long hash;
    private Inventory inventory;

    public CustomInventoryHolder(long hash) {
        this.hash = hash;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public long getHash() {
        return hash;
    }
}
