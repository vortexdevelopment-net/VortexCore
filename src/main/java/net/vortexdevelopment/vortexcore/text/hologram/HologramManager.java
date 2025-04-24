package net.vortexdevelopment.vortexcore.text.hologram;

import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class HologramManager {

    private static final NamespacedKey HOLOGRAM_KEY = new NamespacedKey(VortexPlugin.getInstance(), "hologram");
    private static final NamespacedKey VIEWERS_KEY = new NamespacedKey(VortexPlugin.getInstance(), "hologram_viewers");

    private static Map<Plugin, Set<Hologram>> holograms = new ConcurrentHashMap<>();

    public static void init() {
        //Create a bukkit scheduler task to tick all holograms
        Bukkit.getScheduler().runTaskTimerAsynchronously(VortexPlugin.getInstance(), () -> {
            for (Set<Hologram> hologramsSet : holograms.values()) {
                for (Hologram hologram : hologramsSet) {
                    hologram.tickAsync();
                }
            }
        }, 0, 10);
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
        //Check if we at the main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), () -> createHologram(hologram));
            return;
        }

        holograms.computeIfAbsent(VortexPlugin.getInstance(), k -> ConcurrentHashMap.newKeySet()).add(hologram);
        //Summon the hologram to the world
        Location location = hologram.getLocation();

        //We need one armor stand for each line, use a gap of 0.25 blocks for each line
        for (int i = 0; i < hologram.getLines().size(); i++) {
            ArmorStand armorStand = location.getWorld().createEntity(location.clone().add(0, i * 0.25, 0), ArmorStand.class);
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setMarker(true);
            armorStand.setCollidable(false);

            //AdventureUtils.setCustomEntityName(armorStand, hologram.getLines().get(i)); //Makes custom name visible too
            armorStand.customName(AdventureUtils.formatComponent(hologram.getLines().get(i)));
            armorStand.setCustomNameVisible(true);

            PersistentDataContainer data = armorStand.getPersistentDataContainer();
            data.set(HOLOGRAM_KEY, PersistentDataType.STRING, hologram.getId());
            if (hologram.useViewers()) {
                //Make only the players in the viewers list see the hologram
                armorStand.setVisibleByDefault(false);
                for (UUID uuid : hologram.getViewers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.showEntity(VortexPlugin.getInstance(), armorStand);
                    }
                }
                //Set viewers to the armor stand persistent data
                data.set(VIEWERS_KEY, PersistentDataType.LIST.strings(), hologram.getViewers().stream().map(UUID::toString).toList());
            } else {
                //Show it to all players
                armorStand.setVisibleByDefault(true);
            }

            //Show the hologram to the world
            armorStand.getWorld().addEntity(armorStand);
            hologram.getArmorStands().add(armorStand);
        }
    }

    public static ArmorStand createArmorStand(Hologram hologram) {
        //Check if we at the main thread
        if (!Bukkit.isPrimaryThread()) {
            Callable<ArmorStand> callable = () -> createArmorStand(hologram);
            try {
                return Bukkit.getScheduler().callSyncMethod(VortexPlugin.getInstance(), callable).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ArmorStand armorStand = hologram.getLocation().getWorld().createEntity(hologram.getLocation(), ArmorStand.class);
        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setMarker(true);
        armorStand.setCollidable(false);

        //Set viewers to the armor stand persistent data
        PersistentDataContainer data = armorStand.getPersistentDataContainer();
        data.set(HOLOGRAM_KEY, PersistentDataType.STRING, hologram.getId());
        if (hologram.useViewers()) {
            //Make only the players in the viewers list see the hologram
            armorStand.setVisibleByDefault(false);
            for (UUID uuid : hologram.getViewers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.showEntity(VortexPlugin.getInstance(), armorStand);
                }
            }
            //Set viewers to the armor stand persistent data
            data.set(VIEWERS_KEY, PersistentDataType.LIST.strings(), hologram.getViewers().stream().map(UUID::toString).toList());
        } else {
            //Show it to all players
            armorStand.setVisibleByDefault(true);
        }

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
        if (!Bukkit.isPrimaryThread() && !Bukkit.isStopping()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), HologramManager::clear);
            return;
        }
        holograms.clear();
        //get all entities with the hologram key from all worlds
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand armorStand : world.getEntitiesByClass(ArmorStand.class)) {
                PersistentDataContainer data = armorStand.getPersistentDataContainer();
                if (data.has(HOLOGRAM_KEY, PersistentDataType.STRING)) {
                    armorStand.remove();
                }
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
            List<UUID> oldViewers = data.get(VIEWERS_KEY, PersistentDataType.LIST.strings()).stream().map(UUID::fromString).toList();
            for (UUID uuid : oldViewers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.hideEntity(VortexPlugin.getInstance(), armorStand);
                }
            }

            data.set(VIEWERS_KEY, PersistentDataType.LIST.strings(), hologram.getViewers().stream().map(UUID::toString).toList());
            for (UUID uuid : hologram.getViewers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.showEntity(VortexPlugin.getInstance(), armorStand);
                }
            }
        }
    }
}
