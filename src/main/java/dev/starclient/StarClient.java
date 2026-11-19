package dev.starclient;

import dev.starclient.module.ModuleManager;
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

        KeyBinding toggleEsp = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.starclient.toggle_storage_esp",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleEsp.wasPressed()) {
                ModuleManager.get(StorageEspModule.class).ifPresent(module -> {
                    module.toggle();
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("Storage ESP: " + (module.isEnabled() ? "ON" : "OFF")), true);
                    }
                });
            }
            ModuleManager.tick();
        });
    }
}
