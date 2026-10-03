package net.vortexdevelopment.vortexcore.compatibility.folia;

import net.vortexdevelopment.vortexcore.compatibility.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Run a task on Folia Scheduler or fallback for Bukkit Scheduler
 * Delay is in ticks
 */
public class SchedulerUtils {

    private static boolean isFolia;
    private static FoliaDelegate foliaDelegate;
    private static final Method IS_OWNED_BY_CURRENT_LOCATION = resolveOwnershipMethod(Location.class);
    private static final Method IS_OWNED_BY_CURRENT_BLOCK = resolveOwnershipMethod(Block.class);
    private static final Method IS_OWNED_BY_CURRENT_ENTITY = resolveOwnershipMethod(Entity.class);

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            foliaDelegate = (FoliaDelegate) Class.forName("net.vortexdevelopment.vortexcore.compatibility.folia.FoliaDelegateImpl").getConstructor().newInstance();
        } catch (Throwable e) {
            isFolia = false;
            foliaDelegate = null;
        }
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public static boolean isOwnedByCurrentRegion(Location location) {
        return isOwnedByCurrentRegion(IS_OWNED_BY_CURRENT_LOCATION, location);
    }

    public static boolean isOwnedByCurrentRegion(Block block) {
        return isOwnedByCurrentRegion(IS_OWNED_BY_CURRENT_BLOCK, block);
    }

    public static boolean isOwnedByCurrentRegion(Entity entity) {
        return isOwnedByCurrentRegion(IS_OWNED_BY_CURRENT_ENTITY, entity);
    }

    private static Method resolveOwnershipMethod(Class<?> subjectType) {
        try {
            return Bukkit.class.getMethod("isOwnedByCurrentRegion", subjectType);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static boolean isOwnedByCurrentRegion(Method method, Object subject) {
        if (!isFolia || !ServerVersion.isAtLeastVersion("1.19.4")) {
            return Bukkit.isPrimaryThread();
        }
        if (method == null) {
            throw new IllegalStateException("This Folia server does not expose region ownership checks.");
        }

        try {
            return (boolean) method.invoke(null, subject);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not check Folia region ownership.", exception);
        }
    }

    //Entity
    public static SchedulerTask runEntityTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runEntity(plugin, entity, runnable, null));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runEntity(plugin, entity, runnable, null));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static @Nullable SchedulerTask runEntityTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired) {
        if (isFolia && foliaDelegate != null) {
            Object task = foliaDelegate.runEntity(plugin, entity, runnable, retired);
            if (task == null) {
                runTask(plugin, retired);
                return null;
            }
            return new SchedulerTask(task);
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static SchedulerTask runEntityTaskLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runEntityLater(plugin, entity, runnable, correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTaskLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runEntityLater(plugin, entity, runnable, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public static SchedulerTask runEntityTaskTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runEntityTimer(plugin, entity, runnable, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTaskTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runEntityTimer(plugin, entity, runnable, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    //Location
    public static SchedulerTask runLocationTask(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runLocation(plugin, location, runnable));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTask(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runLocation(plugin, location, runnable));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static SchedulerTask runLocationTaskLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runLocationLater(plugin, location, runnable, correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTaskLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runLocationLater(plugin, location, runnable, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public static SchedulerTask runLocationTaskTimer(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runLocationTimer(plugin, location, runnable, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTaskTimer(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runLocationTimer(plugin, location, runnable, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    //Default
    public static SchedulerTask runTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobal(plugin, runnable));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobal(plugin, runnable));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static SchedulerTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runAsync(plugin, runnable));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runAsync(plugin, runnable));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    public static SchedulerTask runTaskLater(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobalLater(plugin, runnable, correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobalLater(plugin, runnable, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public static SchedulerTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runAsyncLater(plugin, runnable, asyncDelayMillis(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runAsyncLater(plugin, runnable, asyncDelayMillis(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
    }

    public static SchedulerTask scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobalLater(plugin, runnable, correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobalLater(plugin, runnable, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay));
    }

    public static SchedulerTask scheduleSyncRepeatingTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobalTimer(plugin, runnable, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask scheduleSyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobalTimer(plugin, runnable, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period));
    }

    public static SchedulerTask runTaskTimer(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobalTimer(plugin, runnable, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskTimer(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobalTimer(plugin, runnable, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    public static SchedulerTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runAsyncTimer(plugin, runnable, asyncDelayMillis(delay), asyncDelayMillis(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runAsyncTimer(plugin, runnable, asyncDelayMillis(delay), asyncDelayMillis(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }

    public static void cancelTask(SchedulerTask task) {
        if (task == null) return;
        if (isFolia) {
            task.getAsFoliaTask().cancel();
        } else {
            Bukkit.getScheduler().cancelTask(task.getAsBukkitTaskId());
        }
    }

    public static void cancelAllTasks(Plugin plugin) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().cancelTasks(plugin);
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }

    public static boolean isCurrentlyRunning(SchedulerTask task) {
        if (isFolia) {
            return task.getAsFoliaTask().getExecutionState() == io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState.RUNNING;
        } else {
            return Bukkit.getScheduler().isCurrentlyRunning(task.getAsBukkitTaskId());
        }
    }

    public static boolean isQueued(SchedulerTask task) {
        if (isFolia) {
            return task.getAsFoliaTask().getExecutionState() == io.papermc.paper.threadedregions.scheduler.ScheduledTask.ExecutionState.IDLE;
        } else {
            return Bukkit.getScheduler().isQueued(task.getAsBukkitTaskId());
        }
    }

    private static long correctDelay(long delay) {
        if (isFolia && delay <= 0) {
            return 1;
        }
        return delay;
    }

    private static long asyncDelayMillis(long delayTicks) {
        long safeTicks = Math.max(1L, delayTicks);
        return safeTicks > Long.MAX_VALUE / 50L ? Long.MAX_VALUE : safeTicks * 50L;
    }
}
