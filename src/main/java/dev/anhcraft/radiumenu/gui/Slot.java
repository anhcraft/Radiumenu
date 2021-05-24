package dev.anhcraft.radiumenu.gui;

import java.util.*;

import dev.anhcraft.radiumenu.Radiumenu;
import dev.anhcraft.radiumenu.integrations.PlaceholderIntegration;
import dev.anhcraft.radiumenu.utils.item.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Slot {
    private ItemStack item;
    private double money;
    private int points;
    private final List<String> commands;
    private boolean closeGUI;
    private boolean rewardItem;
    private final List<ItemStack> tradeItems;
    private int limitTransaction;
    private final Map<UUID, Integer> transactionCounter;
    private String skullOwner;
    private boolean permissionRequired;

    public Slot(ItemStack item) {
        this.item = item;
        this.commands = new ArrayList<>();
        this.tradeItems = new ArrayList<>();
        this.transactionCounter = new HashMap<>();
        this.skullOwner = "";
    }

    public Slot duplicate() {
        Slot s = new Slot(item.clone());
        s.money = money;
        s.points = points;
        s.commands.addAll(commands);
        s.closeGUI = closeGUI;
        s.rewardItem = rewardItem;
        s.tradeItems.addAll(tradeItems);
        s.limitTransaction = limitTransaction;
        s.transactionCounter.putAll(transactionCounter);
        s.skullOwner = skullOwner;
        s.permissionRequired = permissionRequired;
        return s;
    }

    public ItemStack getItem(Player player) {
        if (!ItemUtils.isNull(this.item)) {
            if (Radiumenu.INTEGRATIONS[2].isHooked()) {
                return ((PlaceholderIntegration) Radiumenu.INTEGRATIONS[2]).format(this.item.clone(), player);
            }
            return this.item.clone();
        }
        return Radiumenu.defaultGuiItem;
    }

    public void setItem(ItemStack item) {
        this.item = ItemUtils.isNull(item) ? Radiumenu.defaultGuiItem : item;
    }

    public double getMoney() {
        return this.money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public int getPoints() {
        return this.points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public List<String> getCommands() {
        return this.commands;
    }

    public boolean isCloseGUI() {
        return this.closeGUI;
    }

    public void setCloseGUI(boolean closeGUI) {
        this.closeGUI = closeGUI;
    }

    public boolean isRewardItem() {
        return this.rewardItem;
    }

    public void setRewardItem(boolean rewardItem) {
        this.rewardItem = rewardItem;
    }

    public ItemStack getRawItem() {
        return this.item;
    }

    public List<ItemStack> getTradeItems() {
        return this.tradeItems;
    }

    public Map<UUID, Integer> getTransactionCounter() {
        return this.transactionCounter;
    }

    public int getLimitTransaction() {
        return this.limitTransaction;
    }

    public void setLimitTransaction(int limitTransaction) {
        this.limitTransaction = limitTransaction;
    }

    public int getTotalTransactions() {
        int i = 0;
        for (int n : transactionCounter.values()) {
            i += n;
        }
        return i;
    }

    public String getSkullOwner() {
        return this.skullOwner;
    }

    public void setSkullOwner(String skullOwner) {
        this.skullOwner = skullOwner;
    }

    public boolean isPermissionRequired() {
        return permissionRequired;
    }

    public void setPermissionRequired(boolean permissionRequired) {
        this.permissionRequired = permissionRequired;
    }
}
