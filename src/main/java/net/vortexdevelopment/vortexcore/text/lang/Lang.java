package net.vortexdevelopment.vortexcore.text.lang;

import net.kyori.adventure.text.Component;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.gui.Gui;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.text.MiniMessagePlaceholder;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@net.vortexdevelopment.vinject.annotation.Component(priority = 9) //Ensure we load it before anything else
@RegisterReloadHook
public class Lang implements ReloadHook {

    private static boolean initialized = false;
    private static FileConfiguration lang;
    public static List<MiniMessagePlaceholder> staticPlaceholders = new ArrayList<>();

    private Lang() {
        onReload();
    }

    /**
     * Adds static placeholders to the list of placeholders.
     * @param placeholders The placeholders to expand.
     * @return A list of placeholders including the static ones.
     */
    public static List<MiniMessagePlaceholder> createPlaceholders(MiniMessagePlaceholder... placeholders) {
        List<MiniMessagePlaceholder> placeholderList = new java.util.ArrayList<>(Stream.of(placeholders).toList());
        placeholderList.addAll(staticPlaceholders);
        return placeholderList;
    }

    public static String replaceStaticPlaceholders(String message) {
        for (MiniMessagePlaceholder placeholder : staticPlaceholders) {
            message = message.replace("<" + placeholder.getPlaceholder() + ">", placeholder.getValue());
        }
        return message;
    }

    public static List<String> replaceStaticPlaceholders(@NotNull List<String> list) {
        List<String> result = new ArrayList<>();
        for (String line : list) {
            for (MiniMessagePlaceholder placeholder : staticPlaceholders) {
                line = line.replace("<" + placeholder.getPlaceholder() + ">", placeholder.getValue());
            }
            result.add(line);
        }
        return result;
    }

    public static String getString(String key) {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return key;
        }
        return replaceStaticPlaceholders(lang.getString(key, key));
    }

    public static String getString(String key, String defaultValue) {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return defaultValue;
        }
        return replaceStaticPlaceholders(lang.getString(key, defaultValue));
    }

    public static Component getComponent(String key) {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return AdventureUtils.formatComponent(key);
        }
        return AdventureUtils.formatComponent(lang.getString(key, key), staticPlaceholders);
    }

    public static Component getComponent(String key, MiniMessagePlaceholder... placeholders) {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return AdventureUtils.formatComponent(key);
        }
        return AdventureUtils.formatComponent(lang.getString(key, key), createPlaceholders(placeholders));
    }

    public static List<String> getList(String key) {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return List.of(key);
        }
        return lang.getStringList(key);
    }

    public static void send(CommandSender player, String key) {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return;
        }
        AdventureUtils.sendMessage(getComponent(key), player);
    }

    public static void sendPrefixed(CommandSender player, String key) {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return;
        }
        AdventureUtils.sendMessage(getPrefix().append(getComponent(key)), player);
    }

    public static void send(CommandSender player, String key, MiniMessagePlaceholder... placeholders) {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return;
        }
        AdventureUtils.sendMessage(getComponent(key, placeholders), player);
    }

    private static String getPrefixString() {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return VortexPlugin.getInstance().getPrefixString();
        }
        return lang.getString("General.Plugin Prefix", VortexPlugin.getInstance().getPrefixString());
    }

    public static Component getPrefix() {
        if (!initialized) {
            VortexPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
            return VortexPlugin.getInstance().getPrefixWithDash();
        }
        return getComponent("General.Plugin Prefix");
    }

    public static void runConsoleCommands(@Nullable Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        //Check if we on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(VortexPlugin.getInstance(), () -> runConsoleCommands(player, commands));
            return;
        }

        for (String command : commands) {
            //Check if command stats with [MESSAGE], if so send the message to the player
            if (command.startsWith("[MESSAGE]")) {
                command = command.substring(9).trim();
                AdventureUtils.sendMessage(AdventureUtils.formatComponent(command, createOptionalPlaceholder(player != null, "player", player.getName())), player);
                continue;
            }

            //Check if command stats with [MESSAGE:<key>], if so send the message to the player from the lang file
            if (command.startsWith("[MESSAGE:")) {
                String key = command.substring(9, command.indexOf("]"));
                command = command.substring(command.indexOf("]") + 1).trim();
                AdventureUtils.sendMessage(getComponent(key, createOptionalPlaceholder(player != null, "player", player.getName())), player);
                continue;
            }

            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (player != null) {
                command = command.replace("<player>", player.getName());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.trim());
        }
    }

    public static MiniMessagePlaceholder[] createOptionalPlaceholder(boolean condition, String placeholder, String value) {
        if (condition) {
            return new MiniMessagePlaceholder[]{new MiniMessagePlaceholder(placeholder, value)};
        }
        return new MiniMessagePlaceholder[0];
    }

    @Override
    public void onReload() {
        try {
            staticPlaceholders.clear();
            File langFile = new File(VortexPlugin.getInstance().getDataFolder(), "lang.yml");
            if (!langFile.exists()) {
                VortexPlugin.getInstance().saveResource("lang.yml", false);
            }
            lang = YamlConfiguration.loadConfiguration(langFile);

            staticPlaceholders.add(new MiniMessagePlaceholder("prefix", lang.getString("General.Plugin Prefix", VortexPlugin.getInstance().getPrefixString())));
            if (lang.getConfigurationSection("Colors") != null) {
                for (String placeholder : lang.getConfigurationSection("Colors").getKeys(false)) {
                    staticPlaceholders.add(new MiniMessagePlaceholder(placeholder, "<color:" + lang.getString("Colors." + placeholder) + ">"));
                }
            }
            initialized = true;
            Gui.BACK_BUTTON_NAME = getString("GUI.Back Button Name", "§cBack");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
