package net.vortexdevelopment.vortexcore.compatibility.folia;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class FoliaDelegateImpl implements FoliaDelegate {

    @Override
    public Object runEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired) {
        return entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), retired);
    }

    @Override
    public Object runEntityLater(Plugin plugin, Entity entity, Runnable runnable, long delay) {
        return entity.getScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), null, delay);
    }

    @Override
    public Object runEntityTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, long period) {
        return entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), null, delay, period);
    }

    @Override
    public Object runLocation(Plugin plugin, Location location, Runnable runnable) {
        return plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> runnable.run());
    }

    @Override
    public Object runLocationLater(Plugin plugin, Location location, Runnable runnable, long delay) {
        return plugin.getServer().getRegionScheduler().runDelayed(plugin, location, scheduledTask -> runnable.run(), delay);
    }

    @Override
    public Object runLocationTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period) {
        return plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, scheduledTask -> runnable.run(), delay, period);
    }

    @Override
    public Object runGlobal(Plugin plugin, Runnable runnable) {
        return plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> runnable.run());
    }

    @Override
    public Object runGlobalLater(Plugin plugin, Runnable runnable, long delay) {
        return plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delay);
    }

    @Override
    public Object runGlobalTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delay, period);
    }

    @Override
    public Object runAsync(Plugin plugin, Runnable runnable) {
        return plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
    }

    @Override
    public Object runAsyncLater(Plugin plugin, Runnable runnable, long delay) {
        return plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object runAsyncTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        return plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delay, period, TimeUnit.MILLISECONDS);
    }
}
