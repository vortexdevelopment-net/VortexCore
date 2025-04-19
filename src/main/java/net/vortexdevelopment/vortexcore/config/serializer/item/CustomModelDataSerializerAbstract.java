package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.config.serializer.type.IntegerSerializerAbstract;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpleyaml.configuration.ConfigurationSection;

/**
 * Serializer for custom model data.
 */
public class CustomModelDataSerializerAbstract extends IntegerSerializerAbstract {

    public CustomModelDataSerializerAbstract() {
        super("CustomModelData");
    }

    @Override
    public Integer serialize(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta.hasCustomModelData()) {
            return meta.getCustomModelData();
        }
        
        return null;
    }

    @Override
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section) {
        read(section, modelData -> {
            if (modelData == null) {
                return;
            }
            
            ItemStack item = current.get();
            ItemMeta meta = item.getItemMeta();
            
            meta.setCustomModelData(modelData);
            item.setItemMeta(meta);
        });
    }
}