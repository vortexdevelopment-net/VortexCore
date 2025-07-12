package net.vortexdevelopment.vortexcore.config.serializer;

import net.vortexdevelopment.vortexcore.config.serializer.item.*;
import net.vortexdevelopment.vortexcore.config.serializer.placeholder.PlaceholderProcessor;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.simpleyaml.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registry for AbstractItemSerializer implementations.
 * This class manages all the serializers used for serializing and deserializing ItemStacks.
 */
public class ItemSerializer {

    private static final List<AbstractItemSerializer<?>> serializers = new ArrayList<>();
    private static boolean initialized = false;

    /**
     * Initialize the registry with default serializers.
     */
    public static void init() {
        if (initialized) {
            return;
        }

        // Register default serializers
        register(new MaterialSerializer());
        register(new NameSerializer());
        register(new LoreSerializer());
        register(new DurabilitySerializer());
        register(new EnchantmentSerializer());
        register(new ItemFlagsSerializer());
        register(new CustomModelDataSerializer());
        register(new UnbreakableSerializer());
        register(new AmountSerializer());

        initialized = true;
    }

    /**
     * Register a new serializer.
     *
     * @param serializer The serializer to register
     */
    public static void register(AbstractItemSerializer<?> serializer) {
        serializers.add(serializer);
        // Sort by priority (lower priority first)
        serializers.sort(Comparator.comparingInt(s -> s.priority));
    }

    public static String toBase64(ItemStack itemStack) {
        byte[] bytes = itemStack.serializeAsBytes();
        StringBuilder base64 = new StringBuilder();
        for (byte b : bytes) {
            base64.append(String.format("%02x", b));
        }
        return base64.toString();
    }

    public static ItemStack fromBase64(String base64ItemStack) {
        byte[] bytes = new byte[base64ItemStack.length() / 2];
        for (int i = 0; i < base64ItemStack.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(base64ItemStack.charAt(i), 16) << 4)
                    + Character.digit(base64ItemStack.charAt(i + 1), 16));
        }
        return ItemStack.deserializeBytes(bytes);
    }

    public static byte[] toBytes(ItemStack itemStack) {
        return itemStack.serializeAsBytes();
    }

    public static ItemStack fromBytes(byte[] bytes) {
        return ItemStack.deserializeBytes(bytes);
    }

    /**
     * Deserialize an ItemStack from a configuration section.
     *
     * @param section The configuration section to deserialize from
     * @return The deserialized ItemStack, or null if the section is null
     */
    public static ItemStack deserialize(ConfigurationSection section) {
        return deserialize(section, PlaceholderProcessor.defaultProcessor());
    }

    public static ItemStack deserialize(ConfigurationSection section, PlaceholderProcessor placeholderProcessor) {
        if (section == null) {
            return null;
        }

        // Initialize if not already initialized
        if (!initialized) {
            init();
        }

        // Create a default ItemStack with AIR material
        Pointer<ItemStack> itemStack = new Pointer<>(new ItemStack(Material.BARRIER, 1));

        // Apply each serializer to deserialize the item
        for (AbstractItemSerializer<?> serializer : serializers) {
            serializer.deserialize(itemStack, section, placeholderProcessor);
        }

        return itemStack.get();
    }

    /**
     * Serialize an ItemStack to a configuration section.
     *
     * @param itemStack The ItemStack to serialize
     * @param section The configuration section to serialize to
     */
    public static void serialize(ItemStack itemStack, ConfigurationSection section) {
        if (itemStack == null || section == null) {
            return;
        }

        // Initialize if not already initialized
        if (!initialized) {
            init();
        }

        // Apply each serializer to serialize the item
        for (AbstractItemSerializer<?> serializer : serializers) {
            Object serialized = serializer.serialize(itemStack);
            if (serialized != null) {
                section.set(serializer.path, serialized);
            }
        }
    }
}
