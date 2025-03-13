package net.vortexdevelopment.vortexcore.hooks.holo;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import net.vortexdevelopment.vortexcore.VortexCore;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.hooks.PluginHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HologramHook implements PluginHook {

    private boolean enabled = false;
    private DecentHolograms hologramsAPI;
    private static final String HOLOGRAM_ID_PREFIX = VortexPlugin.getInstance().getName() + "_";

    public HologramHook() {
        if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            hologramsAPI = DecentHologramsAPI.get();
            enabled = true;
        }
    }

    @Override
    public String getIdentifier() {
        return "Hologram";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        if (!enabled) return;
        VortexCore.getPlugin().getLogger().info("Hooked into DecentHolograms!");
    }

    @Override
    public void onDisable() {
        if (!enabled) return;
        hologramsAPI.getHologramManager().getHolograms().stream().map(Hologram::getId).filter(s -> s.startsWith(HOLOGRAM_ID_PREFIX)).forEach(s -> {
            hologramsAPI.getHologramManager().getHologram(s).delete();
        });
    }

    private String createHologramId(String id) {
        return HOLOGRAM_ID_PREFIX + id;
    }

    public @Nullable Hologram createHologram(String id, Location location, List<String> lines) {
        if (!enabled) return null;
        return DHAPI.createHologram(createHologramId(id), location, lines);
    }

    public void updateHologram(String id, List<String> lines) {
        if (!enabled) return;
        Hologram hologram = hologramsAPI.getHologramManager().getHologram(createHologramId(id));
        if (hologram == null) return;
        HologramPage page = hologram.getPage(0);
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = page.getLine(i);
            if (line == null) {
                page.addLine(new HologramLine(null, page.getNextLineLocation(), lines.get(i)));
            } else {
                line.setText(lines.get(i));
            }
        }
    }

    public void updateLine(String id, int line, String text) {
        if (!enabled) return;
        Hologram hologram = hologramsAPI.getHologramManager().getHologram(createHologramId(id));
        if (hologram == null) return;
        HologramPage page = hologram.getPage(0);
        HologramLine hologramLine = page.getLine(line);
        if (hologramLine == null) {
            page.addLine(new HologramLine(null, page.getNextLineLocation(), text));
        } else {
            hologramLine.setText(text);
        }
    }

    public void createOrUpdateHologram(String id, Location location, List<String> lines) {
        if (!enabled) return;
        Hologram hologram = hologramsAPI.getHologramManager().getHologram(createHologramId(id));
        if (hologram == null) {
            createHologram(id, location, lines);
        } else {
            updateHologram(id, lines);
        }
    }

    public void deleteHologram(String id) {
        if (!enabled) return;
        hologramsAPI.getHologramManager().getHologram(createHologramId(id)).delete();
    }
}
