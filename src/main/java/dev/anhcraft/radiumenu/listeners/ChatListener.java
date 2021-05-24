package dev.anhcraft.radiumenu.listeners;

import dev.anhcraft.radiumenu.Radiumenu;
import dev.anhcraft.radiumenu.utils.chat.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatListener implements Listener {
    private static final Map<UUID, Consumer<String>> CHAT_QUEUE = new HashMap<>();

    public static void prompt(Player player, Consumer<String> callback) {
        player.closeInventory();
        CHAT_QUEUE.put(player.getUniqueId(), callback);
    }

    public static void prompt(Player player, String msg, Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage(Chat.color(msg));
        CHAT_QUEUE.put(player.getUniqueId(), callback);
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent event) {
        Consumer<String> callback = CHAT_QUEUE.remove(event.getPlayer().getUniqueId());
        if(callback != null){
            event.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.accept(event.getMessage());
                }
            }.runTask(Radiumenu.getInstance());
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        CHAT_QUEUE.remove(event.getPlayer().getUniqueId());
    }
}
