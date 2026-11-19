package dev.starclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private ModuleManager() {}
    public static void register(Module module) { MODULES.add(module); }
    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }
    public static <T extends Module> Optional<T> get(Class<T> type) {
        return MODULES.stream().filter(type::isInstance).map(type::cast).findFirst();
    }
    public static void tick() {
        for (Module module : MODULES) if (module.isEnabled()) module.onTick();
    }
    public static void render() {
        for (Module module : MODULES) if (module.isEnabled()) module.onRender();
    }
}
