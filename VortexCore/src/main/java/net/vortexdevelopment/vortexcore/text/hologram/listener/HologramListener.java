package net.vortexdevelopment.vortexcore.text.hologram.listener;

import net.vortexdevelopment.vortexcore.text.hologram.HologramManager;
import net.vortexdevelopment.vortexcore.utils.PerformanceLogger;
import org.bukkit.Chunk;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterListener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RegisterListener
public class HologramListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ArmorStand armorStand) {

                // First, clear any out of session holograms in this chunk
                String sessionId = armorStand.getPersistentDataContainer().get(HologramManager.getSessionIdKey(), PersistentDataType.STRING);
                if (sessionId != null && sessionId.equals(HologramManager.getSessionId())) {
                    armorStand.remove();
                }
            }
        }

        // Now load holograms for this chunk
        HologramManager.loadHologramsInChunk(chunk);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        HologramManager.unloadHologramsInChunk(event.getChunk());
    }
}
