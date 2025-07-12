package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.config.serializer.placeholder.PlaceholderProcessor;
import net.vortexdevelopment.vortexcore.config.serializer.type.StringSerializerAbstract;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.simpleyaml.configuration.ConfigurationSection;

public class MaterialSerializer extends StringSerializerAbstract {

    public MaterialSerializer() {
        super("Material", 1);
    }

    @Override
    public String serialize(ItemStack itemStack) {
        return itemStack.getType().toString();
    }

    @Override
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section, PlaceholderProcessor placeholderProcessor) {
        //TODO add nexo support with serialization chain
        read(section, string -> {
            if (string.isEmpty()) {
                return;
            }

            try {
                current.set(current.get().withType(Material.valueOf(string)));
            } catch (IllegalArgumentException e) {
                // Handle invalid material name
                if (VortexPlugin.getInstance() != null) {
                    VortexPlugin.getInstance().getLogger().warning("Invalid material name: " + string + " in " + section.getCurrentPath());
                } else {
                    System.err.println("Invalid material name: " + string + " in " + section.getCurrentPath());
                }
            }
        });
    }
}
