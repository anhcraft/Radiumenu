package dev.anhcraft.radiumenu.utils;

import dev.anhcraft.jvmkit.utils.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class CommandUtils {
    private static Class<?> craftServerClass;

    static {
        try {
            craftServerClass = Class.forName("org.bukkit.craftbukkit." + NMSVersion.getVersion().toString() + ".CraftServer");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void register(JavaPlugin plugin, PluginCommand command) {
        Object craftServer = craftServerClass.cast(Bukkit.getServer());
        SimpleCommandMap cmdMap = Objects.requireNonNull((SimpleCommandMap) ReflectionUtil.getDeclaredField(craftServerClass, craftServer,"commandMap"));
        cmdMap.register(plugin.getDescription().getName(), command);
    }

    /**
     * Code from HSCore
     * @author HSGamer
     * */
    public static void syncCommands() {
        Runnable syncCommandsRunnable;

        try {
            Class<?> craftServer = Bukkit.getServer().getClass();
            Method syncCommandsMethod = craftServer.getDeclaredMethod("syncCommands");
            syncCommandsMethod.setAccessible(true);

            syncCommandsRunnable = () -> {
                try {
                    syncCommandsMethod.invoke(Bukkit.getServer());
                } catch (ReflectiveOperationException e) {
                    Bukkit.getLogger().log(Level.WARNING, "Error when syncing commands", e);
                }
            };

        } catch (Exception e) {
            syncCommandsRunnable = () -> {};
        }

        syncCommandsRunnable.run();
    }

    @SuppressWarnings("unchecked")
    public static void unregister(JavaPlugin plugin, PluginCommand command) {
        Object craftServer = craftServerClass.cast(Bukkit.getServer());
        SimpleCommandMap cmdMap = Objects.requireNonNull((SimpleCommandMap) ReflectionUtil.getDeclaredField(craftServerClass, craftServer,"commandMap"));
        Map<String, Command> knownCommands = (Map<String, Command>) ReflectionUtil.getDeclaredField(SimpleCommandMap.class, cmdMap, "knownCommands");
        Objects.requireNonNull(knownCommands).remove((plugin.getName()+":"+command.getName()).toLowerCase());
        if(!command.getLabel().contains(":")) knownCommands.remove(command.getLabel());
        List<String> activeAliases = command.getAliases();
        for (String alias : activeAliases) knownCommands.remove(alias);
        List<String> registeredAliases = Objects.requireNonNull((List<String>) ReflectionUtil.getDeclaredField(Command.class, command, "aliases"));
        for (String alias : registeredAliases) knownCommands.remove((plugin.getName()+":"+alias).toLowerCase());
        command.unregister(cmdMap);
    }
}
