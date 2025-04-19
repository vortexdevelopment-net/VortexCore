package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.config.serializer.type.BooleanSerializerAbstract;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpleyaml.configuration.ConfigurationSection;

/**
 * Serializer for the unbreakable flag.
 */
public class UnbreakableSerializerAbstract extends BooleanSerializerAbstract {

    public UnbreakableSerializerAbstract() {
        super("Unbreakable");
    }

    @Override
    public Boolean serialize(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta.isUnbreakable()) {
            return true;
        }
        
        return null;
    }

    @Override
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section) {
        read(section, unbreakable -> {
            if (unbreakable == null) {
                return;
            }
            
            ItemStack item = current.get();
            ItemMeta meta = item.getItemMeta();
            
            meta.setUnbreakable(unbreakable);
            item.setItemMeta(meta);
        });
    }
}