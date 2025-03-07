package me.ceze88.vortexcore.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.ceze88.vortexcore.text.AdventureUtils;
import me.ceze88.vortexcore.utils.Pair;
import me.ceze88.vortexcore.vinject.annotation.RegisterListener;
import net.vortexdevelopment.vinject.annotation.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PromptManager implements Listener {

    private static final Map<UUID, Consumer<String>> promptPlayers = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        if (promptPlayers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Consumer<String> consumer = promptPlayers.remove(event.getPlayer().getUniqueId());
            if (consumer != null) {
                consumer.accept(AdventureUtils.toLegacy(event.message()));
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Consumer<String> consumer = promptPlayers.remove(event.getPlayer().getUniqueId());
        if (consumer != null) {
            consumer.accept(null);
        }
    }

    /**
     * Prompt a player for a response
     * @param player The player to prompt
     * @param consumer The consumer to handle the response. Response value can be null if the player leaves the server before responding
     */
    public static void promptPlayer(Player player, Consumer<String> consumer) {
        promptPlayers.put(player.getUniqueId(), consumer);
    }
}
