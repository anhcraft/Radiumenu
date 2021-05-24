package dev.anhcraft.radiumenu.utils.item;

import dev.anhcraft.radiumenu.utils.chat.Chat;
import dev.anhcraft.radiumenu.utils.XMaterial;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemEditor {
    protected ItemStack item;

    public ItemEditor(ItemStack item) {
        this.item = item;
    }

    public ItemEditor(String name, XMaterial type, int amount) {
        this(name, Objects.requireNonNull(type.parseMaterial()), amount, type.getData());
    }

    public ItemEditor(String name, Material type, int amount) {
        this.item = new ItemStack(type, amount);
        setName(name);
    }

    public ItemEditor(String name, Material type, int amount, short durability) {
        this.item = new ItemStack(type, amount);
        setName(name);
        setDurability(durability);
    }

    public String getName() {
        ItemMeta a = item.getItemMeta();
        return a.getDisplayName();
    }

    public ItemEditor setName(String name) {
        ItemMeta a = item.getItemMeta();
        a.setDisplayName(Chat.color(name));
        item.setItemMeta(a);
        return this;
    }

    public ItemEditor addEnchant(Enchantment enchant, int level) {
        ItemMeta a = item.getItemMeta();
        a.addEnchant(enchant, level, true);
        item.setItemMeta(a);
        return this;
    }

    public ItemEditor removeEnchant(Enchantment enchant) {
        ItemMeta a = item.getItemMeta();
        a.removeEnchant(enchant);
        item.setItemMeta(a);
        return this;
    }

    public Map<Enchantment, Integer> getEnchants() {
        ItemMeta a = item.getItemMeta();
        return a.getEnchants();
    }

    public int getEnchantLevel(Enchantment enchant) {
        ItemMeta a = item.getItemMeta();
        return a.getEnchantLevel(enchant);
    }

    public ItemEditor addLore(String text) {
        ItemMeta a = item.getItemMeta();
        List<String> lore = a.hasLore() ? a.getLore() : new ArrayList<>();
        lore.add(Chat.color(text));
        a.setLore(lore);
        item.setItemMeta(a);
        return this;
    }

    public ItemEditor addLore(List<String> texts) {
        ItemMeta a = item.getItemMeta();
        List<String> lore = a.hasLore() ? a.getLore() : new ArrayList<>();
        for (String b : texts) {
            lore.add(Chat.color(b));
        }
        a.setLore(lore);
        item.setItemMeta(a);
        return this;
    }

    public ItemEditor setLore(List<String> texts) {
        ItemMeta a = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        for (String b : texts) {
            lore.add(Chat.color(b));
        }
        a.setLore(lore);
        item.setItemMeta(a);
        return this;
    }

    public ItemEditor removeLore(int index) {
        ItemMeta a = item.getItemMeta();
        List<String> lore = a.getLore();
        lore.remove(index);
        a.setLore(lore);
        item.setItemMeta(a);
        return this;
    }

    public List<String> getLore() {
        ItemMeta a = item.getItemMeta();
        return a.getLore();
    }

    public ItemEditor addFlag(ItemFlag flag) {
        ItemMeta a = item.getItemMeta();
        a.addItemFlags(flag);
        item.setItemMeta(a);
        return this;
    }

    public ItemEditor removeFlag(ItemFlag flag) {
        ItemMeta a = item.getItemMeta();
        a.removeItemFlags(flag);
        item.setItemMeta(a);
        return this;
    }

    public Boolean hasFlag(ItemFlag flag) {
        ItemMeta a = item.getItemMeta();
        return a.hasItemFlag(flag);
    }

    public Set<ItemFlag> getFlags() {
        ItemMeta a = item.getItemMeta();
        return a.getItemFlags();
    }

    public ItemEditor setDurability(short durability) {
        item.setDurability(durability);
        return this;
    }

    public short getDurability() {
        return item.getDurability();
    }

    public ItemEditor setType(Material type) {
        item.setType(type);
        return this;
    }

    public Material getType() {
        return item.getType();
    }

    public ItemEditor setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public int getAmount() {
        return item.getAmount();
    }

    public ItemStack getItem() {
        return item;
    }
}
