package net.vortexdevelopment.vortexcore.spi;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Static entry for chat prompts; implementation is registered by the platform module (Paper or Spigot).
 */
public final class ChatPrompts {

    private static volatile ChatPromptService service;

    private ChatPrompts() {
    }

    public static void setService(ChatPromptService service) {
        if (isPaperService(ChatPrompts.service) && isSpigotService(service)) {
            return;
        }
        ChatPrompts.service = service;
    }

    private static boolean isPaperService(ChatPromptService service) {
        return service != null && service.getClass().getName().contains(".platform.paper.");
    }

    private static boolean isSpigotService(ChatPromptService service) {
        return service != null && service.getClass().getName().contains(".platform.spigot.");
    }

    public static void promptPlayer(Player player, Consumer<String> consumer) {
        ChatPromptService s = service;
        if (s == null) {
            throw new IllegalStateException("ChatPromptService not initialized (wrong VortexCore artifact or startup order)");
        }
        s.promptPlayer(player, consumer);
    }
}
