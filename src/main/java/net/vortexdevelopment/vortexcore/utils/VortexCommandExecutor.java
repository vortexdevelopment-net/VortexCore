package net.vortexdevelopment.vortexcore.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface VortexCommandExecutor extends TabExecutor {


    default List<String> filterCompletions(String arg, String... completions) {
        return Stream.of(completions)
                .filter(completion -> completion.toLowerCase(Locale.ENGLISH).startsWith(arg.toLowerCase()))
                .toList();
    }

    default List<String> filterOnlinePlayers(String arg) {
        return Bukkit.getOnlinePlayers().stream().filter(player -> player.getName().toLowerCase(Locale.ENGLISH).startsWith(arg.toLowerCase()))
                .map(Player::getName)
                .toList();
    }
}
