package net.vortexdevelopment.vortexcore.text.lang;

import net.kyori.adventure.text.Component;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.gui.Gui;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.text.MiniMessagePlaceholder;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@net.vortexdevelopment.vinject.annotation.Component
@RegisterReloadHook
public class Lang implements ReloadHook {

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
        return replaceStaticPlaceholders(lang.getString(key, key));
    }

    public static String getString(String key, String defaultValue) {
        return replaceStaticPlaceholders(lang.getString(key, defaultValue));
    }

    public static Component getComponent(String key) {
        return AdventureUtils.formatComponent(lang.getString(key, key), staticPlaceholders);
    }

    public static Component getComponent(String key, MiniMessagePlaceholder... placeholders) {
        return AdventureUtils.formatComponent(lang.getString(key, key), createPlaceholders(placeholders));
    }

    public static List<String> getList(String key) {
        return lang.getStringList(key);
    }

    public static void send(CommandSender player, String key) {
        AdventureUtils.sendMessage(getComponent(key), player);
    }

    public static void sendPrefixed(CommandSender player, String key) {
        AdventureUtils.sendMessage(getComponent(key), player);
    }

    public static void send(CommandSender player, String key, MiniMessagePlaceholder... placeholders) {
        AdventureUtils.sendMessage(getComponent(key, placeholders), player);
    }

    private static String getPrefixString() {
        return lang.getString("General.Plugin Prefix", VortexPlugin.getInstance().getPrefixString());
    }

    public static Component getPrefix() {
        return getComponent("General.Plugin Prefix");
    }

    @Override
    public void onReload() {
        staticPlaceholders.clear();
        File langFile = new File(VortexPlugin.getInstance().getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            VortexPlugin.getInstance().saveResource("lang.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
        Gui.BACK_BUTTON_NAME = getString("GUI.Back Button Name", "§cBack");

        staticPlaceholders.add(new MiniMessagePlaceholder("prefix", lang.getString("General.Plugin Prefix", VortexPlugin.getInstance().getPrefixString())));
        if (lang.getConfigurationSection("Colors") != null) {
            for (String placeholder : lang.createSection("Colors").getKeys(false)) {
                staticPlaceholders.add(new MiniMessagePlaceholder(placeholder, "<color:" + lang.getString("Colors." + placeholder) + ">"));
            }
        }
    }
}
