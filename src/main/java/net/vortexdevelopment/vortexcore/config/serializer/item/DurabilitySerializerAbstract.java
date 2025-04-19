package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.config.serializer.type.IntegerSerializerAbstract;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpleyaml.configuration.ConfigurationSection;

public class DurabilitySerializerAbstract extends IntegerSerializerAbstract {

    public DurabilitySerializerAbstract() {
        super("Durability");
    }

    @Override
    public Integer serialize(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof Damageable damageable) {
            return damageable.getDamage();
        }
        return null;
    }

    @Override
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section) {
        read(section, integer -> {
            if (integer == null) {
                return;
            }

            ItemMeta meta = current.get().getItemMeta();
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(integer);
                current.get().setItemMeta(meta);
            }
        });
    }
}
