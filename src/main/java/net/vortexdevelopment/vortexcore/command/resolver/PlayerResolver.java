package net.vortexdevelopment.vortexcore.command.resolver;

import net.vortexdevelopment.vortexcore.command.ParameterResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;

public class PlayerResolver implements ParameterResolver<Player> {
    @Override
    public Player resolve(String input) {
        return Bukkit.getPlayer(input);
    }

    @Override
    public boolean supports(Class<?> type) {
        return Player.class.isAssignableFrom(type);
    }
    
    @Override
    public Set<Class<?>> getSupportedTypes() {
        return Set.of(Player.class);
    }
}