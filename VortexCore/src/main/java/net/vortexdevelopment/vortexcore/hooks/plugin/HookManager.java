package net.vortexdevelopment.vortexcore.hooks.plugin;


import net.vortexdevelopment.vinject.annotation.component.Component;
import net.vortexdevelopment.vinject.annotation.component.Element;
import net.vortexdevelopment.vinject.annotation.lifecycle.OnEvent;
import net.vortexdevelopment.vinject.di.DependencyRepository;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.hooks.internal.types.ShopHook;
import net.vortexdevelopment.vortexcore.hooks.internal.types.StackerHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.shop.EssentialsShopHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.shop.ShopGUIPlusHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.stacker.VortexStackerHook;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RegisterListener
public class HookManager implements Listener {

    private final static Set<PluginHook> pluginHooks = new LinkedHashSet<>();

    static {
        pluginHooks.addAll(DependencyRepository.getInstance().collectElements(PluginHook.class));
        reloadHooks();
    }

    public static int getHookPriority(Class<? extends PluginHook> hookClass) {
        return pluginHooks.stream()
                .filter(hookClass::isInstance)
                .findFirst()
                .map(HookManager::getPriority)
                .orElse(0);
    }

    private static <T extends PluginHook> List<T> getHookByType(Class<T> hookClass) {
        return pluginHooks.stream()
                .filter(hookClass::isInstance)
                .map(hookClass::cast)
                .toList();
    }

    private static void reloadHooks() {
        reloadHooks(ShopHook.class);
        reloadHooks(StackerHook.class);
    }

    private static <T extends PluginHook> void reloadHooks(Class<T> type) {
        List<T> hooks = getHookByType(type);
        T bestHook = hooks.stream()
                .filter(PluginHook::canEnable)
                .findFirst()
                .orElse(null);

        for (T hook : hooks) {
            if (hook == bestHook) {
                if (!hook.isEnabled()) {
                    hook.onEnable();
                    hook.setEnabled(true);
                    VortexPlugin.getInstance().getLogger().info("[HookManager] [Priority] Enabled " + type.getSimpleName() + " hook: " + hook.getClass().getSimpleName() + " (Priority: " + getPriority(hook) + ")");
                }
            } else {
                if (hook.isEnabled()) {
                    hook.onDisable();
                    hook.setEnabled(false);
                    VortexPlugin.getInstance().getLogger().info("[HookManager] [Priority] Disabled " + type.getSimpleName() + " hook: " + hook.getClass().getSimpleName() + " (as a better hook exists or it's unavailable)");
                }
            }
        }
    }

    private static int getPriority(PluginHook hook) {
        return hook.getClass().isAnnotationPresent(Element.class) ? hook.getClass().getAnnotation(Element.class).priority() : 0;
    }

    public static ShopHook getEnabledShopHook() {
        for (ShopHook shopHook : getHookByType(ShopHook.class)) {
            if (shopHook.isEnabled()) {
                return shopHook;
            }
        }
        return null;
    }

    public static StackerHook getEnabledStackerHook() {
        for (StackerHook stackerHook : getHookByType(StackerHook.class)) {
            if (stackerHook.isEnabled()) {
                return stackerHook;
            }
        }
        return null;
    }

    public static void setEnabledShopHook(String pluginName) {
        //Disable all hooks
        for (ShopHook shopHook : getHookByType(ShopHook.class)) {
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
        for (ShopHook shopHook : getHookByType(ShopHook.class)) {
            if (shopHook.getRequiredPlugin().equalsIgnoreCase(pluginName)) {
                return true;
            }
        }
        return false;
    }

    public static ShopHook getShopHook(String pluginName) {
        for (ShopHook shopHook : getHookByType(ShopHook.class)) {
            if (shopHook.getRequiredPlugin().equalsIgnoreCase(pluginName)) {
                return shopHook;
            }
        }
        return null;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        reloadHooks();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        reloadHooks();
    }
}
