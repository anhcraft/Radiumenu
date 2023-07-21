package dev.anhcraft.radiumenu.gui;

import dev.anhcraft.jvmkit.utils.MathUtil;
import dev.anhcraft.jvmkit.utils.ReflectionUtil;
import dev.anhcraft.radiumenu.Radiumenu;
import dev.anhcraft.radiumenu.utils.CommandUtils;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GUI {
    public String id;
    public Slot[] slots;
    private String title;
    private final Map<String, PluginCommand> commands;
    private boolean enabled;
    private final File file;
    private final FileConfiguration config;

    public GUI(int size) {
        slots = new Slot[size];
        title = "Untitled (#" + System.currentTimeMillis() + ")";
        id = UUID.randomUUID().toString();
        for (int i = 0; i < size; ++i) {
            setSlot(i, new Slot(Radiumenu.defaultGuiItem));
        }
        commands = new HashMap<>();
        enabled = false;
        file = new File(Radiumenu.getInstance().getGUIFolder(), id + ".yml");
        try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public GUI(File file) {
        this.file = file;
        config = YamlConfiguration.loadConfiguration(file);
        id = config.getString("id");
        title = config.getString("title");
        enabled = config.getBoolean("enable");
        Radiumenu.chat.sendConsole("&d[#" + id + "] Registering commands...");
        commands = new HashMap<>();
        for (String cmd : config.getStringList("commands")) {
            initCmd(cmd);
        }
        slots = new Slot[config.getInt("size")];
        ConfigurationSection cs = config.getConfigurationSection("slots");
        if (cs == null) {
            Radiumenu.chat.sendConsole("&f[#" + id + "] Loaded 0 slots!");
            slots = new Slot[1];
            slots[0] = new Slot(Radiumenu.defaultGuiItem);
            return;
        }
        List<String> v = new ArrayList<>(cs.getKeys(false));
        int i = 0;
        for (String slot : v) {
            if (Math.random() < 0.1) {
                Radiumenu.chat.sendConsole("&b[#" + this.id + "] Loading slots... " + MathUtil.formatRound(100d / v.size() * i) + "%");
            }
            Slot s = new Slot(cs.getItemStack(slot + ".item"));
            s.setMoney(cs.getDouble(slot + ".money"));
            if(!Radiumenu.INTEGRATIONS[0].isHooked() && s.getMoney() > 0) {
                Radiumenu.chat.sendConsole("&b[#" + this.id + "] - &eSlot at position " + (i + 1) + " may have issues because Vault support is unavailable.");
            }
            s.setPoints(cs.getInt(slot + ".points"));
            if(!Radiumenu.INTEGRATIONS[1].isHooked() && s.getPoints() > 0) {
                Radiumenu.chat.sendConsole("&b[#" + this.id + "] - &eSlot at position " + (i + 1) + " may have issues because PlayerPoints support is unavailable.");
            }
            s.setCloseGUI(cs.getBoolean(slot + ".close"));
            s.setRewardItem(cs.getBoolean(slot + ".rewarditem"));
            s.getCommands().addAll(cs.getStringList(slot + ".commands"));
            if (cs.isSet(slot + ".tradeitems")) {
                //noinspection unchecked
                s.getTradeItems().addAll((List<ItemStack>) cs.getList(slot + ".tradeitems"));
            }
            s.setLimitTransaction(cs.getInt(slot + ".limit_transaction", 0));
            if (cs.isSet(slot + ".transaction_counter")) {
                for (String id : cs.getConfigurationSection(slot + ".transaction_counter").getKeys(false)) {
                    s.getTransactionCounter().put(UUID.fromString(id), cs.getInt(id + ".transaction_counter." + id));
                }
            }
            s.setSkullOwner(cs.getString(slot + ".skullOwner"));
            s.setPermissionRequired(cs.getBoolean(slot + ".perm_required"));
            slots[i] = s;
            i++;
        }
        Radiumenu.chat.sendConsole("&a[#" + id + "] Loaded " + i + " slots!");
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSlot(int index, Slot slot) {
        slots[index] = slot;
    }

    public Slot getSlot(int index) {
        return slots[index];
    }

    public void save() throws IOException {
        config.set("id", id);
        config.set("title", title);
        config.set("commands", new ArrayList<>(commands.keySet()));
        config.set("size", slots.length);
        config.set("enable", enabled);
        config.set("slots", null);
        for (int i = 0; i < slots.length; ++i) {
            Slot s = slots[i];
            String ks = "slots." + i;
            config.set(ks + ".item", s.getRawItem());
            config.set(ks + ".money", s.getMoney());
            config.set(ks + ".points", s.getPoints());
            config.set(ks + ".close", s.isCloseGUI());
            config.set(ks + ".rewarditem", s.isRewardItem());
            config.set(ks + ".perm_required", s.isPermissionRequired());
            config.set(ks + ".commands", s.getCommands());
            config.set(ks + ".tradeitems", s.getTradeItems());
            config.set(ks + ".limit_transaction", s.getLimitTransaction());
            for (Map.Entry<UUID, Integer> k : s.getTransactionCounter().entrySet()) {
                config.set(ks + ".transaction_counter." + k.getKey().toString(), k.getValue());
            }
            config.set(ks + ".skullOwner", s.getSkullOwner());
        }
        config.save(file);
    }

    public Map<String, PluginCommand> getCommands() {
        return commands;
    }

    public int getSize() {
        return slots.length;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void initCmd(String cmd) {
        PluginCommand c = (PluginCommand) ReflectionUtil.invokeDeclaredConstructor(PluginCommand.class, new Class[]{String.class, Plugin.class}, new Object[]{cmd, Radiumenu.getInstance()});
        Objects.requireNonNull(c).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                new Handler(this).openGUI(p, 0);
            } else {
                Radiumenu.chat.sendCommandSender(Radiumenu.getInstance().getConfig().getString("messages.must_be_player"), sender);
            }
            return false;
        });
        CommandUtils.register(Radiumenu.getInstance(), c);
        commands.put(cmd, c);
    }

    public void destroyCmd(JavaPlugin plugin, String cmd) {
        CommandUtils.unregister(plugin, commands.get(cmd));
    }

    public void destroy() {
        for (String cmd : commands.keySet()) {
            destroyCmd(Radiumenu.getInstance(), cmd);
        }
        file.delete();
    }
}
