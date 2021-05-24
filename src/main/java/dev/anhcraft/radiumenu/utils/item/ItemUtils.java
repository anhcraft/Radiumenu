package dev.anhcraft.radiumenu.utils.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemUtils {
    public static boolean isNull(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getType().toString().endsWith("_AIR");
    }

    public static boolean isNull(Material material) {
        return material == null || material == Material.AIR || material.toString().endsWith("_AIR");
    }

    public static boolean compare(ItemStack a, ItemStack b) {
        if (ItemUtils.isNull(a)) {
            return ItemUtils.isNull(b);
        }
        if (ItemUtils.isNull(b)) {
            return ItemUtils.isNull(a);
        }
        return a.equals(b);
    }

    public static boolean compare(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() == b.size()) {
            int i = 0;
            for (ItemStack ai : a) {
                if (ItemUtils.compare(ai, b.get(i++))) continue;
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean compare(ItemStack[] a, ItemStack[] b) {
        if (a.length == b.length) {
            int i = 0;
            for (ItemStack ai : a) {
                if (ItemUtils.compare(ai, b[i++])) continue;
                return false;
            }
            return true;
        }
        return false;
    }

    public static ItemStack clone(ItemStack item) {
        return ItemUtils.isNull(item) ? null : item.clone();
    }
}

