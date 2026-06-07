package net.vortexdevelopment.vortexcore.text.hologram;

import net.vortexdevelopment.vortexcore.text.MiniMessagePlaceholder;

class HologramPlaceholder {

    private final HologramPlaceholderProvider provider;
    private long lastUpdate = 0;
    private long updateIntervalTicks = 0;

    HologramPlaceholder(HologramPlaceholderProvider provider, long updateIntervalTicks) {
        this.provider = provider;
        this.updateIntervalTicks = updateIntervalTicks;
    }

    MiniMessagePlaceholder getPlaceholder() {
        return provider.getPlaceholder();
    }

    Object getValue() {
        return provider.getPlaceholder().getValue();
    }

    boolean shouldUpdate() {
        return shouldUpdate(false);
    }

    boolean shouldUpdate(boolean force) {
        if (updateIntervalTicks == 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (force || now - lastUpdate >= updateIntervalTicks * 50L) {
            lastUpdate = now;
            return true;
        }
        return false;
    }

}
