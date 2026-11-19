package dev.starclient.render;

import dev.starclient.module.ModuleManager;
import dev.starclient.module.modules.StorageEspModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class StorageEspRenderer {
    private StorageEspRenderer() {}

    public static void initialize() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            StorageEspModule module = ModuleManager.get(StorageEspModule.class).orElse(null);
            MinecraftClient client = MinecraftClient.getInstance();
            if (module == null || !module.isEnabled() || client.player == null) return;

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            Vec3d camera = client.gameRenderer.getCamera().getPos();

            for (StorageEspModule.StorageTarget target : module.targets()) {
                Box box = new Box(target.pos()).expand(0.002).offset(-camera.x, -camera.y, -camera.z);
                int color = target.argb();
                WorldRenderer.drawBox(context.matrices(), lines, box,
                    ((color >> 16) & 255) / 255.0f,
                    ((color >> 8) & 255) / 255.0f,
                    (color & 255) / 255.0f, 1.0f);
            }
        });
    }
}
