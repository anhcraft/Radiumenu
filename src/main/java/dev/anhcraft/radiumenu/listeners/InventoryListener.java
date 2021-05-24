package dev.anhcraft.radiumenu.listeners;

import dev.anhcraft.radiumenu.utils.inv.CustomInventoryHolder;
import dev.anhcraft.radiumenu.utils.inv.SlotCallback;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class InventoryListener implements Listener {
    private static final Map<Long, Map<Integer, SlotCallback>> data = new HashMap<>();

    public static void registerSlot(CustomInventoryHolder invHolder, int slot, SlotCallback run) {
        Map<Integer, SlotCallback> map = data.computeIfAbsent(invHolder.getHash(), k -> new HashMap<>());
        map.put(slot, run);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        if(inv != null && inv.getHolder() instanceof CustomInventoryHolder) {
            Player player = (Player) event.getWhoClicked();
            ClickType type = event.getClick();
            Map<Integer, SlotCallback> map = data.get(((CustomInventoryHolder) inv.getHolder()).getHash());
            if(map != null) {
                SlotCallback callback = map.get(event.getRawSlot());
                if(callback != null) {
                    event.setCancelled(true);
                    event.setResult(Event.Result.DENY);
                    callback.run(player, inv, event.getAction(), event.getRawSlot(), event.getCurrentItem(), type);
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void close(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof CustomInventoryHolder) {
            CustomInventoryHolder holder = (CustomInventoryHolder) event.getInventory().getHolder();
            data.remove(holder.getHash());
        }
    }
}
