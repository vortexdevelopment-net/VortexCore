package net.vortexdevelopment.vortexcore.compatibility.folia;

import org.bukkit.scheduler.BukkitTask;

public class SchedulerTask {

    private final Object task;
    private static Class<?> scheduledTaskClass;

    static {
        try {
            scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
        } catch (ClassNotFoundException e) {
            scheduledTaskClass = null;
        }
    }

    public SchedulerTask(Object task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task instanceof BukkitTask || (scheduledTaskClass != null && scheduledTaskClass.isInstance(task))) {
            this.task = task;
        } else {
            throw new IllegalArgumentException("Task: " + task.getClass().getName() + " is not a BukkitTask or ScheduledTask");
        }
    }

    public Object getTask() {
        return task;
    }

    public io.papermc.paper.threadedregions.scheduler.ScheduledTask getAsFoliaTask() {
        return (io.papermc.paper.threadedregions.scheduler.ScheduledTask) task;
    }

    public BukkitTask getAsBukkitTask() {
        return (BukkitTask) task;
    }

    public int getAsBukkitTaskId() {
        return getAsBukkitTask().getTaskId();
    }
}
