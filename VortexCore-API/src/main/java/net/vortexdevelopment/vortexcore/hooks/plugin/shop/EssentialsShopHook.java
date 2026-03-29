package net.vortexdevelopment.vortexcore.hooks.plugin.shop;

import net.ess3.api.IEssentials;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import net.vortexdevelopment.vortexcore.hooks.internal.types.ShopHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

@Element(priority = 1)
@DependsOn(IEssentials.class)
public class EssentialsShopHook extends ShopHook {

    private IEssentials essentials;

    @Override
    public double getPrice(ItemStack itemStack) {
        if (!canSell(itemStack)) {
            return 0;
        }
        return essentials.getWorth().getPrice(essentials, itemStack).doubleValue();
    }

    @Override
    public double getPrice(ItemStack itemStack, Player player) {
        if (!canSell(itemStack)) {
            return 0;
        }
        return essentials.getWorth().getPrice(essentials, itemStack).doubleValue();
    }

    @Override
    public boolean canSell(ItemStack itemStack) {
        if (!canEnable() && !isEnabled() && essentials == null) {
            return false;
        }
        BigDecimal price = essentials.getWorth().getPrice(essentials, itemStack);
        if (price == null) {
            return false;
        }
        return price.doubleValue() > 0;
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
