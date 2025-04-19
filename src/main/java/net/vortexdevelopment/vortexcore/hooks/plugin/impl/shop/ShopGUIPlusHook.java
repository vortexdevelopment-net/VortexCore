package net.vortexdevelopment.vortexcore.hooks.plugin.impl.shop;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.vortexdevelopment.vortexcore.hooks.plugin.types.ShopHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ShopGUIPlusHook extends ShopHook {

    @Override
    public double getPrice(ItemStack itemStack) {
        return ShopGuiPlusApi.getItemStackPriceSell(itemStack);
    }

    @Override
    public double getPrice(ItemStack itemStack, Player player) {
        return ShopGuiPlusApi.getItemStackPriceSell(player, itemStack);
    }

    @Override
    public boolean canSell(ItemStack itemStack) {
        return ShopGuiPlusApi.getItemStackPriceSell(itemStack) != -1.0;
    }

    @Override
    public String getRequiredPlugin() {
        return "ShopGUIPlus";
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
