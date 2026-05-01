package net.vortexdevelopment.vortexcore.hooks.plugin.vault;

import net.milkbowl.vault.economy.Economy;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.function.Consumer;

public class EconomyHook {

    private static EconomyWrapper economy;

    public static void init() {
        //Get service from bukkit
        try {
            RegisteredServiceProvider<Economy> economyService = VortexPlugin.getInstance().getServer().getServicesManager().getRegistration(Economy.class);
            economy = new EconomyWrapper(economyService.getProvider());
        } catch (Throwable e) {
            VortexPlugin.getInstance().getLogger().warning("Vault not found! Economy features will not work.");
        }
    }

    public static void access(Consumer<EconomyWrapper> economyConsumer) {
        if (economy == null) {
            return;
        }
        economyConsumer.accept(economy);
    }
}
