package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.config.serializer.placeholder.PlaceholderProcessor;
import net.vortexdevelopment.vortexcore.config.serializer.type.StringSerializerAbstract;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemStack;
import org.simpleyaml.configuration.ConfigurationSection;

public class NameSerializer extends StringSerializerAbstract {

    public NameSerializer() {
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
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section, PlaceholderProcessor placeholderProcessor) {
        read(section, string -> {
            if (string.isEmpty()) {
                return;
            }

            AdventureUtils.formatItemName(current.get(), placeholderProcessor.process(string));
        });
    }
}
