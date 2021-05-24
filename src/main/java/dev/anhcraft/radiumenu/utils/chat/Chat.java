package dev.anhcraft.radiumenu.utils.chat;

import java.util.List;

import dev.anhcraft.radiumenu.utils.NMSVersion;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Chat {
    private final BaseComponent prefix;
    private final String plainPrefix;

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String[] color(String[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = ChatColor.translateAlternateColorCodes('&', array[i]);
        }
        return array;
    }

    public static List<String> color(List<String> list) {
        for (int i = 0; i < list.size(); ++i) {
            list.set(i, ChatColor.translateAlternateColorCodes('&', list.get(i)));
        }
        return list;
    }

    public Chat(String prefix) {
        this.prefix = this.build(prefix);
        this.plainPrefix = Chat.color(prefix);
    }

    public Chat(BaseComponent prefix) {
        this.prefix = prefix;
        this.plainPrefix = prefix.toLegacyText();
    }

    public void sendConsole(String message) {
        if (NMSVersion.is1_12Above()) {
            Bukkit.getConsoleSender().spigot().sendMessage(prefix, build(message));
        } else {
            Bukkit.getConsoleSender().sendMessage(plainPrefix + Chat.color(message));
        }
    }

    public void sendConsole(BaseComponent message) {
        if (NMSVersion.is1_12Above()) {
            Bukkit.getConsoleSender().spigot().sendMessage(prefix, message);
        } else {
            Bukkit.getConsoleSender().sendMessage(plainPrefix + message.toLegacyText());
        }
    }

    public void sendConsoleNoPrefix(String message) {
        if (NMSVersion.is1_12Above()) {
            Bukkit.getConsoleSender().spigot().sendMessage(build(message));
        } else {
            Bukkit.getConsoleSender().sendMessage(Chat.color(message));
        }
    }

    public void sendConsoleNoPrefix(BaseComponent message) {
        if (NMSVersion.is1_12Above()) {
            Bukkit.getConsoleSender().spigot().sendMessage(message);
        } else {
            Bukkit.getConsoleSender().sendMessage(message.toLegacyText());
        }
    }

    public void sendCommandSender(String message, CommandSender sender) {
        if (NMSVersion.is1_12Above()) {
            sender.spigot().sendMessage(prefix, build(message));
        } else {
            sender.sendMessage(plainPrefix + Chat.color(message));
        }
    }

    public void sendCommandSender(BaseComponent message, CommandSender sender) {
        if (NMSVersion.is1_12Above()) {
            sender.spigot().sendMessage(prefix, message);
        } else {
            sender.sendMessage(plainPrefix + message.toLegacyText());
        }
    }

    public void sendCommandSenderNoPrefix(String message, CommandSender sender) {
        if (NMSVersion.is1_12Above()) {
            sender.spigot().sendMessage(build(message));
        } else {
            sender.sendMessage(Chat.color(message));
        }
    }

    public void sendCommandSenderNoPrefix(BaseComponent message, CommandSender sender) {
        if (NMSVersion.is1_12Above()) {
            sender.spigot().sendMessage(message);
        } else {
            sender.sendMessage(message.toLegacyText());
        }
    }

    public void sendPlayer(String message, Player player) {
        player.spigot().sendMessage(prefix, build(message));
    }

    public void sendPlayer(BaseComponent message, Player player) {
        player.spigot().sendMessage(prefix, message);
    }

    public void sendPlayerNoPrefix(String message, Player player) {
        player.spigot().sendMessage(build(message));
    }

    public void sendPlayerNoPrefix(BaseComponent message, Player player) {
        player.spigot().sendMessage(message);
    }

    public void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPlayer(message, player);
        }
    }

    public void broadcast(BaseComponent message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPlayer(message, player);
        }
    }

    public void broadcastNoPrefix(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPlayerNoPrefix(message, player);
        }
    }

    public void broadcastNoPrefix(BaseComponent message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPlayerNoPrefix(message, player);
        }
    }

    public void broadcast(String message, World world) {
        for (Player player : world.getPlayers()) {
            sendPlayer(message, player);
        }
    }

    public void broadcast(BaseComponent message, World world) {
        for (Player player : world.getPlayers()) {
            sendPlayer(message, player);
        }
    }

    public void broadcastNoPrefix(String message, World world) {
        for (Player player : world.getPlayers()) {
            sendPlayerNoPrefix(message, player);
        }
    }

    public void broadcastNoPrefix(BaseComponent message, World world) {
        for (Player player : world.getPlayers()) {
            sendPlayerNoPrefix(message, player);
        }
    }

    private TextComponent build(String text) {
        return new TextComponent(TextComponent.fromLegacyText(Chat.color(text)));
    }
}

