package net.vortexdevelopment.vortexcore.config.serializer;

import net.vortexdevelopment.vortexcore.config.serializer.placeholder.PlaceholderProcessor;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemStack;
import org.simpleyaml.configuration.ConfigurationSection;

import java.util.function.Consumer;

public abstract class AbstractItemSerializer<T> {

    protected String path;
    protected int priority = 10; // Lower is first

    public AbstractItemSerializer(String path) {
        this.path = path;
    }

    public AbstractItemSerializer(String path, int priority) {
        this.path = path;
        this.priority = priority;
    }

    protected void read(ConfigurationSection section, Consumer<T> consumer) {
        Object object = section.get(path);
        if (object == null) {
            return;
        }
        consumer.accept((T) object);
    }

    public abstract T serialize(ItemStack itemStack);

    public abstract void deserialize(Pointer<ItemStack> current, ConfigurationSection section, PlaceholderProcessor placeholderProcessor);
}
