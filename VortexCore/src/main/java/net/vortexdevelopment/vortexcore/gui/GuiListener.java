package net.vortexdevelopment.vortexcore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.function.Consumer;

public class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GuiHolder gui) {

            if (gui.cancelClick()) {
                event.setCancelled(true);
            }

            Consumer<InventoryClickEvent> onGlobalClick = gui.getOnGlobalClick();
            Consumer<InventoryClickEvent> onTopClick = gui.getOnTopClick();
            Consumer<InventoryClickEvent> onBottomClick = gui.getOnBottomClick();

            if (onGlobalClick != null) {
                onGlobalClick.accept(event);
            }

            if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                if (onTopClick != null) {
                    onTopClick.accept(event);
                }
            } else {
                if (onBottomClick != null) {
                    onBottomClick.accept(event);
                }
            }

            if (gui instanceof PaginatedGui paginatedGui) {
                paginatedGui.handleClick(event);
                return;
            }

            GuiItem item = gui.getItem(event.getSlot());

            if (item != null) {
                item.onClick(event, gui, item);
                if (item.shouldUpdate()) {
                    gui.updateItem(item);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GuiHolder gui) {

            Consumer<InventoryClickEvent> onGlobalDrag = gui.getOnGlobalDrag();
            Consumer<InventoryClickEvent> onTopDrag = gui.getOnTopDrag();
            Consumer<InventoryClickEvent> onBottomDrag = gui.getOnBottomDrag();

            if (onGlobalDrag != null) {
                onGlobalDrag.accept(event);
            }

            if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                if (onTopDrag != null) {
                    onTopDrag.accept(event);
                }
            } else {
                if (onBottomDrag != null) {
                    onBottomDrag.accept(event);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GuiHolder gui) {

            Consumer<InventoryCloseEvent> onClose = gui.getOnClose();

            if (onClose != null) {
                onClose.accept(event);
            }

            gui.onClose((Player) event.getPlayer());
        }
    }
}
