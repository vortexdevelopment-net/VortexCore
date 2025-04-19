package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.config.serializer.type.StringListSerializerAbstract;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemStack;
import org.simpleyaml.configuration.ConfigurationSection;

import java.util.List;

public class LoreSerializerAbstract extends StringListSerializerAbstract {

    public LoreSerializerAbstract() {
        super("Lore");
    }

    @Override
    public List<String> serialize(ItemStack itemStack) {
        if (itemStack.lore() == null) {
            return null;
        }

        return itemStack.lore().stream()
                .map(lore -> AdventureUtils.toLegacy(AdventureUtils.convertToShadedComponent(lore)))
                .toList();
    }

    @Override
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section) {
        read(section, list -> {
            if (list.isEmpty()) {
                return;
            }


            AdventureUtils.formatItemLore(current.get(), list);
        });
    }
}
