package dev.anhcraft.radiumenu.integrations;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class PlayerPointIntegration extends Integration {
    private PlayerPointsAPI api;

    @Override
    public boolean init(JavaPlugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
            return false;
        }
        api = PlayerPoints.getPlugin(PlayerPoints.class).getAPI();
        return api != null;
    }

    public int enough(UUID uuid, int amount) {
        int t = api.look(uuid);
        return t - amount >= 0 ? 0 : amount - t;
    }

    public boolean withdraw(UUID uuid, int amount) {
        int t = api.look(uuid);
        if (t - amount < 0) {
            return false;
        }
        return api.take(uuid, amount);
    }
}
