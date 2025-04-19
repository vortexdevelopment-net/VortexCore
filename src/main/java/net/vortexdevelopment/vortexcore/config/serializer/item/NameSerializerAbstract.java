package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.config.serializer.type.StringSerializerAbstract;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemStack;
import org.simpleyaml.configuration.ConfigurationSection;

public class NameSerializerAbstract extends StringSerializerAbstract {

    public NameSerializerAbstract() {
        super("Name");
    }

    @Override
    public String serialize(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return null;
        }
        if (itemStack.getItemMeta().displayName() == null) {
            return null;
        }
        return AdventureUtils.toLegacy(AdventureUtils.convertToShadedComponent(itemStack.displayName()));
    }

    @Override
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section) {
        read(section, string -> {
            if (string.isEmpty()) {
                return;
            }

            AdventureUtils.formatItemName(current.get(), string);
        });
    }
}
