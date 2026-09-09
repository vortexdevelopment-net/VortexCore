package net.vortexdevelopment.vortexcore.spi;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Paper chat prompt handling.
 */
public interface ChatPromptService {

    void promptPlayer(Player player, Consumer<String> consumer);
}
