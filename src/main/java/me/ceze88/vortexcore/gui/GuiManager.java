package me.ceze88.vortexcore.gui;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager {

    private static final GuiListener inventoryListener = new GuiListener();
    private static final Set<Gui> openGuis = ConcurrentHashMap.newKeySet();

    public static void markAsOpen(Gui gui) {
        openGuis.add(gui);
    }

    public static void markAsClosed(Gui gui) {
        openGuis.remove(gui);
    }

    public static void disable() {
        for (Gui gui : new HashSet<>(openGuis)) {
            gui.closeAll();
        }
    }

    public static void register(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(inventoryListener, plugin);
    }

    public static void unregister(Plugin plugin) {
        disable();
        HandlerList.unregisterAll(inventoryListener);
    }
}
