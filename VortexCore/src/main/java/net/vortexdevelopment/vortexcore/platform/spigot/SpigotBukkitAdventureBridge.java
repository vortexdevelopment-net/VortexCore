package net.vortexdevelopment.vortexcore.platform.spigot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.compatibility.ServerProject;
import net.vortexdevelopment.vortexcore.spi.BukkitAdventureBridge;
import net.vortexdevelopment.vortexcore.spi.BukkitAdventureBridges;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    /**
     * §-based legacy for chat fallbacks. Does not preserve {@link net.kyori.adventure.text.TranslatableComponent}
     * (serializes to the raw translation key) — never use for entity custom names, items, or inventory titles
     * when components may include translatables; use {@link #componentToChatJson} instead.
     */
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder().hexColors().hexCharacter('§').useUnusualXRepeatedCharacterHexFormat().build();
    private static final GsonComponentSerializer GSON_CHAT = GsonComponentSerializer.gson();

    private static String componentToChatJson(Component component) {
        return GSON_CHAT.serialize(component);
    }

    /**
     * {@link org.bukkit.entity.Entity#setCustomName(String)} and {@link ItemMeta#setDisplayName(String)} use
     * {@code CraftChatMessage.fromStringOrNull} (legacy § parsing only). JSON must go through
     * {@code CraftChatMessage.fromJSONOrString}, or item meta through {@link #setDisplayNameComponentReflect}.
     */
    private static @Nullable Object craftChatFromJsonOrString(String message, boolean keepNewlines) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        try {
            Class<?> ccm = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");
            Method m = ccm.getMethod("fromJSONOrString", String.class, boolean.class);
            return m.invoke(null, message, keepNewlines);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Same path as {@link #sendComponentMessage}: Adventure JSON → Bungee → vanilla codec. Use when
     * {@link #craftChatFromJsonOrString} returns null (strict codec rejects some Adventure output).
     */
    private static @Nullable Object craftChatFromBungeeJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            BaseComponent[] parts = ComponentSerializer.parse(json);
            Class<?> ccm = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage");
            Method m = ccm.getMethod("bungeeToVanilla", BaseComponent[].class);
            return m.invoke(null, (Object) parts);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean tryPaperCustomName(Entity entity, Component name) {
        try {
            Method m = entity.getClass().getMethod("customName", Component.class);
            m.invoke(entity, name);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean setNmsCustomName(Entity entity, Object nmsChatComponent) {
        try {
            Class<?> craftEntityClass = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
            Object craftEntity = craftEntityClass.cast(entity);
            Method getHandle = craftEntityClass.getMethod("getHandle");
            Object handle = getHandle.invoke(craftEntity);
            for (Method m : handle.getClass().getMethods()) {
                if (!"setCustomName".equals(m.getName()) || m.getParameterCount() != 1) {
                    continue;
                }
                Class<?> p = m.getParameterTypes()[0];
                if (p == Optional.class || p == Object.class || p.isPrimitive() || p == String.class) {
                    continue;
                }
                if (!p.isInstance(nmsChatComponent)) {
                    continue;
                }
                m.invoke(handle, nmsChatComponent);
                return true;
            }
            try {
                Method m = handle.getClass().getMethod("setCustomName", Optional.class);
                m.invoke(handle, Optional.of(nmsChatComponent));
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        } catch (ReflectiveOperationException e) {
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    private static void setDisplayNameComponentReflect(ItemMeta meta, Component name) {
        String json = componentToChatJson(name.decoration(TextDecoration.ITALIC, false));
        BaseComponent[] parts = ComponentSerializer.parse(json);
        try {
            Method m = meta.getClass().getMethod("setDisplayNameComponent", BaseComponent[].class);
            m.invoke(meta, (Object) parts);
        } catch (ReflectiveOperationException e) {
            meta.setDisplayName(LEGACY_SECTION.serialize(name.decoration(TextDecoration.ITALIC, false)));
        }
    }

    private static void setLoreComponentsReflect(ItemMeta meta, List<Component> lore) {
        List<BaseComponent[]> lines = new ArrayList<>(lore.size());
        for (Component line : lore) {
            lines.add(ComponentSerializer.parse(componentToChatJson(
                    line.style(builder -> builder.decoration(TextDecoration.ITALIC, false)))));
        }
        try {
            Method m = meta.getClass().getMethod("setLoreComponents", List.class);
            m.invoke(meta, lines);
        } catch (ReflectiveOperationException e) {
            meta.setLore(lore.stream()
                    .map(line -> LEGACY_SECTION.serialize(line.decoration(TextDecoration.ITALIC, false)))
                    .collect(Collectors.toList()));
        }
    }

    private static @Nullable Component getItemNameReflect(ItemMeta meta) {
        try {
            Method getParts = meta.getClass().getMethod("getDisplayNameComponent");
            BaseComponent[] parts = (BaseComponent[]) getParts.invoke(meta);
            if (parts == null || parts.length == 0) {
                return null;
            }
            return GSON_CHAT.deserialize(ComponentSerializer.toString(parts));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable List<Component> getLoreComponentsReflect(ItemMeta meta) {
        try {
            Method getLoreComponents = meta.getClass().getMethod("getLoreComponents");
            @SuppressWarnings("unchecked")
            List<BaseComponent[]> raw = (List<BaseComponent[]>) getLoreComponents.invoke(meta);
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            List<Component> out = new ArrayList<>(raw.size());
            for (BaseComponent[] parts : raw) {
                if (parts == null || parts.length == 0) {
                    out.add(Component.empty());
                } else {
                    out.add(GSON_CHAT.deserialize(ComponentSerializer.toString(parts)));
                }
            }
            return out;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Item meta and custom names may store either JSON chat or legacy § strings depending on source.
     */
    private static Component componentFromSpigotStorage(@Nullable String raw) {
        if (raw == null) {
            return Component.empty();
        }
        if (!raw.isEmpty() && raw.charAt(0) == '{') {
            try {
                return GSON_CHAT.deserialize(raw);
            } catch (Throwable ignored) {
                // fall through to legacy
            }
        }
        return LEGACY_SECTION.deserialize(raw);
    }

    @PostConstruct
    public void registerBridge() {
        if (ServerProject.isServer(ServerProject.PAPER)) {
            return;
        }
        BukkitAdventureBridges.install(this);
    }

    @Override
    public void applyItemName(ItemMeta meta, Component name) {
        if (meta == null || name == null) {
            return;
        }
        setDisplayNameComponentReflect(meta, name);
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
        setLoreComponentsReflect(meta, lore);
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
        List<Component> fromComponents = getLoreComponentsReflect(meta);
        if (fromComponents != null) {
            return fromComponents;
        }
        for (String line : meta.getLore()) {
            out.add(componentFromSpigotStorage(line));
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
        Component fromParts = getItemNameReflect(meta);
        if (fromParts != null) {
            return fromParts;
        }
        return componentFromSpigotStorage(meta.getDisplayName());
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, int size, Component title) {
        try {
            Class<?> clazz = Class.forName("org.bukkit.craftbukkit.inventory.CraftInventoryCustom");
            Constructor<?> ctor = clazz.getConstructor(InventoryHolder.class, int.class, Component.class);
            return (Inventory) ctor.newInstance(owner, size, title);
        } catch (Throwable ignored) {
            Object nms = craftChatFromJsonOrString(componentToChatJson(title), false);
            if (nms != null) {
                try {
                    Class<?> clazz = Class.forName("org.bukkit.craftbukkit.inventory.util.CraftInventoryCreator");
                    Object instance = clazz.getField("INSTANCE").get(null);
                    for (Method m : instance.getClass().getMethods()) {
                        if (!"createInventory".equals(m.getName()) || m.getParameterCount() != 3) {
                            continue;
                        }
                        Class<?>[] ps = m.getParameterTypes();
                        if (ps[0] != InventoryHolder.class || ps[1] != int.class) {
                            continue;
                        }
                        if (ps[2].isAssignableFrom(nms.getClass())) {
                            return (Inventory) m.invoke(instance, owner, size, nms);
                        }
                    }
                } catch (Throwable ignored2) {
                    // fall through
                }
            }
            return Bukkit.createInventory(owner, size, LEGACY_SECTION.serialize(title));
        }
    }

    @Override
    public void sendComponentMessage(CommandSender sender, Component message) {
        VortexPlugin plugin = VortexPlugin.getInstance();
        if (plugin != null && plugin.sendAudienceMessage(sender, message)) {
            return;
        }
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
    public void setCustomName(Entity entity, Component name) {
        if (name == null) {
            entity.setCustomName(null);
            return;
        }
        if (tryPaperCustomName(entity, name)) {
            return;
        }
        String json = componentToChatJson(name);
        Object nms = craftChatFromJsonOrString(json, false);
        if (nms == null) {
            nms = craftChatFromBungeeJson(json);
        }
        if (nms != null && setNmsCustomName(entity, nms)) {
            return;
        }
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
