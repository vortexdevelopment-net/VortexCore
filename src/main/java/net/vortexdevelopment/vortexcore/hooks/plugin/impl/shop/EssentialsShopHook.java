package net.vortexdevelopment.vortexcore.hooks.plugin.impl.shop;

import com.earth2me.essentials.Essentials;
import net.ess3.api.IEssentials;
import net.vortexdevelopment.vortexcore.hooks.plugin.types.ShopHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EssentialsShopHook extends ShopHook {

    private IEssentials essentials;

    @Override
    public double getPrice(ItemStack itemStack) {
        return essentials.getWorth().getPrice(essentials, itemStack).doubleValue();
    }

    @Override
    public double getPrice(ItemStack itemStack, Player player) {
        return essentials.getWorth().getPrice(essentials, itemStack).doubleValue();
    }

    @Override
    public boolean canSell(ItemStack itemStack) {
        return essentials.getWorth().getPrice(essentials, itemStack).doubleValue() > 0;
    }

    @Override
    public String getRequiredPlugin() {
        return "Essentials";
    }

    @Override
    public void onEnable() {
        if (canEnable()) {
            this.essentials = getPlugin(IEssentials.class);
        }
    }

    @Override
    public void onDisable() {

    }
}
