package dev.anhcraft.radiumenu.utils.inv;

import java.util.ArrayList;
import java.util.List;

import dev.anhcraft.jvmkit.utils.MathUtil;
import dev.anhcraft.radiumenu.listeners.InventoryListener;
import dev.anhcraft.radiumenu.utils.chat.Chat;
import dev.anhcraft.radiumenu.utils.item.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryEditor {
    private final CustomInventoryHolder invHolder;
    private final Inventory inv;
    public SlotCallback[] slotCallbacks;

    public InventoryEditor(String name, int size) {
        size = MathUtil.nextMultiple(size, 9);
        invHolder = new CustomInventoryHolder(System.currentTimeMillis());
        inv = Bukkit.getServer().createInventory(invHolder, size, Chat.color(name));
        invHolder.setInventory(inv);
        slotCallbacks = new SlotCallback[size];
    }

    public InventoryEditor set(int index, ItemStack item) {
        inv.setItem(index, item);
        return this;
    }

    public InventoryEditor set(int index, ItemStack item, SlotCallback run) {
        inv.setItem(index, item);
        slotCallbacks[index] = run;
        return this;
    }

    public ItemStack get(int column, int row) {
        return inv.getItem(column * row);
    }

    public ItemStack get(int index) {
        return inv.getItem(index);
    }

    public InventoryEditor remove(int index) {
        inv.setItem(index, null);
        slotCallbacks[index] = null;
        return this;
    }

    public InventoryEditor remove(ItemStack item) {
        for (int i = 0; i < inv.getSize(); ++i) {
            if (!ItemUtils.compare(item, get(i))) continue;
            remove(i);
        }
        return this;
    }

    public InventoryEditor addItem(ItemStack item) {
        int emptySlot = inv.firstEmpty();
        if (emptySlot != -1) {
            inv.setItem(emptySlot, item);
        }
        return this;
    }

    public InventoryEditor addItem(ItemStack item, SlotCallback run) {
        int emptySlot = inv.firstEmpty();
        if (emptySlot != -1) {
            inv.setItem(emptySlot, item);
            slotCallbacks[emptySlot] = run;
        }
        return this;
    }

    public InventoryEditor addUniqueItem(ItemStack item) {
        boolean has = false;
        for (ItemStack i : inv) {
            if (!ItemUtils.compare(i, item)) continue;
            has = true;
            break;
        }
        if (!has) {
            addItem(item);
        }
        return this;
    }

    public InventoryEditor addUniqueItem(ItemStack item, SlotCallback run) {
        boolean has = false;
        for (ItemStack i : inv) {
            if (!ItemUtils.compare(i, item)) continue;
            has = true;
            break;
        }
        if (!has) {
            addItem(item, run);
        }
        return this;
    }

    public InventoryEditor fill(ItemStack item) {
        for (int i = 0; i < inv.getSize(); ++i) {
            int empty = inv.firstEmpty();
            if (empty == -1) continue;
            set(i, item);
        }
        return this;
    }

    public InventoryEditor fill(ItemStack item, SlotCallback run) {
        for (int i = 0; i < inv.getSize(); ++i) {
            int empty = inv.firstEmpty();
            if (empty == -1) continue;
            set(i, item, run);
        }
        return this;
    }

    public InventoryEditor clear() {
        inv.clear();
        slotCallbacks = new SlotCallback[inv.getSize()];
        return this;
    }

    public InventoryEditor open(Player player) {
        for (int i = 0; i < slotCallbacks.length; ++i) {
            SlotCallback handler = slotCallbacks[i];
            if (handler == null) continue;
            InventoryListener.registerSlot(invHolder, i, handler);
        }
        player.openInventory(inv);
        return this;
    }

    public Inventory getInventory() {
        return inv;
    }

    public InventoryEditor update(Player player) {
        player.getInventory().setContents(inv.getContents());
        player.updateInventory();
        return this;
    }

    public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack i : inv.getContents()) {
            if (ItemUtils.isNull(i)) continue;
            items.add(i);
        }
        return items;
    }
}
