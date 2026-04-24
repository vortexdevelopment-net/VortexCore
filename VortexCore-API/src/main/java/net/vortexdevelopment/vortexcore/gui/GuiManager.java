package net.vortexdevelopment.vortexcore.gui;

import net.vortexdevelopment.vortexcore.VortexPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager {

    private static final GuiListener inventoryListener = new GuiListener();
    private static final Set<GuiHolder> openGuis = ConcurrentHashMap.newKeySet();

    public static void markAsOpen(GuiHolder gui) {
        openGuis.add(gui);
    }

    public static void markAsClosed(GuiHolder gui) {
        openGuis.remove(gui);
    }

    public static void disable() {
        // Snapshot so closeAll / events cannot mutate the set we iterate; also used to find stragglers.
        Set<GuiHolder> snapshot = Set.copyOf(openGuis);
        for (GuiHolder gui : snapshot) {
            gui.closeAll();
        }
        // If openers ever desynced from the real client view, close by top-inventory holder.
        // Reflection: InventoryView was a class on older servers and is an interface on newer API;
        // compiling against either breaks the other with IncompatibleClassChangeError.
        if (Bukkit.isPrimaryThread()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory top = openTopInventory(player);
                if (top == null) {
                    continue;
                }
                InventoryHolder holder = top.getHolder();
                if (holder instanceof GuiHolder gh && snapshot.contains(gh)) {
                    player.closeInventory();
                }
            }
        }
        openGuis.clear();
    }

    public static void register(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(inventoryListener, plugin);
        //Schedule taks to auto update items in open GUIs
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (GuiHolder gui : openGuis) {
                gui.autoUpdate();
            }
        }, 0L, 20L);
    }

    public static void unregister(Plugin plugin) {
        disable();
        HandlerList.unregisterAll(inventoryListener);
    }

    /**
     * For compatibility with older servers, we need to use reflection to get the top inventory because they changed the inventory view class to an interface.
     * @param player The player to get the top inventory of.
     * @return The top inventory of the player, or null if the player is not open.
     */
    private static Inventory openTopInventory(Player player) {
        try {
            Method getOpen = Player.class.getMethod("getOpenInventory");
            Object view = getOpen.invoke(player);
            if (view == null) {
                return null;
            }
            Method getTop = view.getClass().getMethod("getTopInventory");
            Object top = getTop.invoke(view);
            return top instanceof Inventory inv ? inv : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
