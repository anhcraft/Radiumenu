package dev.anhcraft.radiumenu.integrations;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class Integration {
    private boolean hooked = false;

    public abstract boolean init(JavaPlugin var1);

    public boolean isHooked() {
        return this.hooked;
    }

    public void setHooked(boolean hooked) {
        this.hooked = hooked;
    }
}
