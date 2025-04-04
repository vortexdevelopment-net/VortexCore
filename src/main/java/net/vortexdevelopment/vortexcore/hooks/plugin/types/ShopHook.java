package net.vortexdevelopment.vortexcore.hooks.plugin.types;

import net.vortexdevelopment.vortexcore.hooks.plugin.PluginHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class ShopHook extends PluginHook {

    public abstract double getPrice(ItemStack itemStack, Player player);

    public abstract boolean canSell(ItemStack itemStack);

}
