package net.vortexdevelopment.vortexcore.text.hologram;

import it.unimi.dsi.fastutil.Hash;
import lombok.Getter;
import lombok.Setter;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Hologram {

    @Getter private String id;
    @Getter private Location location;
    @Setter private List<String> lines = new ArrayList<>();
    @Getter private List<UUID> viewers = new LinkedList<>(); // List of players who can see the hologram
    @Setter private boolean useViewers = false; // If true, only players in the viewers list can see the hologram
    @Getter @Setter private boolean visible = true;
    @Getter @Setter private List<ArmorStand> armorStands = new ArrayList<>();
    private Map<String, HologramPlaceholder> placeholders = new HashMap<>();
    private boolean shouldUpdate = true; //If true, the hologram lines are changed and need to re-render them

    public Hologram(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public Hologram(String id, Location location, String... lines) {
        this.id = id;
        this.location = location;
        this.lines = new ArrayList<>(List.of(lines));
    }

    public Hologram(String id, Location location, List<String> lines) {
        this.id = id;
        this.location = location;
        this.lines = lines;
    }

    public void registerPlaceholder(HologramPlaceholderProvider placeholder, long updateIntervalTicks) {
        placeholders.put("<" + placeholder.getPlaceholder().getPlaceholder() + ">", new HologramPlaceholder(placeholder, updateIntervalTicks));
    }

    public synchronized void update() {
        if (!shouldUpdate) return;
        shouldUpdate = false;

        // Ensure armorStands size matches lines size
        while (armorStands.size() < lines.size()) {
            ArmorStand armorStand = HologramManager.createArmorStand(this);
            armorStands.add(armorStand);
        }
        while (armorStands.size() > lines.size()) {
            ArmorStand armorStand = armorStands.remove(armorStands.size() - 1);
            armorStand.remove();
        }

        //Calculate the new position of each armor stand
        for (int i = 0; i < armorStands.size(); i++) {
            ArmorStand armorStand = armorStands.get(i);
            //Check if the armorstand was spawned or not
            if (!armorStand.isInWorld()) {
                //Set the new location before spawning
                armorStand.teleportAsync(location.clone().add(0, i * 0.25, 0));
//                if (Bukkit.isPrimaryThread()) {
//                    armorStand.getWorld().addEntity(armorStand);
//                } else {
//                    Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), () -> armorStand.getWorld().addEntity(armorStand));
//                }
                continue;
            }

            armorStand.teleportAsync(location.clone().add(0, i * 0.25, 0));
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            ArmorStand armorStand = armorStands.get(i);
            armorStand.customName(AdventureUtils.formatComponent(line, placeholders.values().stream()
                    .map(HologramPlaceholder::getPlaceholder)
                    .toList()));
        }
    }

    public synchronized void updatePlaceholders() {
        //Check if we need to update any placeholder or do we have any at all
        if (placeholders.isEmpty()) return;
        if (placeholders.values().stream().noneMatch(HologramPlaceholder::shouldUpdate)) return;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String updatedLine = line;
            boolean hasUpdate = false;

            for (Map.Entry<String, HologramPlaceholder> entry : placeholders.entrySet()) {
                String formatted = entry.getKey(); // e.g., "<player>"
                HologramPlaceholder hologramPlaceholder = entry.getValue();

                if (updatedLine.contains(formatted) && hologramPlaceholder.shouldUpdate()) {
                    updatedLine = updatedLine.replace(formatted, hologramPlaceholder.getValue());
                    hasUpdate = true;
                }
            }

            if (hasUpdate && i < armorStands.size()) {
                ArmorStand armorStand = armorStands.get(i);
                armorStand.customName(AdventureUtils.formatComponent(updatedLine));
            }
        }
    }


    public List<String> getLines() {
        return List.copyOf(lines);
    }

    public void addLine(String line) {
        lines.add(line);
        this.shouldUpdate = true;
    }

    public void removeLine(int index) {
        lines.remove(index);
        this.shouldUpdate = true;
    }

    public void setLine(int index, String line) {
        lines.set(index, line);
        this.shouldUpdate = true;
    }

    public boolean canSee(UUID uuid) {
        if (!useViewers) {
            return true; // If useViewers is false, everyone can see the hologram
        }
        return viewers.contains(uuid);
    }

    public boolean useViewers() {
        return useViewers;
    }

    public void remove() {
        if (!Bukkit.isPrimaryThread() && !Bukkit.isStopping()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), this::remove);
            return;
        }
        for (ArmorStand armorStand : armorStands) {
            armorStand.remove();
        }
        armorStands.clear();
    }

    void tickAsync() {
        update();
        updatePlaceholders();
    }
}
