package net.vortexdevelopment.vortexcore.platform.spigot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.chat.ComponentSerializer;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@net.vortexdevelopment.vinject.annotation.component.Component
public class SpigotBukkitAdventureBridge implements BukkitAdventureBridge {

    /** CraftBukkit exposes {@code isStopping()} at runtime; it is not part of the Spigot API. */
    private static final Method BUKKIT_IS_STOPPING = resolveBukkitIsStopping();

    private static Method resolveBukkitIsStopping() {
        try {
            return Bukkit.class.getMethod("isStopping");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /** Classic § codes only (no hex); matches what Spigot item names, lore, and inventory titles accept well. */
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder().hexColors().hexCharacter('§').useUnusualXRepeatedCharacterHexFormat().build();
    private static final GsonComponentSerializer GSON_CHAT = GsonComponentSerializer.gson();

    @PostConstruct
    public void registerBridge() {
        BukkitAdventureBridges.install(this);
    }

    @Override
    public void applyItemName(ItemMeta meta, Component name) {
        if (meta == null || name == null) {
            return;
        }
        meta.setDisplayName(LEGACY_SECTION.serialize(name.decoration(TextDecoration.ITALIC, false)));
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
        List<String> lines = lore.stream().map(LEGACY_SECTION::serialize).collect(Collectors.toCollection(ArrayList::new));
        meta.setLore(lines);
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
        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            return List.of();
        }
        List<Component> out = new ArrayList<>();
        for (String line : meta.getLore()) {
            out.add(LEGACY_SECTION.deserialize(line));
        }
        return out;
    }

    @Override
    public boolean hasItemLore(ItemMeta meta) {
        return meta != null && meta.hasLore() && meta.getLore() != null && !meta.getLore().isEmpty();
    }

    @Override
    public boolean hasItemName(ItemMeta meta) {
        return meta != null && meta.hasDisplayName();
    }

    @Override
    public @Nullable Component getItemName(ItemMeta meta) {
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        return LEGACY_SECTION.deserialize(meta.getDisplayName());
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, int size, Component title) {
        return Bukkit.createInventory(owner, size, LEGACY_SECTION.serialize(title));
    }

    @Override
    public void sendComponentMessage(CommandSender sender, Component message) {
        try {
            String json = GSON_CHAT.serialize(message);
            net.md_5.bungee.api.chat.BaseComponent[] parts = ComponentSerializer.parse(json);
            if (parts.length == 0) {
                sender.sendMessage(LEGACY_SECTION.serialize(message));
            } else {
                sender.spigot().sendMessage(parts);
            }
        } catch (Throwable ignored) {
            sender.sendMessage(LEGACY_SECTION.serialize(message));
        }
    }

    @Override
    public void setEntityCustomName(LivingEntity entity, Component name) {
        entity.setCustomName(LEGACY_SECTION.serialize(name));
    }

    @Override
    public void teleportLivingEntity(LivingEntity entity, Location destination) {
        entity.teleport(destination);
    }

    @Override
    public boolean isServerStopping() {
        Method m = BUKKIT_IS_STOPPING;
        if (m == null) {
            return false;
        }
        try {
            Object r = m.invoke(null);
            return r instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
