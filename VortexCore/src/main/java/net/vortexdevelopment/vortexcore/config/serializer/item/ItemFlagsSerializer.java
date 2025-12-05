package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.config.serializer.placeholder.PlaceholderProcessor;
import net.vortexdevelopment.vortexcore.config.serializer.type.StringListSerializerAbstract;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpleyaml.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Serializer for item flags.
 */
public class ItemFlagsSerializer extends StringListSerializerAbstract {

    public ItemFlagsSerializer() {
        super("Item Flags");
    }

    @Override
    public List<String> serialize(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        Set<ItemFlag> flags = meta.getItemFlags();

        if (flags.isEmpty()) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (ItemFlag flag : flags) {
            result.add(flag.name());
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

            for (String flagName : list) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException e) {
                    VortexPlugin.getInstance().getLogger().warning("Invalid item flag: " + flagName + " in " + section.getCurrentPath());
                }
            }

            item.setItemMeta(meta);
        });
    }
}