package net.vortexdevelopment.vortexcore.text.hologram;

import lombok.Getter;
import lombok.Setter;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class Hologram {

    @Getter private String id;
    @Getter private Location location;
    @Setter private List<String> lines = new ArrayList<>();
    @Getter private List<UUID> viewers = new LinkedList<>(); // List of players who can see the hologram
    @Setter private boolean useViewers = false; // If true, only players in the viewers list can see the hologram
    @Getter @Setter private boolean visible = true;
    @Getter @Setter private List<ArmorStand> armorStands = new ArrayList<>();

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

    public List<String> getLines() {
        return List.copyOf(lines);
    }

    public void addLine(String line) {
        lines.add(line);
    }

    public void removeLine(int index) {
        lines.remove(index);
    }

    public void setLine(int index, String line) {
        lines.set(index, line);
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
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), this::remove);
            return;
        }
        for (ArmorStand armorStand : armorStands) {
            armorStand.remove();
        }
        armorStands.clear();
    }
}
