package net.vortexdevelopment.vortexcore.utils;

import org.bukkit.Chunk;
import org.bukkit.Location;

public class WorldUtils {

    /**
     * Checks if a given location is within the specified chunk without triggering chunk loading.
     *
     * @param location The location to check.
     * @param chunk    The chunk to compare against.
     * @return True if the location is within the chunk, false otherwise.
     */
    public static boolean isLocationAtChunk(Location location, Chunk chunk) {
        return location.getWorld().equals(chunk.getWorld()) &&
               location.getBlockX() >> 4 == chunk.getX() &&
               location.getBlockZ() >> 4 == chunk.getZ();
    }

    /**
     * Checks if the chunk at the given location is currently loaded without triggering chunk loading.
     *
     * @param location The location to check.
     * @return True if the chunk is loaded, false otherwise.
     */
    public static boolean isChunkLoadedAtLocation(Location location) {
        return location.getWorld().isChunkLoaded(
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4
        );
    }
}
