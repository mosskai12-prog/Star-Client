package dev.starclient.module;

public abstract class Module {
    private final String name;
    private final String description;
    private final String category;
    private boolean enabled;

    protected Module(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
    }
    public final String getName() { return name; }
    public final String getDescription() { return description; }
    public final String getCategory() { return category; }
    public final boolean isEnabled() { return enabled; }
    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
}
