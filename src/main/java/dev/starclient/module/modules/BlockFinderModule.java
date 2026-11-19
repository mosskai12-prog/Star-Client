package dev.starclient.module.modules;

import dev.starclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.List;

public abstract class BlockFinderModule extends Module {
    private final int color;
    private final List<Target> targets = new ArrayList<>();
    private int range = 32;
    private int tickCounter;

    protected BlockFinderModule(String name, String description, int color) {
        super(name, description, "Base Hunting");
        this.color = color;
    }

    protected abstract boolean matches(BlockState state);

    @Override
    public final void onTick() {
        if (++tickCounter < 10) return;
        tickCounter = 0;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            targets.clear();
            return;
        }

        BlockPos origin = client.player.getBlockPos();
        List<Target> found = new ArrayList<>();
        int checked = 0;
        for (BlockPos pos : BlockPos.iterateOutwards(origin, range, range, range)) {
            if (checked++ >= 32768) break;
            if (matches(client.world.getBlockState(pos))) {
                found.add(new Target(pos.toImmutable(), color));
            }
        }
        targets.clear();
        targets.addAll(found);
    }

    @Override
    public final void onRender() {
        for (Target target : targets) {
            GizmoDrawing.box(target.pos(), DrawStyle.stroked(target.argb(), 2.0f)).ignoreOcclusion();
        }
    }

    @Override
    public void onDisable() { targets.clear(); }

    public final List<Target> targets() { return List.copyOf(targets); }
    public final int range() { return range; }
    public final void setRange(int value) { range = Math.max(8, Math.min(64, value)); }

    public record Target(BlockPos pos, int argb) {}
}
