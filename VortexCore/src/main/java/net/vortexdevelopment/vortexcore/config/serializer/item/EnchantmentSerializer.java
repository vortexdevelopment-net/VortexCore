package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.config.serializer.placeholder.PlaceholderProcessor;
import net.vortexdevelopment.vortexcore.config.serializer.type.StringListSerializerAbstract;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpleyaml.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serializer for item enchantments.
 * Format: "ENCHANTMENT:LEVEL" (e.g., "SHARPNESS:5")
 */
public class EnchantmentSerializer extends StringListSerializerAbstract {

    public EnchantmentSerializer() {
        super("Enchantments");
    }

    @Override
    public List<String> serialize(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        Map<Enchantment, Integer> enchants;

        // Handle enchanted books differently
        if (meta instanceof EnchantmentStorageMeta enchantmentMeta) {
            enchants = enchantmentMeta.getStoredEnchants();
        } else {
            enchants = meta.getEnchants();
        }

        if (enchants.isEmpty()) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            String enchantName = entry.getKey().getKey().getKey().toUpperCase();
            int level = entry.getValue();
            result.add(enchantName + ":" + level);
        }

        return result;
    }

    @Override
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section, PlaceholderProcessor placeholderProcessor) {
        read(section, list -> {
            if (list.isEmpty()) {
                return;
            }

            ItemStack item = current.get();
            ItemMeta meta = item.getItemMeta();
            boolean isEnchantedBook = meta instanceof EnchantmentStorageMeta;

            for (String enchantString : list) {
                String[] parts = enchantString.split(":");
                if (parts.length != 2) {
                    if (VortexPlugin.getInstance() != null) {
                        VortexPlugin.getInstance().getLogger().warning("Invalid enchantment format: " + enchantString + " in " + section.getCurrentPath());
                    } else {
                        System.err.println("Invalid enchantment format: " + enchantString + " in " + section.getCurrentPath());
                    }
                    continue;
                }

                String enchantName = parts[0].toUpperCase();
                int level;

                try {
                    level = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    if (VortexPlugin.getInstance() != null) {
                        VortexPlugin.getInstance().getLogger().warning("Invalid enchantment level: " + parts[1] + " in " + section.getCurrentPath());
                    } else {
                        System.err.println("Invalid enchantment level: " + parts[1] + " in " + section.getCurrentPath());
                    }
                    continue;
                }

                try {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                    if (enchantment == null) {
                        if (VortexPlugin.getInstance() != null) {
                            VortexPlugin.getInstance().getLogger().warning("Unknown enchantment: " + enchantName + " in " + section.getCurrentPath());
                        } else {
                            System.err.println("Unknown enchantment: " + enchantName + " in " + section.getCurrentPath());
                        }
                        continue;
                    }

                    if (isEnchantedBook) {
                        EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) meta;
                        enchantMeta.addStoredEnchant(enchantment, level, true);
                    } else {
                        meta.addEnchant(enchantment, level, true);
                    }
                } catch (IllegalArgumentException e) {
                    if (VortexPlugin.getInstance() != null) {
                        VortexPlugin.getInstance().getLogger().warning("Error adding enchantment: " + enchantName + " in " + section.getCurrentPath());
                    } else {
                        System.err.println("Error adding enchantment: " + enchantName + " in " + section.getCurrentPath());
                    }
                }
            }

            item.setItemMeta(meta);
        });
    }
}
