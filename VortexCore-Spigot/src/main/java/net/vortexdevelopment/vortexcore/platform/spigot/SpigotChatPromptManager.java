package net.vortexdevelopment.vortexcore.platform.spigot;

import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.spi.ChatPromptService;
import net.vortexdevelopment.vortexcore.spi.ChatPrompts;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@net.vortexdevelopment.vinject.annotation.component.Component
@RegisterListener
public class SpigotChatPromptManager implements Listener, ChatPromptService {

    private final Map<UUID, Consumer<String>> promptPlayers = new ConcurrentHashMap<>();

    @PostConstruct
    public void registerChatPromptFacade() {
        ChatPrompts.setService(this);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!promptPlayers.containsKey(id)) {
            return;
        }
        event.setCancelled(true);
        Consumer<String> consumer = promptPlayers.remove(id);
        if (consumer != null) {
            String message = event.getMessage();
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), () -> consumer.accept(message));
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Consumer<String> consumer = promptPlayers.remove(event.getPlayer().getUniqueId());
        if (consumer != null) {
            consumer.accept(null);
        }
    }

    @Override
    public void promptPlayer(Player player, Consumer<String> consumer) {
        promptPlayers.put(player.getUniqueId(), consumer);
    }
}
