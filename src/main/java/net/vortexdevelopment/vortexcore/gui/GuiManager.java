package net.vortexdevelopment.vortexcore.gui;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
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
        for (GuiHolder gui : new HashSet<>(openGuis)) {
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
