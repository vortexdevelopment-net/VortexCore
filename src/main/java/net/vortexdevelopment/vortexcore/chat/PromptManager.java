package net.vortexdevelopment.vortexcore.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PromptManager implements Listener {

    private static final Map<UUID, Consumer<String>> promptPlayers = new ConcurrentHashMap<>();

    private static Method originalMessageMethod;

    static {
        try {
            originalMessageMethod = AsyncChatEvent.class.getMethod("originalMessage");
            originalMessageMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) throws Exception {
        if (promptPlayers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Consumer<String> consumer = promptPlayers.remove(event.getPlayer().getUniqueId());
            if (consumer != null) {
                Object response = originalMessageMethod.invoke(event);
                Component shaded = AdventureUtils.convertToShadedComponent(response);
                consumer.accept(AdventureUtils.toLegacy(shaded));
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
