package net.vortexdevelopment.vortexcore.platform.paper;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import net.kyori.adventure.text.Component;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.spi.ChatPromptService;
import net.vortexdevelopment.vortexcore.spi.ChatPrompts;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@net.vortexdevelopment.vinject.annotation.component.Component
@RegisterListener(registerWhenClassPresent = "io.papermc.paper.event.player.AsyncChatEvent")
public class PaperPromptManager implements Listener, ChatPromptService {

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

    @PostConstruct
    public void registerChatPromptFacade() {
        ChatPrompts.setService(this);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) throws Exception {
        if (promptPlayers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Consumer<String> consumer = promptPlayers.remove(event.getPlayer().getUniqueId());
            if (consumer != null) {
                Object response = originalMessageMethod.invoke(event);
                Component shaded = AdventureUtils.convertToShadedComponent(response);
                Bukkit.getServer().getScheduler().callSyncMethod(VortexPlugin.getInstance(), () -> {
                    consumer.accept(PaperBukkitAdventureBridge.LEGACY_SECTION.serialize(shaded));
                    return CompletableFuture.runAsync(() -> {}); //Return empty future, can't return null
                });
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
    @Override
    public void promptPlayer(Player player, Consumer<String> consumer) {
        promptPlayers.put(player.getUniqueId(), consumer);
    }
}
