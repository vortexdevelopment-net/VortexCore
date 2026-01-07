package net.vortexdevelopment.vortexcore.hooks.plugin;


import net.vortexdevelopment.vortexcore.hooks.internal.types.ShopHook;
import net.vortexdevelopment.vortexcore.hooks.internal.types.StackerHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.shop.EssentialsShopHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.shop.ShopGUIPlusHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.stacker.VortexStackerHook;

import java.util.LinkedHashSet;
import java.util.Set;

public class HookManager {

    private final static Set<ShopHook> shopHooks = new LinkedHashSet<>();
    private final static Set<StackerHook> stackerHooks = new LinkedHashSet<>();

    static {
        shopHooks.add(new ShopGUIPlusHook());
        shopHooks.add(new EssentialsShopHook());

        stackerHooks.add(new VortexStackerHook());


        // Enable hooks if their required plugin is present
        for (ShopHook shopHook : shopHooks) {
            if (shopHook.canEnable()) {
                shopHook.onEnable();
                shopHook.setEnabled(true);
            }
        }

        for (StackerHook stackerHook : stackerHooks) {
            if (stackerHook.canEnable()) {
                stackerHook.onEnable();
                stackerHook.setEnabled(true);
            }
        }
    }

    public static ShopHook getEnabledShopHook() {
        for (ShopHook shopHook : shopHooks) {
            if (shopHook.isEnabled()) {
                return shopHook;
            }
        }
        return null;
    }

    public static StackerHook getEnabledStackerHook() {
        for (StackerHook stackerHook : stackerHooks) {
            if (stackerHook.isEnabled()) {
                return stackerHook;
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
            } else if (shopHook.isEnabled()) {
                shopHook.onDisable();
                shopHook.setEnabled(false);
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
