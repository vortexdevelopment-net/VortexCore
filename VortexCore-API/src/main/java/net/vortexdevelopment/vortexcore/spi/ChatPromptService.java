package net.vortexdevelopment.vortexcore.spi;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Platform-specific chat prompt handling (Paper {@code AsyncChatEvent} vs Spigot {@code AsyncPlayerChatEvent}).
 */
public interface ChatPromptService {

    void promptPlayer(Player player, Consumer<String> consumer);
}
