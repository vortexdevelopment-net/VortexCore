package net.vortexdevelopment.vortexcore.spi;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Paper vs Spigot differences for Adventure on Bukkit: items, inventories, messaging, entity names.
 */
public interface BukkitAdventureBridge {

    void applyItemName(ItemMeta meta, Component name);

    void applyItemName(ItemStack item, Component name);

    void applyItemLore(ItemMeta meta, List<Component> lore);

    void applyItemLore(ItemStack item, List<Component> lore);

    List<Component> getItemLore(ItemMeta meta);

    boolean hasItemLore(ItemMeta meta);

    boolean hasItemName(ItemMeta meta);

    @Nullable Component getItemName(ItemMeta meta);

    Inventory createInventory(InventoryHolder owner, int size, Component title);

    void sendComponentMessage(CommandSender sender, Component message);

    void setEntityCustomName(LivingEntity entity, Component name);

    /**
     * Move an entity for holograms / GUIs. Paper may use async teleport; Spigot uses synchronous {@code teleport}.
     */
    void teleportLivingEntity(LivingEntity entity, Location destination);

    /**
     * True while the server is shutting down (Paper). On Spigot without {@code Bukkit.isStopping()}, returns false.
     */
    boolean isServerStopping();
}
