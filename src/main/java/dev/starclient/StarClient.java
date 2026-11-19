package dev.starclient;

import dev.starclient.gui.StarClickScreen;
import dev.starclient.module.ModuleManager;
import dev.starclient.module.modules.CropEspModule;
import dev.starclient.module.modules.EnderChestEspModule;
import dev.starclient.module.modules.SpawnerEspModule;
import dev.starclient.module.modules.StorageEspModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class StarClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModuleManager.register(new StorageEspModule());
        ModuleManager.register(new SpawnerEspModule());
        ModuleManager.register(new CropEspModule());
        ModuleManager.register(new EnderChestEspModule());

        KeyBinding openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.starclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyBinding.Category.MISC
        ));

        KeyBinding toggleEsp = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.starclient.toggle_storage_esp",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new StarClickScreen());
                else if (client.currentScreen instanceof StarClickScreen) client.setScreen(null);
            }

            while (toggleEsp.wasPressed()) {
                ModuleManager.get(StorageEspModule.class).ifPresent(module -> {
                    module.toggle();
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("Storage ESP: " + (module.isEnabled() ? "ON" : "OFF")), true);
                    }
                });
            }
            ModuleManager.tick();
            ModuleManager.render();
        });
    }
}
