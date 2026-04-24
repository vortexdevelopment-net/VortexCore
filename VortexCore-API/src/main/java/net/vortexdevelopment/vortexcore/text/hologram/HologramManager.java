package net.vortexdevelopment.vortexcore.text.hologram;

import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.compatibility.KnownServerVersions;
import net.vortexdevelopment.vortexcore.compatibility.ServerVersion;
import net.vortexdevelopment.vortexcore.spi.BukkitAdventureBridges;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.text.MiniMessagePlaceholder;
import net.vortexdevelopment.vortexcore.text.lang.Lang;
import net.vortexdevelopment.vortexcore.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class HologramManager {

    private static final UUID sessionId = UUID.randomUUID();

    /** Comma-separated UUID strings; empty string means no viewers. */
    private static final String VIEWERS_DELIMITER = ",";

    private static final NamespacedKey SESSION_ID_KEY = new NamespacedKey(VortexPlugin.getInstance(), "hologram_session_id");
    private static final NamespacedKey HOLOGRAM_KEY = new NamespacedKey(VortexPlugin.getInstance(), "hologram");
    private static final NamespacedKey VIEWERS_KEY = new NamespacedKey(VortexPlugin.getInstance(), "hologram_viewers");

    private static final Map<Plugin, Set<Hologram>> holograms = new ConcurrentHashMap<>();

    private static final @Nullable Method WORLD_CREATE_ENTITY;
    private static final @Nullable Method WORLD_ADD_ENTITY;
    private static final @Nullable Method ENTITY_IS_IN_WORLD;
    private static final @Nullable Method ENTITY_SET_VISIBLE_BY_DEFAULT;

    static {
        WORLD_CREATE_ENTITY = resolveMethod(World.class, "createEntity", Location.class, Class.class);
        WORLD_ADD_ENTITY = resolveMethod(World.class, "addEntity", Entity.class);
        ENTITY_IS_IN_WORLD = resolveMethod(Entity.class, "isInWorld");
        ENTITY_SET_VISIBLE_BY_DEFAULT = resolveMethod(Entity.class, "setVisibleByDefault", boolean.class);
    }

    private static @Nullable Method resolveMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static String encodeViewers(List<UUID> viewers) {
        if (viewers == null || viewers.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < viewers.size(); i++) {
            if (i > 0) {
                sb.append(VIEWERS_DELIMITER);
            }
            sb.append(viewers.get(i).toString());
        }
        return sb.toString();
    }

    private static List<UUID> decodeViewers(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>();
        for (String part : raw.split(VIEWERS_DELIMITER)) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // skip malformed token
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static void applyVisibleByDefault(Entity entity, boolean visible) {
        if (ENTITY_SET_VISIBLE_BY_DEFAULT == null) {
            return;
        }
        try {
            ENTITY_SET_VISIBLE_BY_DEFAULT.invoke(entity, visible);
        } catch (ReflectiveOperationException ignored) {
            // If the runtime rejects the call, fall back to per-player hide/show only
        }
    }

    /**
     * Per-viewer visibility: uses {@code setVisibleByDefault} when the server API exposes it; otherwise
     * {@link Player#hideEntity}/{@link Player#showEntity} for every online player.
     */
    private static void applyHologramViewerVisibility(ArmorStand stand, Hologram hologram) {
        if (hologram.useViewers()) {
            applyVisibleByDefault(stand, false);
            for (UUID uuid : hologram.getViewers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.showEntity(VortexPlugin.getInstance(), stand);
                }
            }
            if (ENTITY_SET_VISIBLE_BY_DEFAULT == null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!hologram.getViewers().contains(player.getUniqueId())) {
                        player.hideEntity(VortexPlugin.getInstance(), stand);
                    }
                }
            }
        } else {
            applyVisibleByDefault(stand, true);
            if (ENTITY_SET_VISIBLE_BY_DEFAULT == null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.showEntity(VortexPlugin.getInstance(), stand);
                }
            }
        }
    }

    /**
     * When a player joins, re-apply viewer rules (needed for viewer lists and for servers without
     * {@code setVisibleByDefault}).
     */
    public static void onPlayerJoin(Player player) {
        Set<Hologram> set = holograms.get(VortexPlugin.getInstance());
        if (set == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        for (Hologram hologram : set) {
            if (!hologram.useViewers()) {
                continue;
            }
            for (ArmorStand stand : hologram.getArmorStands()) {
                if (hologram.getViewers().contains(uuid)) {
                    player.showEntity(VortexPlugin.getInstance(), stand);
                } else if (ENTITY_SET_VISIBLE_BY_DEFAULT == null) {
                    player.hideEntity(VortexPlugin.getInstance(), stand);
                }
            }
        }
    }

    /**
     * Registers a stand created via {@code World#createEntity} (1.20.3+). No-op when spawn was used or the API is absent.
     */
    public static void registerArmorStandInWorldIfNeeded(ArmorStand stand) {
        if (!ServerVersion.isAtLeastVersion(KnownServerVersions.V1_20_3)) {
            return;
        }
        if (WORLD_ADD_ENTITY == null || ENTITY_IS_IN_WORLD == null) {
            return;
        }
        try {
            if (!(Boolean) ENTITY_IS_IN_WORLD.invoke(stand)) {
                WORLD_ADD_ENTITY.invoke(stand.getWorld(), stand);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prefer {@code World#createEntity} on 1.20.3+ when present; otherwise {@link World#spawn(Location, Class)}.
     */
    private static ArmorStand createArmorStandEntity(Location location, Consumer<ArmorStand> configure) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("location has no world");
        }
        if (ServerVersion.isAtLeastVersion(KnownServerVersions.V1_20_3) && WORLD_CREATE_ENTITY != null) {
            try {
                Object created = WORLD_CREATE_ENTITY.invoke(world, location, ArmorStand.class);
                ArmorStand stand = (ArmorStand) created;
                configure.accept(stand);
                return stand;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        ArmorStand armorStand = world.spawn(location, ArmorStand.class);
        configure.accept(armorStand);
        return armorStand;
    }

    public static void init() {
        clear();
        //Create a bukkit scheduler task to tick all holograms
        Bukkit.getScheduler().runTaskTimerAsynchronously(VortexPlugin.getInstance(), () -> {
            for (Set<Hologram> hologramsSet : holograms.values()) {
                for (Hologram hologram : hologramsSet) {
                    hologram.tickAsync();
                }
            }
        }, 0, 10L);
    }

    public static @Nullable Hologram getHologram(String id) {
        Set<Hologram> hologramsSet = holograms.get(VortexPlugin.getInstance());
        if (hologramsSet != null) {
            for (Hologram hologram : hologramsSet) {
                if (hologram.getId().equals(id)) {
                    return hologram;
                }
            }
        }
        return null;
    }

    public static void createHologram(Hologram hologram) {
        Location location = hologram.getLocation();
        holograms.computeIfAbsent(VortexPlugin.getInstance(), k -> ConcurrentHashMap.newKeySet()).add(hologram);

        // Do not create hologram if the chunk is not loaded
        if (!WorldUtils.isChunkLoadedAtLocation(location)) return;

        //Check if we at the main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), () -> createHologram(hologram));
            return;
        }

        List<MiniMessagePlaceholder> placeholders = new ArrayList<>(hologram.getPlaceholders());
        placeholders.addAll(Lang.staticPlaceholders);

        //Summon the hologram to the world
        // We need one armor stand for each line, use a gap of 0.25 blocks for each line
        // First line (index 0) should be at the top, so we reverse the Y offset
        int lineCount = hologram.getLines().size();
        for (int i = 0; i < lineCount; i++) {
            double yOffset = (lineCount - 1 - i) * 0.25;
            Location lineLoc = location.clone().add(0, yOffset, 0);
            final int lineIndex = i;
            ArmorStand armorStand = createArmorStandEntity(lineLoc, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setMarker(true);
                stand.setCollidable(false);
                stand.setPersistent(false);

                BukkitAdventureBridges.get().setEntityCustomName(stand,
                        AdventureUtils.formatComponent(hologram.getLines().get(lineIndex), placeholders));
                stand.setCustomNameVisible(true);

                PersistentDataContainer data = stand.getPersistentDataContainer();
                data.set(HOLOGRAM_KEY, PersistentDataType.STRING, hologram.getId());
                data.set(SESSION_ID_KEY, PersistentDataType.STRING, sessionId.toString());
                if (hologram.useViewers()) {
                    data.set(VIEWERS_KEY, PersistentDataType.STRING, encodeViewers(hologram.getViewers()));
                }
                applyHologramViewerVisibility(stand, hologram);
            });

            registerArmorStandInWorldIfNeeded(armorStand);
            hologram.getArmorStands().add(armorStand);
        }
    }

    public static ArmorStand createArmorStand(Hologram hologram) {
        return createArmorStand(hologram, hologram.getLocation());
    }

    /**
     * Create an armor stand for the hologram at the given location without adding it to the world
     * @param hologram The hologram to create the armor stand for
     * @param location The location to create the armor stand at
     * @return The created armor stand
     */
    public static ArmorStand createArmorStand(Hologram hologram, Location location) {
        //Check if we at the main thread
        if (!Bukkit.isPrimaryThread()) {
            Callable<ArmorStand> callable = () -> createArmorStand(hologram, location);
            try {
                return Bukkit.getScheduler().callSyncMethod(VortexPlugin.getInstance(), callable).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ArmorStand armorStand = createArmorStandEntity(location, stand -> {
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setCollidable(false);
            stand.setPersistent(false);

            PersistentDataContainer data = stand.getPersistentDataContainer();
            data.set(HOLOGRAM_KEY, PersistentDataType.STRING, hologram.getId());
            if (hologram.useViewers()) {
                data.set(VIEWERS_KEY, PersistentDataType.STRING, encodeViewers(hologram.getViewers()));
            }
            applyHologramViewerVisibility(stand, hologram);
        });

        return armorStand;
    }

    public static void removeHologram(Hologram hologram) {
        Set<Hologram> hologramsSet = holograms.get(VortexPlugin.getInstance());
        if (hologramsSet != null) {
            hologramsSet.remove(hologram);
            hologram.remove();
        }
    }

    public static void clear() {
        if (!Bukkit.isPrimaryThread() && !BukkitAdventureBridges.get().isServerStopping()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), HologramManager::clear);
            return;
        }
        //get all entities with the hologram key from all worlds
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand armorStand : world.getEntitiesByClass(ArmorStand.class)) {
                PersistentDataContainer data = armorStand.getPersistentDataContainer();
                if (data.has(HOLOGRAM_KEY, PersistentDataType.STRING) || data.has(getSessionIdKey(), PersistentDataType.STRING)) {
                    armorStand.remove();
                }
            }
        }
        holograms.clear();
    }

    public static void loadHologramsInChunk(Chunk chunk) {
        for (Hologram hologram : holograms.getOrDefault(VortexPlugin.getInstance(), Set.of())) {
            Location location = hologram.getLocation();
            if (WorldUtils.isLocationAtChunk(location, chunk)) {
                createHologram(hologram);
            }
        }
    }

    public static void unloadHologramsInChunk(Chunk chunk) {
        for (Hologram hologram : holograms.getOrDefault(VortexPlugin.getInstance(), Set.of())) {
            Location location = hologram.getLocation();
            if (WorldUtils.isLocationAtChunk(location, chunk)) {
                hologram.remove();
            }
        }
    }

    public void updateViewers(Hologram hologram) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), () -> updateViewers(hologram));
            return;
        }
        for (ArmorStand armorStand : hologram.getArmorStands()) {
            PersistentDataContainer data = armorStand.getPersistentDataContainer();
            String rawOld = data.get(VIEWERS_KEY, PersistentDataType.STRING);
            List<UUID> oldViewers = decodeViewers(rawOld);
            for (UUID uuid : oldViewers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.hideEntity(VortexPlugin.getInstance(), armorStand);
                }
            }

            if (hologram.useViewers()) {
                data.set(VIEWERS_KEY, PersistentDataType.STRING, encodeViewers(hologram.getViewers()));
            } else {
                data.remove(VIEWERS_KEY);
            }
            applyHologramViewerVisibility(armorStand, hologram);
        }
    }

    public static NamespacedKey getHologramKey() {
        return HOLOGRAM_KEY;
    }

    public static NamespacedKey getSessionIdKey() {
        return SESSION_ID_KEY;
    }

    public static String getSessionId() {
        return sessionId.toString();
    }
}
