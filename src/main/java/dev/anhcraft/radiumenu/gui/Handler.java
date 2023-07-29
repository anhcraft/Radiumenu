package dev.anhcraft.radiumenu.gui;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.anhcraft.jvmkit.lang.enumeration.RegEx;
import dev.anhcraft.jvmkit.utils.ArrayUtil;
import dev.anhcraft.jvmkit.utils.MathUtil;
import dev.anhcraft.radiumenu.Radiumenu;
import dev.anhcraft.radiumenu.gui.pagination.PaginationStream;
import dev.anhcraft.radiumenu.integrations.Integration;
import dev.anhcraft.radiumenu.integrations.PlayerPointIntegration;
import dev.anhcraft.radiumenu.integrations.VaultIntegration;
import dev.anhcraft.radiumenu.listeners.ChatListener;
import dev.anhcraft.radiumenu.utils.*;
import dev.anhcraft.radiumenu.utils.chat.ComponentBuilder;
import dev.anhcraft.radiumenu.utils.inv.InventoryEditor;
import dev.anhcraft.radiumenu.utils.item.ItemEditor;
import dev.anhcraft.radiumenu.utils.item.ItemUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Handler {
    private final ConfigurationSection mainConfig;
    private final ConfigurationSection localeConfig;
    private final GUI GUI;
    
    public Handler(GUI GUI) {
        this.GUI = GUI;
        this.mainConfig = Radiumenu.getInstance().getConfig();
        this.localeConfig = Radiumenu.getInstance().localeConfig;
    }
    
    public void openGUI(Player player, int index) {
        if (player.hasPermission("rdm.open.*") || player.hasPermission("rdm.open." + GUI.id)) {
            if (GUI.isEnabled()) {
                forceOpenGUI(player, index);
            } else {
                Radiumenu.chat.sendPlayer(localeConfig.getString("messages.inaccessible_gui"), player);
            }
        } else {
            Radiumenu.chat.sendPlayer(localeConfig.getString("messages.no_perm"), player);
        }
    }
    
    public void forceOpenGUI(Player player, int index) {
        boolean multiPage = GUI.slots.length > 54;
        InventoryEditor inv = new InventoryEditor(GUI.getTitle(), Math.min(GUI.slots.length, 54));
        int size = inv.getInventory().getSize();
        int offset = size * index;
        if (offset < 0 || offset >= GUI.slots.length) {
            return;
        }
        int ctnSize = multiPage ? size - 9 : size;
        for (int i = 0; i < ctnSize; i++) {
            int pos = offset + i;
            if (GUI.getSize() <= pos) break;
            Slot s = GUI.slots[pos];
            ItemStack itemStack = s.getItem(player).clone();
            if (itemStack.getItemMeta() instanceof SkullMeta && !s.getSkullOwner().isEmpty()) {
                SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
                meta.setOwner(s.getSkullOwner().replace("%s%", player.getName()));
                itemStack.setItemMeta(meta);
            }
            ItemEditor item = new ItemEditor(itemStack);
            if (s.getMoney() > 0) {
                item.addLore(localeConfig.getString("gui.money_cost").replace("{value}", Double.toString(s.getMoney())));
            }
            if (s.getPoints() > 0) {
                item.addLore(localeConfig.getString("gui.point_cost").replace("{value}", Integer.toString(s.getPoints())));
            }
            if (s.getTradeItems().size() > 0) {
                item.addLore(localeConfig.getString("gui.tradingitem").replace("{value}", Integer.toString(s.getTradeItems().size())));
                item.addLore(localeConfig.getString("gui.tradingitem_use"));
            }
            if (s.getLimitTransaction() > 0) {
                item.addLore(localeConfig.getString("gui.limit_transaction").replace("{value}", Integer.toString(s.getLimitTransaction())));
            }
            inv.set(i, item.getItem(), (p, inv_, invAction, slot, item_, clickType) -> {
                if(s.isPermissionRequired() && !player.hasPermission("rdm.slot." + GUI.id + "." + pos)) {
                    p.closeInventory();
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.no_perm"), p);
                    return;
                }
                if (s.getTradeItems().size() > 0 && clickType.isRightClick()) {
                    viewTradeItems(p, pos, 0);
                    return;
                }
                if (s.getLimitTransaction() > 0 && s.getTransactionCounter().getOrDefault(player.getUniqueId(), 0) >= s.getLimitTransaction()) {
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.reached_limit").replace("{limit}", Integer.toString(s.getLimitTransaction())), p);
                    return;
                }
                if (s.isRewardItem() && p.getInventory().firstEmpty() == -1) {
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.inv_full"), p);
                    return;
                }
                if (s.getMoney() > 0) {
                    Integration in = Radiumenu.INTEGRATIONS[0];
                    if (!in.isHooked()) {
                        Radiumenu.chat.sendPlayer(localeConfig.getString("messages.economic_not_init"), p);
                        return;
                    }
                    double nm = ((VaultIntegration) in).enough(p, s.getMoney());
                    if (nm > 0) {
                        Radiumenu.chat.sendPlayer(localeConfig.getString("messages.not_enough_money").replace("{need_more}", MathUtil.formatRound(nm)), p);
                        return;
                    }
                }
                if (s.getPoints() > 0) {
                    Integration in = Radiumenu.INTEGRATIONS[1];
                    if (!in.isHooked()) {
                        Radiumenu.chat.sendPlayer(localeConfig.getString("messages.economic_not_init"), p);
                        return;
                    }
                    int nm = ((PlayerPointIntegration) in).enough(p.getUniqueId(), s.getPoints());
                    if (nm > 0) {
                        Radiumenu.chat.sendPlayer(localeConfig.getString("messages.not_enough_points").replace("{need_more}", String.valueOf(nm)), p);
                        return;
                    }
                }
                if (s.getTradeItems().size() > 0 && !ArrayUtil.toList(p.getInventory().getContents()).containsAll(s.getTradeItems())) {
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.not_enough_items"), p);
                    return;
                }
                if (s.getMoney() > 0 && !((VaultIntegration) Radiumenu.INTEGRATIONS[0]).withdraw(p, s.getMoney())) {
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.transaction_error"), p);
                    return;
                }
                if (s.getPoints() > 0 && !((PlayerPointIntegration) Radiumenu.INTEGRATIONS[1]).withdraw(p.getUniqueId(), s.getPoints())) {
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.transaction_error"), p);
                    return;
                }
                if (s.getTradeItems().size() > 0) {
                    ItemStack[] items = player.getInventory().getContents();
                    for (ItemStack tradeItem : s.getTradeItems()) {
                        int remaining = tradeItem.getAmount();
                        for (ItemStack is : items) {
                            if(!ItemUtils.isNull(is) && is.isSimilar(tradeItem)){
                                int a = is.getAmount();
                                if(remaining < a) {
                                    is.setAmount(a - remaining);
                                } else {
                                    is.setAmount(0);
                                    remaining -= a;
                                }
                            }
                        }
                        if(remaining > 0) {
                            Radiumenu.chat.sendPlayer(localeConfig.getString("messages.transaction_error"), p);
                            return;
                        }
                    }
                    player.getInventory().setContents(items);
                }
                if (s.isCloseGUI()) {
                    p.closeInventory();
                }
                if (s.isRewardItem()) {
                    p.getInventory().addItem(s.getItem(p));
                }
                if (s.getPoints() > 0 || s.getMoney() > 0 || s.getTradeItems().size() > 0) {
                    s.getTransactionCounter().put(p.getUniqueId(), s.getTransactionCounter().getOrDefault(p.getUniqueId(), 0) + 1);
                }
                for (String cmd : s.getCommands()) {
                    if ((cmd = cmd.trim()).startsWith("console:")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring("console:".length()).trim().replace("%s%", p.getName()));
                        continue;
                    }
                    if (cmd.startsWith("op:")) {
                        try {
                            executeAsOp(p, cmd.substring("op:".length()).trim().replace("%s%", p.getName()));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    if (cmd.startsWith("gui:")) {
                        String id = cmd.substring("gui:".length()).trim().replace("%s%", p.getName());
                        GUI g = Radiumenu.GUI.get(id);
                        if (g != null) {
                            new Handler(g).openGUI(p, 0);
                        }
                        continue;
                    }
                    if (cmd.startsWith("player_")) {
                        String[] x = cmd.substring("player_".length()).split(":");
                        Player target = Bukkit.getPlayer(x[0]);
                        if (target == null) continue;
                        Bukkit.dispatchCommand(target, x[1].trim().replace("%s%", p.getName()));
                        continue;
                    }
                    if (cmd.startsWith("connect:")) {
                        //noinspection UnstableApiUsage
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("Connect");
                        out.writeUTF(cmd.substring("connect:".length()));
                        p.sendPluginMessage(Radiumenu.getInstance(), "BungeeCord", out.toByteArray());
                        continue;
                    }
                    Bukkit.dispatchCommand(p, cmd.replace("%s%", p.getName()));
                }
            });
        }
        if (multiPage) {
            inv.set(ctnSize, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
            inv.set(ctnSize + 1, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
            if(index > 0) {
                inv.set(ctnSize + 2, new ItemEditor(localeConfig.getString("gui.pagination.prev"), Material.ARROW, 1).getItem(), (p, inventory, inventoryAction, i1, itemStack, clickType) -> forceOpenGUI(p, index - 1));
            } else {
                inv.set(ctnSize + 2, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
            }
            inv.set(ctnSize + 3, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
            inv.set(ctnSize + 4, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
            inv.set(ctnSize + 5, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
            if(offset + size < GUI.getSize()) {
                inv.set(ctnSize + 6, new ItemEditor(localeConfig.getString("gui.pagination.next"), Material.ARROW, 1).getItem(), (p, inventory, inventoryAction, i1, itemStack, clickType) -> forceOpenGUI(p, index + 1));
            } else {
                inv.set(ctnSize + 6, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
            }
            inv.set(ctnSize + 7, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
            inv.set(ctnSize + 8, Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
        }
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 2.0f, 3.0f);
    }

    private void executeAsOp(Player player, String cmd) throws Exception {
        try {
            boolean current = player.isOp();
            if (current) {
                player.performCommand(cmd);
            } else {
                player.setOp(true);
                player.performCommand(cmd);
                player.setOp(false);
            }
        }
        catch (Exception ex) {
            Bukkit.getServer().shutdown();
            throw new Exception("Having errors while trying to execute an OP command by " + player.getName());
        }
    }

    private void viewTradeItems(Player player, int slot, int index) {
        InventoryEditor inv = new InventoryEditor(localeConfig.getString("gui.view_tradeitems.title").replace("{index}", Integer.toString(slot)), 54);
        inv.fill(Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
        Slot s = GUI.getSlot(slot);
        new PaginationStream<>(s.getTradeItems(), index, 45).forEach((i, j, item) -> {
            inv.set(i, item.clone(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> { });
        });
        inv.set(46, new ItemEditor(localeConfig.getString("gui.pagination.prev"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i13, itemStack, clickType) -> viewTradeItems(player1, slot, index - 1));
        inv.set(49, new ItemEditor(localeConfig.getString("gui.pagination.next"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i12, itemStack, clickType) -> viewTradeItems(player1, slot, index + 1));
        inv.set(52, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player12, inventory, inventoryAction, i14, itemStack, clickType) -> forceOpenGUI(player12, 0));
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 4.0f, 3.0f);
    }

    public void editor(Player player, final int index) {
        InventoryEditor inv = new InventoryEditor(localeConfig.getString("gui.gui_editor.title").replace("{title}", GUI.getTitle()), 54);
        inv.fill(Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});

        int maxSlot = new PaginationStream<>(GUI.slots, index, 45).forEach((i, j, s) -> {
            inv.set(i, new ItemEditor(s.getItem(player).clone()).setLore(localeConfig.getStringList("gui.gui_editor.info").stream().map(s5 -> s5.replace("{current}", Integer.toString(j + 1)).replace("{max}", Integer.toString(GUI.getSize()))).collect(Collectors.toList())).getItem(), (player12, inventory, inventoryAction, i1, itemStack, clickType) -> {
                if (clickType.isLeftClick()) {
                    slotEditor(player12, j);
                } else if (clickType.isRightClick()) {
                    if (clickType.isShiftClick()) {
                        GUI.slots[j] = new Slot(Radiumenu.defaultGuiItem);
                        editor(player, index);
                    } else {
                        slotCopier(player12, 0, j);
                    }
                }
            });
        }) + index * 45;
        
        inv.set(45, new ItemEditor(localeConfig.getString("gui.pagination.prev"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> editor(player1, index - 1));
        inv.set(46, new ItemEditor(localeConfig.getString("gui.pagination.next"), Material.ARROW, 1).getItem(), (player12, inventory, inventoryAction, i1, itemStack, clickType) -> editor(player12, index + 1));
        inv.set(47, new ItemEditor(localeConfig.getString("gui.gui_editor.edit_id"), Material.ENCHANTED_BOOK, 1).getItem(), (player13, inventory, inventoryAction, i12, itemStack, clickType) -> {
            ChatListener.prompt(player13, localeConfig.getString("gui.gui_editor.edit_id_msg"), s -> {
                if (!Radiumenu.GUI.containsKey(s)) {
                    Radiumenu.GUI.put(s, Radiumenu.GUI.remove(GUI.id));
                    GUI.id = s;
                    editor(player13, 0);
                } else {
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.id_already_existed"), player13);
                }
            });
        });
        inv.set(48, new ItemEditor(localeConfig.getString("gui.gui_editor.get_opening_perm"), Material.PAPER, 1).getItem(), (player12, inventory, inventoryAction, i1, itemStack, clickType) -> {
            player12.closeInventory();
            player12.spigot().sendMessage(new ComponentBuilder(TextComponent.class).text("rdm.open." + GUI.id, new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "rdm.open." + GUI.id)).build());
        });
        inv.set(49, new ItemEditor(localeConfig.getString("gui.gui_editor.rename"), XMaterial.OAK_SIGN, 1).getItem(), (player14, inventory, inventoryAction, i13, itemStack, clickType) -> {
            ChatListener.prompt(player14, localeConfig.getString("gui.gui_editor.rename_msg"), s -> {
                GUI.setTitle(s);
                editor(player14, index);
            });
        });
        inv.set(50, new ItemEditor(localeConfig.getString("gui.gui_editor.cmd"), XMaterial.COMMAND_BLOCK, 1).getItem(), (player13, inventory, inventoryAction, i12, itemStack, clickType) -> guiCommandEditor(player13, 0));
        if (GUI.getSize() == 1) {
            inv.set(51, new ItemEditor(localeConfig.getString("gui.gui_editor.modify_size"), Material.EMERALD, 1)
                    .setLore(localeConfig.getStringList("gui.gui_editor.modify_size_info_min")
                            .stream()
                            .map(s5 -> s5.replace("{current}", Integer.toString(GUI.getSize())))
                            .collect(Collectors.toList())
                    ).getItem(), (player13, inventory, inventoryAction, i12, itemStack, clickType) -> {
                if (clickType.isRightClick()) {
                    return;
                }
                if (clickType.isShiftClick()) {
                    Slot[] nSlots = new Slot[GUI.getSize() + 9];
                    System.arraycopy(GUI.slots, 0, nSlots, 0, GUI.slots.length);
                    for (int j = 0; j < 9; ++j) {
                        nSlots[j + GUI.getSize()] = new Slot(Radiumenu.defaultGuiItem);
                    }
                    GUI.slots = nSlots;
                } else {
                    Slot[] nSlots = new Slot[GUI.getSize() + 1];
                    System.arraycopy(GUI.slots, 0, nSlots, 0, GUI.slots.length);
                    nSlots[GUI.getSize()] = new Slot(Radiumenu.defaultGuiItem);
                    GUI.slots = nSlots;
                }
                editor(player, index);
            });
        } else if (GUI.getSize() >= mainConfig.getInt("limit_slot")) {
            inv.set(51, new ItemEditor(localeConfig.getString("gui.gui_editor.modify_size"), Material.EMERALD, 1).setLore(localeConfig.getStringList("gui.gui_editor.modify_size_info_max").stream().map(s5 -> s5.replace("{current}", Integer.toString(GUI.getSize()))).collect(Collectors.toList())).getItem(), (player13, inventory, inventoryAction, i12, itemStack, clickType) -> {
                if (clickType.isLeftClick()) {
                    return;
                }
                if (clickType.isShiftClick()) {
                    Slot[] nSlots = new Slot[GUI.getSize() - 9];
                    System.arraycopy(GUI.slots, 0, nSlots, 0, GUI.slots.length - 9);
                    GUI.slots = nSlots;
                } else {
                    Slot[] nSlots = new Slot[GUI.getSize() - 1];
                    System.arraycopy(GUI.slots, 0, nSlots, 0, GUI.slots.length - 1);
                    GUI.slots = nSlots;
                }
                editor(player, maxSlot - 9 <= 0 ? index - 1 : index);
            });
        } else {
            inv.set(51, new ItemEditor(localeConfig.getString("gui.gui_editor.modify_size"), Material.EMERALD, 1).setLore(localeConfig.getStringList("gui.gui_editor.modify_size_info").stream().map(s5 -> s5.replace("{current}", Integer.toString(GUI.getSize()))).collect(Collectors.toList())).getItem(), (player13, inventory, inventoryAction, i12, itemStack, clickType) -> {
                if (clickType.isLeftClick()) {
                    if (clickType.isShiftClick()) {
                        Slot[] nSlots = new Slot[GUI.getSize() + 9];
                        int sz = 9;
                        if (mainConfig.getInt("limit_slot") < nSlots.length) {
                            nSlots = new Slot[mainConfig.getInt("limit_slot")];
                            sz = mainConfig.getInt("limit_slot") - GUI.getSize();
                        }
                        System.arraycopy(GUI.slots, 0, nSlots, 0, GUI.slots.length);
                        for (int j = 0; j < sz; ++j) {
                            nSlots[j + GUI.getSize()] = new Slot(Radiumenu.defaultGuiItem);
                        }
                        GUI.slots = nSlots;
                        editor(player, index);
                    } else {
                        Slot[] nSlots = new Slot[GUI.getSize() + 1];
                        System.arraycopy(GUI.slots, 0, nSlots, 0, GUI.slots.length);
                        nSlots[GUI.getSize()] = new Slot(Radiumenu.defaultGuiItem);
                        GUI.slots = nSlots;
                        editor(player, index);
                    }
                } else if (clickType.isRightClick()) {
                    if (clickType.isShiftClick()) {
                        int sz = Math.max(GUI.getSize() - 9, 1);
                        Slot[] nSlots = new Slot[sz];
                        System.arraycopy(GUI.slots, 0, nSlots, 0, sz);
                        GUI.slots = nSlots;
                        editor(player, maxSlot - 9 <= 0 ? index - 1 : index);
                    } else {
                        int sz = Math.max(GUI.getSize() - 1, 1);
                        Slot[] nSlots = new Slot[sz];
                        System.arraycopy(GUI.slots, 0, nSlots, 0, sz);
                        GUI.slots = nSlots;
                        editor(player, maxSlot - 1 <= 0 ? index - 1 : index);
                    }
                }
            });
        }
        inv.set(53, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player13, inventory, inventoryAction, i12, itemStack, clickType) -> Radiumenu.getInstance().openAdminMenu(player13, 0));
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 2.0f, 3.0f);
    }

    private void slotCopier(Player player, int index, int slot) {
        InventoryEditor inv = new InventoryEditor(localeConfig.getString("gui.slot_copy_helper.title").replace("{index}", Integer.toString(slot)), 54);
        inv.fill(Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});

         new PaginationStream<>(GUI.slots, index, 45).forEach((i, j, s) -> {
             if (j == slot) {
                 inv.set(i, new ItemEditor("&a", Material.BARRIER, 1).setLore(localeConfig.getStringList("gui.slot_copy_helper.self").stream().map(s5 -> s5.replace("{current}", Integer.toString(j + 1)).replace("{max}", Integer.toString(GUI.getSize()))).collect(Collectors.toList())).getItem(), (player12, inventory, inventoryAction, i1, itemStack, clickType) -> {});
             } else {
                 inv.set(i, new ItemEditor(s.getItem(player).clone()).setLore(localeConfig.getStringList("gui.slot_copy_helper.other").stream().map(s5 -> s5.replace("{current}", Integer.toString(j + 1)).replace("{max}", Integer.toString(GUI.getSize())).replace("{from}", Integer.toString(slot))).collect(Collectors.toList())).getItem(), (player12, inventory, inventoryAction, i1, itemStack, clickType) -> {
                     GUI.setSlot(j, GUI.getSlot(slot).duplicate());
                     slotCopier(player, index, slot);
                 });
             }
        });

        inv.set(46, new ItemEditor(localeConfig.getString("gui.pagination.prev"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> slotCopier(player1, index - 1, slot));
        inv.set(49, new ItemEditor(localeConfig.getString("gui.pagination.next"), Material.ARROW, 1).getItem(), (player12, inventory, inventoryAction, i1, itemStack, clickType) -> slotCopier(player12, index + 1, slot));
        inv.set(52, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player12, inventory, inventoryAction, i1, itemStack, clickType) -> editor(player12, 0));
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 2.0f, 3.0f);
    }

    private void slotEditor(Player player, final int index) {
        InventoryEditor inv = new InventoryEditor(localeConfig.getString("gui.slot_editor.title").replace("{index}", Integer.toString(index)), 45);
        inv.fill(Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
        final Slot s = GUI.getSlot(index);
        inv.set(10, new ItemEditor(localeConfig.getString("gui.slot_editor.edit_item"), Material.CAKE, 1).getItem(), (player12, inventory, inventoryAction, i, itemStack, clickType) -> editItemSlot(player12, index));
        inv.set(11, new ItemEditor(localeConfig.getString("gui.slot_editor.set_money"), Material.DIAMOND, 1).setLore(localeConfig.getStringList("gui.slot_editor.set_money_info").stream().map(s5 -> s5.replace("{current}", Double.toString(s.getMoney()))).collect(Collectors.toList())).getItem(), (player13, inventory, inventoryAction, i, itemStack, clickType) -> {
            ChatListener.prompt(player13, localeConfig.getString("gui.slot_editor.set_money_msg"), v -> {
                if (RegEx.POSITIVE_DECIMAL.valid(v)) {
                    s.setMoney(Double.parseDouble(v));
                    slotEditor(player13, index);
                } else {
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.positive_real_number"), player13);
                }
            });
        });
        inv.set(12, new ItemEditor(localeConfig.getString("gui.slot_editor.set_points"), Material.EMERALD, 1).setLore(localeConfig.getStringList("gui.slot_editor.set_points_info").stream().map(s5 -> s5.replace("{current}", Integer.toString(s.getPoints()))).collect(Collectors.toList())).getItem(), (player14, inventory, inventoryAction, i, itemStack, clickType) -> {
            ChatListener.prompt(player14, localeConfig.getString("gui.slot_editor.set_points_msg"), v -> {
                if (RegEx.POSITIVE_INTEGER.valid(v)) {
                    s.setPoints(Integer.parseInt(v));
                    slotEditor(player14, index);
                } else {
                    Radiumenu.chat.sendPlayer(localeConfig.getString("messages.positive_integer"), player14);
                }
            });
        });
        inv.set(13, new ItemEditor(localeConfig.getString("gui.slot_editor.auto_close"), Material.PAPER, 1).setLore(localeConfig.getStringList("gui.slot_editor.auto_close_info").stream().map(s5 -> s5.replace("{status}", s.isCloseGUI() ? localeConfig.getString("gui.slot_editor.enabled") : localeConfig.getString("gui.slot_editor.disabled"))).collect(Collectors.toList())).getItem(), (player15, inventory, inventoryAction, i, itemStack, clickType) -> {
            s.setCloseGUI(!s.isCloseGUI());
            slotEditor(player15, index);
        });
        inv.set(14, new ItemEditor(localeConfig.getString("gui.slot_editor.item_reward"), Material.PAPER, 1).setLore(localeConfig.getStringList("gui.slot_editor.item_reward_info").stream().map(s5 -> s5.replace("{status}", s.isRewardItem() ? localeConfig.getString("gui.slot_editor.enabled") : localeConfig.getString("gui.slot_editor.disabled"))).collect(Collectors.toList())).getItem(), (player16, inventory, inventoryAction, i, itemStack, clickType) -> {
            s.setRewardItem(!s.isRewardItem());
            slotEditor(player16, index);
        });
        inv.set(15, new ItemEditor(localeConfig.getString("gui.slot_editor.cmd"), XMaterial.COMMAND_BLOCK, 1).getItem(), (player18, inventory, inventoryAction, i, itemStack, clickType) -> slotCommandEditor(player18, index, 0));
        inv.set(16, new ItemEditor(localeConfig.getString("gui.slot_editor.tradeitems"), Material.ENDER_CHEST, 1).setLore(localeConfig.getStringList("gui.slot_editor.tradeitems_info")).getItem(), (player17, inventory, inventoryAction, i, itemStack, clickType) -> tradeItemsEditor(player17, index, 0));
        inv.set(19, new ItemEditor(localeConfig.getString("gui.slot_editor.limit_transaction"), Material.BOOK, 1).setLore(localeConfig.getStringList("gui.slot_editor.limit_transaction_info").stream().map(s5 -> s5.replace("{current}", Integer.toString(s.getLimitTransaction())).replace("{total}", Integer.toString(s.getTotalTransactions()))).collect(Collectors.toList())).getItem(), (player12, inventory, action, slot, item, click) -> {
            if (click.isLeftClick()) {
                ChatListener.prompt(player12, localeConfig.getString("gui.slot_editor.limit_transaction_msg"), v -> {
                    if (RegEx.POSITIVE_INTEGER.valid(v)) {
                        s.setLimitTransaction(Integer.parseInt(v));
                        slotEditor(player12, index);
                    } else {
                        Radiumenu.chat.sendPlayer(localeConfig.getString("messages.positive_integer"), player12);
                    }
                });
            } else if (click.isRightClick()) {
                s.getTransactionCounter().clear();
                slotEditor(player12, index);
            }
        });
        inv.set(20, new ItemEditor(localeConfig.getString("gui.slot_editor.skull_owner"), XMaterial.PLAYER_HEAD, 1).setLore(localeConfig.getStringList("gui.slot_editor.skull_owner_info").stream().map(s5 -> s5.replace("{current}", s.getSkullOwner())).collect(Collectors.toList())).getItem(), (player16, inventory, inventoryAction, i, itemStack, clickType) -> {
            ChatListener.prompt(player16,  localeConfig.getString("gui.slot_editor.skull_owner_msg"), v -> {
                s.setSkullOwner(v);
                slotEditor(player16, index);
            });
        });
        inv.set(21, new ItemEditor(localeConfig.getString("gui.slot_editor.perm_required"), Material.PAPER, 1).setLore(localeConfig.getStringList("gui.slot_editor.perm_required_info").stream().map(s5 -> s5.replace("{status}", s.isPermissionRequired() ? localeConfig.getString("gui.slot_editor.enabled") : localeConfig.getString("gui.slot_editor.disabled"))).collect(Collectors.toList())).getItem(), (player16, inventory, inventoryAction, i, itemStack, clickType) -> {
            if(clickType.isLeftClick()) {
                s.setPermissionRequired(!s.isPermissionRequired());
                slotEditor(player16, index);
            } else if(clickType.isRightClick()) {
                player16.closeInventory();
                player16.spigot().sendMessage(new ComponentBuilder(TextComponent.class).text("rdm.slot." + GUI.id + "." + index, new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "rdm.slot." + GUI.id + "." + index)).build());
            }
        });
        inv.set(31, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player17, inventory, inventoryAction, i, itemStack, clickType) -> editor(player17, 0));
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 4.0f, 3.0f);
    }

    private void tradeItemsEditor(Player player, int slot, int index) {
        InventoryEditor inv = new InventoryEditor(localeConfig.getString("gui.slot_tradeitems_editor.title").replace("{index}", Integer.toString(slot)), 54);
        inv.fill(Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
        Slot s = GUI.getSlot(slot);

        new PaginationStream<>(s.getTradeItems(), index, 45).forEach((i, j, u) -> {
            inv.set(i, new ItemEditor(u.clone()).addLore(localeConfig.getString("gui.slot_tradeitems_editor.info")).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> {
                s.getTradeItems().remove(j);
                tradeItemsEditor(player, slot, 0);
            });
        });
        inv.set(46, new ItemEditor(localeConfig.getString("gui.pagination.prev"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i13, itemStack, clickType) -> tradeItemsEditor(player1, slot, index - 1));
        inv.set(48, new ItemEditor(localeConfig.getString("gui.pagination.next"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i12, itemStack, clickType) -> tradeItemsEditor(player1, slot, index + 1));
        inv.set(50, new ItemEditor(localeConfig.getString("gui.slot_tradeitems_editor.add"), Material.EMERALD, 1).getItem(), (player12, inventory, inventoryAction, i14, itemStack, clickType) -> slotTradeItemAdder(player12, slot));
        inv.set(52, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player12, inventory, inventoryAction, i14, itemStack, clickType) -> slotEditor(player12, slot));
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 4.0f, 3.0f);
    }

    private void slotTradeItemAdder(Player player, int index) {
        InventoryEditor s = new InventoryEditor(localeConfig.getString("gui.slot_tradeitems_adder.title").replace("{index}", Integer.toString(index)), 9);
        for (int i = 0; i < 9; ++i) {
            if (i == 8) {
                s.set(i, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> slotEditor(player, index));
                continue;
            }
            if (i == 6) {
                s.set(i, new ItemEditor(localeConfig.getString("gui.slot_tradeitems_adder.done"), Material.DIAMOND, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> {
                    if (!ItemUtils.isNull(inventory.getItem(0))) {
                        GUI.getSlot(index).getTradeItems().add(inventory.getItem(0));
                    }
                    tradeItemsEditor(player, index, 0);
                });
                continue;
            }
            if (i == 0) {
                s.set(i, null);
                continue;
            }
            s.set(i, new ItemEditor(localeConfig.getString("gui.slot_tradeitems_adder.notice"), XMaterial.OAK_SIGN, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> {});
        }
        s.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 4.0f, 3.0f);
    }

    private void slotCommandEditor(Player player, final int slot, int index) {
        InventoryEditor inv = new InventoryEditor(localeConfig.getString("gui.slot_cmd_editor.title").replace("{index}", Integer.toString(slot)), 54);
        inv.fill(Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});
        final Slot s = GUI.getSlot(slot);

        new PaginationStream<>(s.getCommands(), index, 45).forEach((i, j, u) -> {
            inv.set(i, new ItemEditor("&e" + u, Material.PAPER, 1).setLore(localeConfig.getStringList("gui.slot_cmd_editor.info")).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> {
                s.getCommands().remove(j);
                slotCommandEditor(player, slot, 0);
            });
        });

        inv.set(46, new ItemEditor(localeConfig.getString("gui.pagination.prev"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i13, itemStack, clickType) -> slotCommandEditor(player1, slot, index - 1));
        inv.set(48, new ItemEditor(localeConfig.getString("gui.pagination.next"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i12, itemStack, clickType) -> slotCommandEditor(player1, slot, index + 1));
        inv.set(50, new ItemEditor(localeConfig.getString("gui.slot_cmd_editor.add"), Material.EMERALD, 1)
                .setLore(localeConfig.getStringList("gui.slot_cmd_editor.add_info")).getItem(), (player12, inventory, inventoryAction, i14, itemStack, clickType) -> {
            ChatListener.prompt(player12, localeConfig.getString("gui.slot_cmd_editor.add_msg"), v -> {
                s.getCommands().add(v);
                slotCommandEditor(player12, slot, 0);
            });
        });
        inv.set(52, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player12, inventory, inventoryAction, i14, itemStack, clickType) -> slotEditor(player12, slot));
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 4.0f, 3.0f);
    }

    private void guiCommandEditor(Player player, int index) {
        InventoryEditor inv = new InventoryEditor(localeConfig.getString("gui.gui_cmd_editor.title"), 54);
        inv.fill(Radiumenu.backgroundItem, (player1, inventory, inventoryAction, i, itemStack, clickType) -> {});

        new PaginationStream<>(new ArrayList<>(GUI.getCommands().keySet()), index, 45).forEach((i, j, u) -> {
            inv.set(i, new ItemEditor("&e" + u, Material.PAPER, 1).setLore(localeConfig.getStringList("gui.gui_cmd_editor.info")).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> {
                GUI.destroyCmd(Radiumenu.getInstance(), u);
                guiCommandEditor(player, 0);
            });
        });

        inv.set(46, new ItemEditor(localeConfig.getString("gui.pagination.prev"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i13, itemStack, clickType) -> guiCommandEditor(player1, index - 1));
        inv.set(48, new ItemEditor(localeConfig.getString("gui.pagination.next"), Material.ARROW, 1).getItem(), (player1, inventory, inventoryAction, i12, itemStack, clickType) -> guiCommandEditor(player1, index + 1));
        inv.set(50, new ItemEditor(localeConfig.getString("gui.gui_cmd_editor.add"), Material.EMERALD, 1).setLore(localeConfig.getStringList("gui.gui_cmd_editor.info")).getItem(), (player12, inventory, inventoryAction, i14, itemStack, clickType) -> {
            ChatListener.prompt(player, localeConfig.getString("gui.gui_cmd_editor.add_msg"), v -> {
                GUI.initCmd(v);
                guiCommandEditor(player12, 0);
            });
        });
        inv.set(52, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player12, inventory, inventoryAction, i14, itemStack, clickType) -> editor(player12, 0));
        inv.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 4.0f, 3.0f);
    }

    private void editItemSlot(Player player, int index) {
        InventoryEditor s = new InventoryEditor(localeConfig.getString("gui.slot_item_editor.title").replace("{index}", Integer.toString(index)), 9);
        for (int i = 0; i < 9; ++i) {
            if (i == 8) {
                s.set(i, new ItemEditor(localeConfig.getString("gui.back"), Material.BARRIER, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> slotEditor(player, index));
                continue;
            }
            if (i == 6) {
                s.set(i, new ItemEditor(localeConfig.getString("gui.slot_item_editor.done"), Material.DIAMOND, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> {
                    GUI.getSlot(index).setItem(inventory.getItem(0));
                    slotEditor(player, index);
                });
                continue;
            }
            if (i == 0) {
                s.set(i, GUI.getSlot(index).getRawItem());
                continue;
            }
            s.set(i, new ItemEditor(localeConfig.getString("gui.slot_item_editor.notice"), XMaterial.OAK_SIGN, 1).getItem(), (player1, inventory, inventoryAction, i1, itemStack, clickType) -> {});
        }
        s.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 4.0f, 3.0f);
    }
}
