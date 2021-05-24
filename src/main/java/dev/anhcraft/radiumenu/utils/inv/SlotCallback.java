/*
 * Decompiled with CFR 0.145.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 */
package dev.anhcraft.radiumenu.utils.inv;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface SlotCallback {
    void run(Player player, Inventory inventory, InventoryAction inventoryAction, int slot, ItemStack item, ClickType clickType);
}
