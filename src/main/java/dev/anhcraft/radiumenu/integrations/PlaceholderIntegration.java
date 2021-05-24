package dev.anhcraft.radiumenu.integrations;

import java.util.ArrayList;
import java.util.List;

import me.clip.placeholderapi.PlaceholderAPI;
import dev.anhcraft.radiumenu.utils.item.ItemEditor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PlaceholderIntegration extends Integration {
    @Override
    public boolean init(JavaPlugin plugin) {
        return plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public ItemStack format(ItemStack item, Player player) {
        if (item == null) return null;
        ItemEditor i = new ItemEditor(item);
        if (i.getName() != null) {
            i.setName(PlaceholderAPI.setPlaceholders(player, i.getName()));
        }
        if (i.getLore() != null) {
            List<String> list = new ArrayList<>();
            for (String str : i.getLore()) {
                list.add(PlaceholderAPI.setPlaceholders(player, str));
            }
            i.setLore(list);
        }
        return i.getItem();
    }
}

