package net.vortexdevelopment.vortexcore.hooks.plugin;


import net.vortexdevelopment.vortexcore.hooks.internal.types.ShopHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.shop.EssentialsShopHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.shop.ShopGUIPlusHook;

import java.util.HashSet;
import java.util.Set;

public class HookManager {

    private final static Set<ShopHook> shopHooks = new HashSet<>();

    static {
        shopHooks.add(new ShopGUIPlusHook());
        shopHooks.add(new EssentialsShopHook());
    }

    public static ShopHook getEnabledShopHook() {
        for (ShopHook shopHook : shopHooks) {
            if (shopHook.isEnabled()) {
                return shopHook;
            }
        }
        return null;
    }

    public static void setEnabledShopHook(String pluginName) {
        //Disable all hooks
        for (ShopHook shopHook : shopHooks) {
            if (shopHook.getRequiredPlugin().equalsIgnoreCase(pluginName)) {
                shopHook.onEnable();
                shopHook.setEnabled(true);
                System.err.println("Enabled " + pluginName + " as price provider.");
            } else if (shopHook.isEnabled()) {
                shopHook.onDisable();
                shopHook.setEnabled(false);
                System.err.println("Disabled " + shopHook.getRequiredPlugin());
            }
        }
    }

    public static boolean hasShopHook(String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) {
            return false;
        }
        for (ShopHook shopHook : shopHooks) {
            if (shopHook.getRequiredPlugin().equalsIgnoreCase(pluginName)) {
                return true;
            }
        }
        return false;
    }

    public static ShopHook getShopHook(String pluginName) {
        for (ShopHook shopHook : shopHooks) {
            if (shopHook.getRequiredPlugin().equalsIgnoreCase(pluginName)) {
                return shopHook;
            }
        }
        return null;
    }
}
