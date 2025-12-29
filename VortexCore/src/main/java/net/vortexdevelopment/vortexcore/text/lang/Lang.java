package net.vortexdevelopment.vortexcore.text.lang;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.vortexdevelopment.vinject.config.yaml.YamlConfig;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.gui.Gui;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.text.MiniMessagePlaceholder;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@net.vortexdevelopment.vinject.annotation.Component(priority = 9) //Ensure we load it before anything else
@RegisterReloadHook
public class Lang implements ReloadHook {

    private static boolean initialized = false;
    private static YamlConfig lang;
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
        List<MiniMessagePlaceholder> placeholderList = new ArrayList<>(Stream.of(placeholders).toList());
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
            command = processRandomPlaceholders(command);
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

            // Check for sound - [SOUND] SOUND_NAME [VOLUME] [PITCH]
            if (command.startsWith("[SOUND]")) {
                command = command.substring(7).trim();
                if (player != null) {
                    try {
                        String[] parts = command.split(" ");
                        String soundName = parts[0];
                        String namespace = soundName.split(":").length > 1 ? soundName.split(":")[0] : "minecraft";
                        String key = soundName.split(":").length > 1 ? soundName.split(":")[1] : soundName;
                        Sound sound = (Sound) Sound.class.getDeclaredField(key).get(null);
                        if (sound == null) {
                            VortexPlugin.getInstance().getLogger().warning("Sound not found: " + soundName);
                            continue;
                        }
                        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    } catch (Exception e) {
                        VortexPlugin.getInstance().getLogger().warning("Failed to play sound: " + command);
                        e.printStackTrace();
                    }
                }
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

    private static String processRandomPlaceholders(String command) {
        // [RANDOM_NUMBER:from:to]
        Pattern randomNumberPattern = Pattern.compile("\\[RANDOM_NUMBER:(-?\\d+):(-?\\d+)\\]");
        Matcher randomNumberMatcher = randomNumberPattern.matcher(command);
        StringBuilder sb = new StringBuilder();
        while (randomNumberMatcher.find()) {
            try {
                long from = Long.parseLong(randomNumberMatcher.group(1));
                long to = Long.parseLong(randomNumberMatcher.group(2));
                long min = Math.min(from, to);
                long max = Math.max(from, to);
                long randomValue = ThreadLocalRandom.current().nextLong(min, max + 1);
                randomNumberMatcher.appendReplacement(sb, String.valueOf(randomValue));
            } catch (Exception e) {
                randomNumberMatcher.appendReplacement(sb, randomNumberMatcher.group(0));
            }
        }
        randomNumberMatcher.appendTail(sb);
        command = sb.toString();

        // [RANDOM:element1:element2:...]
        Pattern randomPattern = Pattern.compile("\\[RANDOM:([^\\]]+)\\]");
        Matcher randomMatcher = randomPattern.matcher(command);
        sb = new StringBuilder();
        while (randomMatcher.find()) {
            String[] elements = randomMatcher.group(1).split(":");
            if (elements.length > 0) {
                String randomElement = elements[ThreadLocalRandom.current().nextInt(elements.length)];
                randomMatcher.appendReplacement(sb, Matcher.quoteReplacement(randomElement));
            } else {
                randomMatcher.appendReplacement(sb, "");
            }
        }
        randomMatcher.appendTail(sb);
        return sb.toString();
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
                // Check if it exists in the jar
                if (VortexPlugin.class.getResource("/lang.yml") == null) {
                    return;
                }
                VortexPlugin.getInstance().saveResource("lang.yml", false);
            }
            lang = YamlConfig.load(langFile);

            // Add missing keys from bundled resource
            InputStream defConfigStream = VortexPlugin.getInstance().getResource("lang.yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                boolean changed = false;
                for (String key : defConfig.getKeys(true)) {
                    if (!lang.contains(key) || (lang.get(key) == null) || lang.getString(key).isEmpty()) {
                        lang.set(key, defConfig.get(key));
                        changed = true;
                    }
                }
                if (changed) {
                    lang.save();
                }
            }

            staticPlaceholders.add(new MiniMessagePlaceholder("prefix", lang.getString("General.Plugin Prefix", VortexPlugin.getInstance().getPrefixString())));
            if (lang.getConfigurationSection("Colors") != null) {
                for (String placeholder : lang.getConfigurationSection("Colors").getKeys(false)) {
                    staticPlaceholders.add(new MiniMessagePlaceholder(placeholder, "<color:" + lang.getString("Colors." + placeholder) + ">"));
                }
            }

            // Custom Placeholders
            if (lang.getConfigurationSection("Custom Placeholders") != null) {
                for (String placeholder : lang.getConfigurationSection("Custom Placeholders").getKeys(false)) {
                    staticPlaceholders.add(new MiniMessagePlaceholder(placeholder, lang.getString("Custom Placeholders." + placeholder)));
                }
            }
            initialized = true;
            Gui.BACK_BUTTON_NAME = getString("GUI.Back Button Name", "§cBack");
            AdventureUtils.reloadMiniMessage();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
