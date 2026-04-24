package net.vortexdevelopment.vortexcore.text;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.spi.AdventurePlatforms;
import net.vortexdevelopment.vortexcore.spi.BukkitAdventureBridge;
import net.vortexdevelopment.vortexcore.spi.BukkitAdventureBridges;
import net.vortexdevelopment.vortexcore.text.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AdventureUtils {

    public static Class<?> getComponentClass() {
        return AdventurePlatforms.get().getServerComponentClass();
    }

    private static final TagResolver languageResolver = TagResolver.resolver(
            "language", (args, context) -> {
                String yamlPath = args.popOr("missing-key").value();
                Component component = MiniMessage.miniMessage().deserialize(replaceLegacy(Lang.getString(yamlPath, yamlPath)));
                return Tag.selfClosingInserting(component);
            }
    );


    private static final TagResolver playerCommandResolver = TagResolver.resolver(
            "playercommand", (args, context) -> {
                try {
                    String command = args.pop().value();
                    // Does not work in 1.18.2 - target method does not exist
                    Pointered pointered = context.target();
                    if (pointered instanceof Player player) {
                        //Run command on player
                        player.performCommand(command);
                    }
                } catch (ParsingException ignored) {}
                return Tag.selfClosingInserting(Component.empty());
            }
    );

    private static final TagResolver consoleCommandResolver = TagResolver.resolver(
            "consolecommand", (args, context) -> {
                try {
                    String command = args.pop().value();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } catch (ParsingException ignored) {}
                return Tag.selfClosingInserting(Component.empty());
            }
    );

    private static MiniMessage miniMessage;

    public static void reloadMiniMessage() {
        TagResolver.Builder builder = TagResolver.builder()
                .resolver(StandardTags.defaults())
                .resolver(languageResolver)
                .resolver(playerCommandResolver)
                .resolver(consoleCommandResolver);

        for (MiniMessagePlaceholder placeholder : Lang.staticPlaceholders) {
            // No need to check if the value is a component, these are all strings for static placeholders
            builder.resolver(Placeholder.parsed(placeholder.getPlaceholder(), replaceLegacy(placeholder.getValue().toString())));
        }

        TagResolver baseResolver = builder.build();
        miniMessage = MiniMessage.builder().tags(baseResolver).build();
    }

    public static MiniMessage getMiniMessage() {
        if (miniMessage == null) {
            reloadMiniMessage();
        }
        return miniMessage;
    }

    public static Component convertToShadedComponent(Object original) {
        return AdventurePlatforms.get().convertToShadedComponent(original);
    }

    /**
     * Convert a shaded component to a json string
     *
     * @param component The shaded Component to convert
     *
     * @return Json string
     */
    public static String convertToJson(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    /**
     * Convert a json string to the non-shaded component
     * Cast it to the correct type
     *
     * @param json Json string
     *
     * @return Non-shaded component
     */
    public static Object convertToOriginalComponent(String json) {
        return AdventurePlatforms.get().convertToOriginalComponent(json);
    }

    /**
     * Convert the shaded Component to the original one
     * Cast it to the correct type
     *
     * @param component Shaded component
     *
     * @return Original component
     */
    public static Object convertToOriginalComponent(Component component) {
        return AdventurePlatforms.get().convertToOriginalComponent(component);
    }

    /**
     * Convert a list of shaded components to a list of original components
     * Cast it to the correct type
     *
     * @param components List of shaded components
     *
     * @return List of original components
     */
    public static Object convertToOriginalComponent(List<Component> components) {
        try {
            LinkedList<Object> list = new LinkedList<>();
            for (Component component : components) {
                list.add(convertToOriginalComponent(component));
            }
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Convert a list of shaded components to a list of original components
     * Cast it to the correct type
     *
     * @param components List of shaded components
     *
     * @return List of original components
     */
    public static Object convertToOriginalComponent(Component... components) {
        try {
            LinkedList<Object> list = new LinkedList<>();
            for (Component component : components) {
                list.add(convertToOriginalComponent(component));
            }
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static final LegacyComponentSerializer SEND_FALLBACK_LEGACY = LegacyComponentSerializer.legacySection();

    /**
     * Sends a {@link Component} to targets. If {@link BukkitAdventureBridge} is not installed yet (e.g. messages
     * during {@code onLoad} or before Vinject finishes in {@code onEnable}), falls back to legacy section strings
     * on {@link CommandSender#sendMessage(String)} — same as the Spigot bridge implementation.
     */
    private static void deliverComponentMessage(Component message, CommandSender... targets) {
        BukkitAdventureBridge bridge = BukkitAdventureBridges.getOrNull();
        if (bridge != null) {
            for (CommandSender sender : targets) {
                bridge.sendComponentMessage(sender, message);
            }
        } else {
            String legacy = SEND_FALLBACK_LEGACY.serialize(message);
            for (CommandSender sender : targets) {
                sender.sendMessage(legacy);
            }
        }
    }

    public static void sendMessage(Component message, CommandSender... target) {
        deliverComponentMessage(message, target);
    }

    public static void sendMessage(String message, CommandSender... target) {
        deliverComponentMessage(formatComponent(message), target);
    }

    //
    // Item name formatting
    //

    public static void formatItemName(ItemMeta meta, String name) {
        setItemName(meta, formatComponent(name));
    }

    public static void formatItemName(ItemMeta meta, String name, MiniMessagePlaceholder... placeholders) {
        setItemName(meta, formatComponent(name, placeholders));
    }

    public static void formatItemName(ItemMeta meta, String name, List<MiniMessagePlaceholder> placeholders) {
        setItemName(meta, formatComponent(name, placeholders));
    }

    public static void formatItemName(ItemStack item, String name) {
        formatItemName(item, formatComponent(name));
    }

    public static void formatItemName(ItemStack item, String name, MiniMessagePlaceholder... placeholders) {
        formatItemName(item, formatComponent(name, placeholders));
    }

    public static void formatItemName(ItemStack item, String name, List<MiniMessagePlaceholder>placeholders) {
        formatItemName(item, formatComponent(name,placeholders));
    }

    //
    // Item lore formatting
    //

    public static void formatItemLore(ItemMeta meta, List<String> lore) {
        setItemLore(meta, formatComponent(lore).toArray(new Component[0]));
    }

    public static void formatItemLore(ItemMeta meta, List<String> lore, MiniMessagePlaceholder... placeholders) {
        setItemLore(meta, formatComponent(lore, placeholders).toArray(new Component[0]));
    }

    public static void formatItemLore(ItemMeta meta, List<String> lore, List<MiniMessagePlaceholder> placeholders) {
        setItemLore(meta, formatComponent(lore, placeholders).toArray(new Component[0]));
    }

    public static void formatItemLore(ItemMeta meta, List<Component> lore, String... unused) {
        setItemLore(meta, lore.toArray(new Component[0]));
    }

    public static void formatItemLore(ItemStack item, List<String> lore) {
        formatItemLore(item, lore.toArray(new String[0]));
    }

    public static void formatItemLore(ItemStack item, List<String> lore, MiniMessagePlaceholder... placeholders) {
        formatItemLore(item, lore.toArray(new String[0]), placeholders);
    }

    public static void formatItemLore(ItemStack item, List<String> lore, List<MiniMessagePlaceholder>placeholders) {
        formatItemLore(item, lore.toArray(new String[0]), placeholders.toArray(new MiniMessagePlaceholder[0]));
    }

    public static void formatItemLore(ItemStack item, String... lore) {
        formatItemLore(item, formatComponent(lore));
    }

    public static void formatItemLore(ItemStack item, List<Component> lore, String... unused) {
        formatItemLore(item, lore.toArray(new Component[0]));
    }

    public static void formatItemLore(ItemStack item, String[] lore, MiniMessagePlaceholder... placeholders) {
        LinkedList<Component> components = new LinkedList<>();
        for (String line : lore) {
            components.add(formatComponent(line, placeholders).decorate(TextDecoration.ITALIC.withState(TextDecoration.State.FALSE).decoration()));
        }
        formatItemLore(item, components.toArray(new Component[0]));
    }

    public static void formatItemName(ItemStack item, Component name) {
        setItemName(item, name);
    }

    public static void formatItemLore(ItemStack item, Component... lore) {
        setItemLore(item, lore);
    }

    public static void appendItemLore(ItemStack item, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !BukkitAdventureBridges.get().hasItemLore(meta)) {
            return;
        }
        List<Component> currentLore = new ArrayList<>(BukkitAdventureBridges.get().getItemLore(meta));
        currentLore.addAll(lore);
        setItemLore(item, currentLore.toArray(new Component[0]));
    }

    private static void setItemName(ItemStack item, Component name) {
        BukkitAdventureBridges.get().applyItemName(item, name);
    }

    private static void setItemName(ItemMeta meta, Component name) {
        if (name == null || meta == null) {
            return;
        }
        BukkitAdventureBridges.get().applyItemName(meta, name);
    }

    private static void setItemLore(ItemMeta meta, Component... lore) {
        if (lore == null || lore.length == 0) {
            return;
        }
        List<Component> formatted = new ArrayList<>();
        for (Component line : lore) {
            formatted.add(line.style(builder -> builder.decoration(TextDecoration.ITALIC, false)));
        }
        BukkitAdventureBridges.get().applyItemLore(meta, formatted);
    }

    private static void setItemLore(ItemStack item, Component... lore) {
        if (lore == null || lore.length == 0) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<Component> formatted = new ArrayList<>();
        for (Component line : lore) {
            formatted.add(line.style(builder -> builder.decoration(TextDecoration.ITALIC, false)));
        }
        BukkitAdventureBridges.get().applyItemLore(item, formatted);
    }

    // Formatting stuff
    public static Component formatComponentWithPrefix(String text) {
        return VortexPlugin.getInstance().getPrefix().append(formatComponent(text));
    }

    public static Component formatComponentWithPrefixWithDash(String text) {
        return VortexPlugin.getInstance().getPrefixWithDash().append(formatComponent(text));
    }

    public static Component formatComponent(String text) {
        return getMiniMessage().deserialize(replaceLegacy(text));
    }

    public static Component formatComponent(String text, MiniMessagePlaceholder... placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            return formatComponent(text);
        }

        TagResolver.Builder localBuilder = TagResolver.builder();
        for (MiniMessagePlaceholder placeholder : placeholders) {
            String key = placeholder.getPlaceholder();
            if (placeholder.isComponent()) {
                localBuilder.resolver(Placeholder.component(key, (Component) placeholder.getValue()));
            } else {
                localBuilder.resolver(Placeholder.parsed(key, replaceLegacy(placeholder.getValue().toString())));
            }
        }

        return getMiniMessage().deserialize(replaceLegacy(text), localBuilder.build());
    }

    public static Component formatComponent(String text, List<MiniMessagePlaceholder> placeholders) {
        return formatComponent(text, placeholders.toArray(new MiniMessagePlaceholder[0]));
    }

    public static List<Component> formatComponent(List<String> list) {
        List<Component> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatComponent(line));
        }
        return result;
    }

    public static List<Component> formatComponent(String... list) {
        List<Component> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatComponent(line));
        }
        return result;
    }

    public static List<Component> formatComponent(List<String> list, MiniMessagePlaceholder... placeholders) {
        List<Component> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatComponent(line, placeholders));
        }
        return result;
    }

    public static List<Component> formatComponent(List<String> list, List<MiniMessagePlaceholder> placeholders) {
        return formatComponent(list, placeholders.toArray(new MiniMessagePlaceholder[0]));
    }

    public static String formatLegacy(String text) {
        return ChatColor.translateAlternateColorCodes('&',
                LegacyComponentSerializer.legacyAmpersand().serialize(getMiniMessage().deserialize(replaceLegacy(text))));
    }

    public static List<String> formatLegacy(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatLegacy(line));
        }
        return result;
    }

    public static List<String> formatLegacy(String... list) {
        List<String> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatLegacy(line));
        }
        return result;
    }

    public static String toMiniMessage(Component component) {
        String s = getMiniMessage().serialize(component);
        // Strip negation tags MiniMessage emits; closing </!x> was missing and broke YAML round-trips
        s = s.replaceAll("<!(italic|i|underlined|u|strikethrough|st|bold|b|obfuscated|obf)>", "");
        s = s.replaceAll("</!(italic|i|underlined|u|strikethrough|st|bold|b|obfuscated|obf)>", "");
        return s;
    }

    public static List<String> toMiniMessage(List<Component> components) {
        List<String> list = new ArrayList<>();
        for (Component component : components) {
            list.add(toMiniMessage(component));
        }
        return list;
    }

    public static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static List<String> toLegacy(List<Component> components) {
        List<String> list = new ArrayList<>();
        for (Component component : components) {
            list.add(toLegacy(component));
        }
        return list;
    }

    public static List<String> toLegacy(Component... components) {
        List<String> list = new ArrayList<>();
        for (Component component : components) {
            list.add(toLegacy(component));
        }
        return list;
    }

    public static String replaceLegacy(String legacy) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < legacy.length(); i++) {
            char current = legacy.charAt(i);
            char next = legacy.charAt(i == legacy.length() - 1 ? i : i + 1);
            if (current == '§' || current == '&') {
                if (next == 'x' && legacy.length() > i + 13) {
                    builder.append("<color:#");
                    builder.append(legacy.charAt(i + 3));
                    builder.append(legacy.charAt(i + 5));
                    builder.append(legacy.charAt(i + 7));

                    builder.append(legacy.charAt(i + 9));
                    builder.append(legacy.charAt(i + 11));
                    builder.append(legacy.charAt(i + 13));
                    builder.append(">");
                    i += 13;
                    continue;
                }
                String color = getColor(next);
                if (color == null) {
                    builder.append(current);
                    continue;
                }
                builder.append(color);
                i++;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    public static String getColor(char c) {
        switch (c) {
            case '0':
                return "<black>";
            case '1':
                return "<dark_blue>";
            case '2':
                return "<dark_green>";
            case '3':
                return "<dark_aqua>";
            case '4':
                return "<dark_red>";
            case '5':
                return "<dark_purple>";
            case '6':
                return "<gold>";
            case '7':
                return "<gray>";
            case '8':
                return "<dark_gray>";
            case '9':
                return "<blue>";
            case 'a':
                return "<green>";
            case 'b':
                return "<aqua>";
            case 'c':
                return "<red>";
            case 'd':
                return "<light_purple>";
            case 'e':
                return "<yellow>";
            case 'f':
                return "<white>";
            case 'k':
                return "<obfuscated>";
            case 'l':
                return "<bold>";
            case 'm':
                return "<strikethrough>";
            case 'n':
                return "<underlined>";
            case 'o':
                return "<italic>";
            case 'r':
                return "<reset>";
            default:
                return null;
        }
    }

    public static String clear(String msg) {
        return PlainTextComponentSerializer.plainText().serialize(getMiniMessage().deserialize(replaceLegacy(msg)));
    }

    public static String clear(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static List<Component> splitComponent(Component message, char c) {
        List<Component> components = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (char character : toLegacy(message).toCharArray()) {
            if (character == c) {
                components.add(formatComponent(builder.toString()));
                builder = new StringBuilder();
            } else {
                builder.append(character);
            }
        }
        components.add(formatComponent(builder.toString()));
        return components;
    }

    public static Component formatPlaceholder(Component message, MiniMessagePlaceholder... placeholder) {
        return message.replaceText(builder -> {
            for (MiniMessagePlaceholder place : placeholder) {
                if (place.isComponent()) {
                    builder.matchLiteral("{" + place.getPlaceholder() + "}").replacement((Component) place.getValue());
                } else {
                    builder.matchLiteral(place.getPlaceholder()).replacement(formatComponent(place.getValue().toString()));
                }
            }
        });
    }

    //Bukkit defaults for time
    public static Title createTitle(Component title, Component subtitle) {
        return Title.title(title, subtitle, Title.Times.times(
                Duration.of(10 * 50L, ChronoUnit.MILLIS),
                Duration.of(70 * 50L, ChronoUnit.MILLIS),
                Duration.of(20 * 50L, ChronoUnit.MILLIS)
        ));
    }
    // times in ticks
    public static Title createTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return Title.title(title, subtitle, Title.Times.times(
                Duration.of(fadeIn * 50L, ChronoUnit.MILLIS),
                Duration.of(stay * 50L, ChronoUnit.MILLIS),
                Duration.of(fadeOut * 50L, ChronoUnit.MILLIS)
        ));
    }

    public static Title createTitle(Component title, Component subtitle, Title.Times times) {
        return Title.title(title, subtitle, times);
    }

    public static BossBar createBossBar(String title, BarColor color, BarStyle style, float progress, MiniMessagePlaceholder... placeholders) {
        return BossBar.bossBar(formatComponent(title, placeholders), progress, BossBar.Color.valueOf(color.name()), BossBar.Overlay.valueOf(style.name().replace("SOLID", "PROGRESS")));
    }

    public static Inventory createInventory(InventoryHolder owner, int rows, String title) {
        return createInventory(owner, rows, formatComponent(title));
    }

    public static Inventory createInventory(InventoryHolder owner, int rows, Component title) {
        return BukkitAdventureBridges.get().createInventory(owner, rows * 9, title);
    }
}
