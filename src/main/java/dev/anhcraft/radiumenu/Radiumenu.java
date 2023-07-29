package dev.anhcraft.radiumenu;

import dev.anhcraft.jvmkit.utils.IOUtil;
import dev.anhcraft.jvmkit.utils.ReflectionUtil;
import dev.anhcraft.radiumenu.gui.GUI;
import dev.anhcraft.radiumenu.gui.Handler;
import dev.anhcraft.radiumenu.gui.pagination.PaginationStream;
import dev.anhcraft.radiumenu.integrations.Integration;
import dev.anhcraft.radiumenu.integrations.PlaceholderIntegration;
import dev.anhcraft.radiumenu.integrations.PlayerPointIntegration;
import dev.anhcraft.radiumenu.integrations.VaultIntegration;
import dev.anhcraft.radiumenu.listeners.ChatListener;
import dev.anhcraft.radiumenu.listeners.InventoryListener;
import dev.anhcraft.radiumenu.utils.*;
import dev.anhcraft.radiumenu.utils.chat.Chat;
import dev.anhcraft.radiumenu.utils.inv.InventoryEditor;
import dev.anhcraft.radiumenu.utils.item.ItemEditor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public final class Radiumenu extends JavaPlugin {
    private static Radiumenu instance;
    public static ItemStack backgroundItem;
    public static ItemStack defaultGuiItem;
    public static Chat chat;
    public static final Map<String, GUI> GUI = new HashMap<>();
    public static final Integration[] INTEGRATIONS = new Integration[]{
            new VaultIntegration(),
            new PlayerPointIntegration(),
            new PlaceholderIntegration()
    };
    public YamlConfiguration localeConfig;

    public File getGUIFolder() {
        return new File(getDataFolder(), "data");
    }

    public static Radiumenu getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        backgroundItem = new ItemEditor("&a", XMaterial.GRAY_STAINED_GLASS_PANE, 1).getItem();
        defaultGuiItem = new ItemEditor("&a", XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE, 1).getItem();

        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        PluginCommand c = (PluginCommand) ReflectionUtil.invokeDeclaredConstructor(PluginCommand.class, new Class[]{String.class, Plugin.class}, new Object[]{"rdm", this});
        Objects.requireNonNull(c).setExecutor((sender, command, label, args) -> {
            if (args.length == 0) {
                if (sender.hasPermission("rdm.admin")) {
                    if (sender instanceof Player) {
                        Player p = (Player)sender;
                        openAdminMenu(p, 0);
                    } else {
                        chat.sendCommandSender(localeConfig.getString("messages.must_be_player"), sender);
                    }
                } else {
                    chat.sendCommandSender(localeConfig.getString("messages.no_perm"), sender);
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("rdm.admin")) {
                    init();
                    chat.sendCommandSender(localeConfig.getString("messages.reloaded"), sender);
                } else {
                    chat.sendCommandSender(localeConfig.getString("messages.no_perm"), sender);
                }
            }
            return false;
        });
        CommandUtils.register(this, c);

        new BukkitRunnable() {
            @Override
            public void run() {
                init();
            }
        }.runTaskLater(this, 40);

        new BukkitRunnable(){
            public void run() {
                try {
                    for (GUI g : GUI.values()) {
                        g.save();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(this, 0L, 20L * getConfig().getLong("save_interval"));
    }

    private void init() {
        //noinspection ResultOfMethodCallIgnored
        getGUIFolder().mkdirs();
        reloadConfig();

        try {
            localeConfig = YamlConfiguration.loadConfiguration(new StringReader(new String(IOUtil.readResource(getClass(), "/locale/" + getConfig().getString("locale", "vi") + ".yml"), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        chat = new Chat(getConfig().getString("prefix"));

        if (getConfig().getBoolean("integrations.vault") && INTEGRATIONS[0].init(this)) {
            chat.sendConsole("&aHooked to &fVault!");
            INTEGRATIONS[0].setHooked(true);
        }
        if (getConfig().getBoolean("integrations.playerpoints") && INTEGRATIONS[1].init(this)) {
            chat.sendConsole("&aHooked to &fPlayerPoints!");
            INTEGRATIONS[1].setHooked(true);
        }
        if (getConfig().getBoolean("integrations.placeholderapi") && INTEGRATIONS[2].init(this)) {
            chat.sendConsole("&aHooked to &fPlaceholderAPI!");
            INTEGRATIONS[2].setHooked(true);
        }

        GUI.clear();
        try {
            for (File f : Objects.requireNonNull(getGUIFolder().listFiles())) {
                getLogger().info("Loading file " + f.getName() + "...");
                GUI g = new GUI(f);
                GUI.put(g.getId(), g);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        CommandUtils.syncCommands();
    }

    @Override
    public void onDisable() {
        try {
            for (GUI g : GUI.values()) {
                g.save();

                for (Iterator<String> it = g.getCommands().keySet().iterator(); it.hasNext(); ) {
                    g.destroyCmd(this, it.next());

                    it.remove();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openAdminMenu(Player player, int index) {
        InventoryEditor inv = new InventoryEditor(localeConfig.getString("gui.admin_menu.title"), 54);
        inv.fill(backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});

        new PaginationStream<>(new ArrayList<>(GUI.values()), index, 45).forEach((i, j_, g) -> {
            inv.set(i, new ItemEditor((g.isEnabled() ? localeConfig.getString("gui.admin_menu.enabled") : localeConfig.getString("gui.admin_menu.disabled")) + "&r " + g.getTitle(), Material.ENDER_CHEST, 1)
                    .setLore(
                            localeConfig.getStringList("gui.admin_menu.gui_info")
                                    .stream()
                                    .map(s -> s.replace("{id}", g.getId())
                                            .replace("{size}", Integer.toString(g.getSize()))
                                            .replace("{commands}", String.join(", ", g.getCommands().keySet()))
                                    ).collect(Collectors.toList())
                    ).getItem(), (player13, inventory, inventoryAction, i1, itemStack, clickType) -> {
                if (clickType.isLeftClick()) {
                    if (clickType.isShiftClick()) {
                        GUI cg = new GUI(g.getSize());
                        cg.setTitle(cg.getTitle() + " (Clone #" + System.currentTimeMillis() + ")");
                        for (int j = 0; j < cg.getSize(); j++) {
                            cg.setSlot(j, g.getSlot(j).duplicate());
                        }
                        GUI.put(cg.getId(), cg);
                        openAdminMenu(player13, 0);
                    } else {
                        new Handler(g).editor(player13, 0);
                    }
                } else if (clickType.isRightClick()) {
                    if (clickType.isShiftClick()) {
                        openDestroyMenu(player13, g);
                    } else {
                        g.setEnabled(!g.isEnabled());
                        openAdminMenu(player13, 0);
                    }
                }
            });
        });

        inv.set(46, new ItemEditor(localeConfig.getString("gui.pagination.prev"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> openAdminMenu(player1, index - 1));
        inv.set(49, new ItemEditor(localeConfig.getString("gui.pagination.next"), Material.ARROW, 1).getItem(), (player12, inventory, inventoryAction, i1, itemStack, clickType) -> openAdminMenu(player12, index + 1));
        inv.set(52, new ItemEditor(localeConfig.getString("gui.admin_menu.new_gui"), Material.FEATHER, 1).setLore(localeConfig.getStringList("gui.admin_menu.new_gui_info")).getItem(), (player14, inventory, inventoryAction, i12, itemStack, clickType) -> {
            player14.closeInventory();
            int size = 54;
            if (clickType.isShiftClick() && clickType.isLeftClick()) {
                size = 45;
            } else if (clickType.isShiftClick() && clickType.isRightClick()) {
                size = 108;
            } else if (clickType.isRightClick()) {
                size = 27;
            }
            GUI g = new GUI(size);
            GUI.put(g.getId(), g);
            openAdminMenu(player14, index);
        });
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 2.0f, 1.0f);
    }

    private void openDestroyMenu(Player p, GUI g) {
        InventoryEditor s = new InventoryEditor(localeConfig.getString("gui.destroy_menu.title"), 9);
        s.fill(backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
        s.set(1, new ItemEditor(localeConfig.getString("gui.destroy_menu.accept"), XMaterial.EMERALD, 1).getItem(), (player, inventory, inventoryAction, i, itemStack, clickType) -> {
            g.destroy();
            GUI.remove(g.getId());
            openAdminMenu(player, 0);
        });
        s.set(4, new ItemEditor(localeConfig.getString("gui.destroy_menu.info"), Material.BOOK, 1).getItem(), (player, inventory, inventoryAction, i, itemStack, clickType) -> {});
        s.set(7, new ItemEditor(localeConfig.getString("gui.destroy_menu.deny"), XMaterial.BARRIER, 1).getItem(), (player, inventory, inventoryAction, i, itemStack, clickType) -> openAdminMenu(player, 0));
        s.open(p);
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 4.0f, 1.0f);
    }
}
