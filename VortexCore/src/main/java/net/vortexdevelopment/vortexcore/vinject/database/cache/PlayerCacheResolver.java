package net.vortexdevelopment.vortexcore.vinject.database.cache;

import net.vortexdevelopment.vinject.annotation.Inject;
import net.vortexdevelopment.vinject.annotation.component.Component;
import net.vortexdevelopment.vinject.annotation.database.RegisterCacheContributor;
import net.vortexdevelopment.vinject.database.cache.Cache;
import net.vortexdevelopment.vinject.database.cache.CacheConfig;
import net.vortexdevelopment.vinject.database.cache.CacheContributor;
import net.vortexdevelopment.vinject.database.cache.CacheCoordinator;
import net.vortexdevelopment.vinject.database.cache.CacheProvider;
import net.vortexdevelopment.vinject.database.cache.CacheResolver;
import net.vortexdevelopment.vinject.database.repository.RepositoryInvocationContext;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

@RegisterListener
public class PlayerCacheResolver implements Listener {

    @Inject
    private CacheCoordinator cacheCoordinator;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();

        cacheCoordinator.provide(DefaultCacheKeys.PLAYER_UUID, playerUUID);
        cacheCoordinator.provide(DefaultCacheKeys.PLAYER_NAME, playerName);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();

        cacheCoordinator.invalidate(DefaultCacheKeys.PLAYER_UUID, playerUUID);
        cacheCoordinator.invalidate(DefaultCacheKeys.PLAYER_NAME, playerName);
    }
}
