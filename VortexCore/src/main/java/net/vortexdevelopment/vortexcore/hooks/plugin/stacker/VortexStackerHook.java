package net.vortexdevelopment.vortexcore.hooks.plugin.stacker;

import net.vortexdevelopment.vortexcore.hooks.internal.types.StackerHook;
import net.vortexdevelopment.vortexstacker.api.VortexStackerApi;
import net.vortexdevelopment.vortexstacker.api.modules.stack.item.StackedItem;
import net.vortexdevelopment.vortexstacker.api.modules.stack.item.StackedItemManager;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.function.Consumer;

public class VortexStackerHook extends StackerHook {

    private StackedItemManager stackedItemManager;

    @Override
    public @NotNull BigInteger getRealAmount(@NotNull Item item) {
        if (stackedItemManager == null) {
            return BigInteger.valueOf(item.getItemStack().getAmount());
        }
        StackedItem stackedItem = stackedItemManager.getStackedItem(item);
        if (stackedItem == null) {
            return BigInteger.valueOf(item.getItemStack().getAmount());
        }
        return stackedItem.getAmount();
    }

    @Override
    public void createStackedItem(@NotNull Item item, @NotNull BigInteger amount) {
        stackedItemManager.createStackedItem(item, amount);
    }

    @Override
    public void createStackedItem(@NotNull Location location, @NotNull ItemStack itemStack, @NotNull BigInteger amount) {
        stackedItemManager.createStackedItem(itemStack, location, amount);
    }

    @Override
    public String getRequiredPlugin() {
        return "VortexStacker";
    }

    @Override
    public void onEnable() {
        stackedItemManager = VortexStackerApi.getItemManager();
    }

    @Override
    public void onDisable() {
        stackedItemManager = null;
    }
}
