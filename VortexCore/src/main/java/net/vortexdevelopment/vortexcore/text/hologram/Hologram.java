package net.vortexdevelopment.vortexcore.text.hologram;

import it.unimi.dsi.fastutil.Hash;
import lombok.Getter;
import lombok.Setter;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.text.MiniMessagePlaceholder;
import net.vortexdevelopment.vortexcore.text.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

public class Hologram {

    @Getter private String id;
    @Getter private Location location;
    private List<String> lines = new ArrayList<>();
    @Getter private List<UUID> viewers = new LinkedList<>(); // List of players who can see the hologram
    @Setter private boolean useViewers = false; // If true, only players in the viewers list can see the hologram
    @Getter @Setter private boolean visible = true;
    @Getter @Setter private List<ArmorStand> armorStands = new ArrayList<>();
    private Map<String, HologramPlaceholder> placeholders = new HashMap<>();
    private boolean shouldUpdate = true; //If true, the hologram lines are changed and need to re-render them
    private int previousSize = 0; // Track previous size to detect size changes

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

    public List<MiniMessagePlaceholder> getPlaceholders() {
        return placeholders.values().stream().map(HologramPlaceholder::getPlaceholder).toList();
    }

    public synchronized void update() {
        update(false);
    }

     /**
     * Updates the hologram's armor stands to match the current lines.
     * This method ensures that each armor stand corresponds to the correct line,
     * adjusting positions and names as necessary.
     */
    public synchronized void update(boolean force) {
        if ((!shouldUpdate && !force) || !getLocation().isChunkLoaded()) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), () -> update(force));
            return;
        }
        shouldUpdate = false;

        // Ensure armorStands size matches lines size
        // Remove excess armor stands from the end
        while (armorStands.size() > lines.size()) {
            ArmorStand armorStand = armorStands.remove(armorStands.size() - 1);
            armorStand.remove();
        }

        // Add missing armor stands if there are fewer than lines
        while (armorStands.size() < lines.size()) {
            double yOffset = (lines.size() - 1 - armorStands.size()) * 0.25;
            Location correctLocation = location.clone().add(0, yOffset, 0);
            ArmorStand armorStand = HologramManager.createArmorStand(this, correctLocation);
            armorStands.addLast(armorStand);
        }
        
        // Now ensure each armor stand at index i corresponds to line i
        // This guarantees proper order regardless of previous state
        for (int i = 0; i < lines.size(); i++) {
            ArmorStand armorStand = armorStands.get(i);
            String line = lines.get(i);
            
            // Calculate correct position for this index (first line at top, subsequent lines below)
            // Index 0 (first line) should be at the top, so we reverse the Y offset
            // For N lines: line 0 is at (N-1)*0.25, line 1 is at (N-2)*0.25, etc.
            double yOffset = (lines.size() - 1 - i) * 0.25;
            Location correctLocation = location.clone().add(0, yOffset, 0);

            //Check if the armorstand was spawned or not
            if (!armorStand.isInWorld()) {
                // Entity is already at correct location, just add to world
                armorStand.getWorld().addEntity(armorStand);
            } else {
                // For existing stands, teleport to ensure correct position
                armorStand.teleportAsync(correctLocation);
            }
            
            // Update the custom name for this line - ensure it matches the line at this index
            updateArmorStandName(armorStand, line);
            armorStand.setCustomNameVisible(true);
        }
        
        // Update the tracked size after rendering
        previousSize = armorStands.size();
    }

    /**
     * Checks if the size of armor stands has changed compared to the number of lines.
     * When size changes, all lines need to be re-rendered because positions may have shifted.
     */
    private boolean hasSizeChanged() {
        return armorStands.size() != lines.size() || previousSize != lines.size();
    }

    /**
     * Updates armor stand name with placeholder replacement.
     * This method processes all placeholders in a single pass for efficiency.
     */
    private void updateArmorStandName(ArmorStand armorStand, String line) {
        if (placeholders.isEmpty()) {
            // No placeholders, just format the line
            armorStand.customName(AdventureUtils.formatComponent(line, Lang.staticPlaceholders));
            return;
        }

        List<MiniMessagePlaceholder> placeholderList = new ArrayList<>(Lang.staticPlaceholders);
        for (HologramPlaceholder hologramPlaceholder : placeholders.values()) {
            placeholderList.add(hologramPlaceholder.getPlaceholder());
        }

        armorStand.customName(AdventureUtils.formatComponent(line, placeholderList));
    }

    public synchronized void updatePlaceholders() {
        updatePlaceholders(false);
    }

     /**
     * Updates only the armor stands that have placeholders needing updates.
     * This method checks each line for placeholders and updates only those lines.
     */
    public synchronized void updatePlaceholders(boolean force) {
        //Check if we need to update any placeholder or do we have any at all
        if (placeholders.isEmpty() || !getLocation().isChunkLoaded()) return;
        if (placeholders.values().stream().noneMatch(HologramPlaceholder::shouldUpdate) && !force) return;

        // Only update lines that contain placeholders that need updating
        for (int i = 0; i < lines.size() && i < armorStands.size(); i++) {
            String line = lines.get(i);
            boolean needsUpdate = false;

            // Check if this line has any placeholders that need updating
            for (Map.Entry<String, HologramPlaceholder> entry : placeholders.entrySet()) {
                String placeholderKey = entry.getKey();
                HologramPlaceholder hologramPlaceholder = entry.getValue();
                
                if (line.contains(placeholderKey)) {
                    needsUpdate = true;
                    break;
                }
            }

            if (needsUpdate) {
                ArmorStand armorStand = armorStands.get(i);
                updateArmorStandName(armorStand, line);
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

    public void setLines(List<String> lines) {
        this.lines = lines;
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
