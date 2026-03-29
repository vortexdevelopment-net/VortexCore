package net.vortexdevelopment.vortexcore.platform.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import net.vortexdevelopment.vortexcore.spi.BukkitAdventureBridge;
import net.vortexdevelopment.vortexcore.spi.BukkitAdventureBridges;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@net.vortexdevelopment.vinject.annotation.component.Component
public class PaperBukkitAdventureBridge implements BukkitAdventureBridge {

    @PostConstruct
    public void registerBridge() {
        BukkitAdventureBridges.install(this);
    }

    @Override
    public void applyItemName(ItemMeta meta, Component name) {
        if (name == null || meta == null) {
            return;
        }
        try {
            meta.itemName(name);
        } catch (Throwable e) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        }
    }

    @Override
    public void applyItemName(ItemStack item, Component name) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        applyItemName(meta, name);
        item.setItemMeta(meta);
    }

    @Override
    public void applyItemLore(ItemMeta meta, List<Component> lore) {
        if (meta == null || lore == null || lore.isEmpty()) {
            return;
        }
        meta.lore(lore);
    }

    @Override
    public void applyItemLore(ItemStack item, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        applyItemLore(meta, lore);
        item.setItemMeta(meta);
    }

    @Override
    public List<Component> getItemLore(ItemMeta meta) {
        if (meta == null || !meta.hasLore()) {
            return List.of();
        }
        List<Component> lore = meta.lore();
        return lore == null ? List.of() : new ArrayList<>(lore);
    }

    @Override
    public boolean hasItemLore(ItemMeta meta) {
        return meta != null && meta.hasLore();
    }

    @Override
    public boolean hasItemName(ItemMeta meta) {
        return meta != null && (meta.hasItemName() || meta.hasDisplayName());
    }

    @Override
    public @Nullable Component getItemName(ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        if (meta.hasItemName()) {
            return meta.itemName();
        }
        if (meta.hasDisplayName()) {
            return meta.displayName();
        }
        return null;
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, int size, Component title) {
        return Bukkit.createInventory(owner, size, title);
    }

    @Override
    public void sendComponentMessage(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }

    @Override
    public void setEntityCustomName(LivingEntity entity, Component name) {
        entity.customName(name);
    }

    @Override
    public void teleportLivingEntity(LivingEntity entity, Location destination) {
        entity.teleportAsync(destination);
    }

    @Override
    public boolean isServerStopping() {
        return Bukkit.isStopping();
    }
}
