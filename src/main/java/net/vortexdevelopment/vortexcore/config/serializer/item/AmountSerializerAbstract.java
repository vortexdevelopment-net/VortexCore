package net.vortexdevelopment.vortexcore.config.serializer.item;

import net.vortexdevelopment.vortexcore.config.serializer.type.IntegerSerializerAbstract;
import net.vortexdevelopment.vortexcore.utils.Pointer;
import org.bukkit.inventory.ItemStack;
import org.simpleyaml.configuration.ConfigurationSection;

/**
 * Serializer for item amount (stack size).
 */
public class AmountSerializerAbstract extends IntegerSerializerAbstract {

    public AmountSerializerAbstract() {
        super("Amount");
    }

    @Override
    public Integer serialize(ItemStack itemStack) {
        int amount = itemStack.getAmount();
        // Only serialize if the amount is not the default (1)
        return amount > 1 ? amount : null;
    }

    @Override
    public void deserialize(Pointer<ItemStack> current, ConfigurationSection section) {
        read(section, amount -> {
            if (amount == null || amount <= 0) {
                // Set the default amount to 1 if the value is null or less than or equal to 0
                // If the amount is 0 bukkit will set the type to air which is not desired
                current.get().setAmount(1);
                return;
            }
            
            ItemStack item = current.get();
            item.setAmount(amount);
        });
    }
}