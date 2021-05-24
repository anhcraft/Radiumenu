package dev.anhcraft.radiumenu.integrations;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultIntegration extends Integration {
    private Economy eco;

    @Override
    public boolean init(JavaPlugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }
        RegisteredServiceProvider<?> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        eco = (Economy) rsp.getProvider();
        return eco != null;
    }

    public double enough(OfflinePlayer player, double amount) {
        double t = eco.getBalance(player);
        return t - amount >= 0.0 ? 0.0 : amount - t;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        double t = eco.getBalance(player);
        if (t - amount < 0.0) {
            return false;
        }
        return eco.withdrawPlayer(player, amount).transactionSuccess();
    }
}
