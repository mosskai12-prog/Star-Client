package dev.starclient.module.modules;

import dev.starclient.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.List;

public final class StorageEspModule extends Module {
    private int range = 32;
    private boolean chests = true, trappedChests = true, barrels = true, shulkers = true;
    private final List<StorageTarget> targets = new ArrayList<>();
    private int tickCounter;

    public StorageEspModule() {
        super("Storage ESP", "Highlights nearby storage blocks through terrain.", "Base Hunting");
    }

    @Override
    public void onTick() {
        if (++tickCounter < 10) return;
        tickCounter = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) { targets.clear(); return; }

        BlockPos origin = client.player.getBlockPos();
        List<StorageTarget> found = new ArrayList<>();
        int checked = 0;
        for (BlockPos pos : BlockPos.iterateOutwards(origin, range, range, range)) {
            if (checked++ >= 32768) break;
            int color = colorFor(client.world.getBlockState(pos).getBlock());
            if (color != 0) found.add(new StorageTarget(pos.toImmutable(), color));
        }
        targets.clear();
        targets.addAll(found);
    }

    @Override
    public void onRender() {
        for (StorageTarget target : targets) {
            GizmoDrawing.box(target.pos(), DrawStyle.stroked(target.argb(), 2.0f)).ignoreOcclusion();
        }
    }

    @Override
    public void onDisable() { targets.clear(); }

    private int colorFor(Block block) {
        if (chests && block == Blocks.CHEST) return 0xFFFFC44D;
        if (trappedChests && block == Blocks.TRAPPED_CHEST) return 0xFFFF5555;
        if (barrels && block == Blocks.BARREL) return 0xFF55D6A3;
        if (shulkers && isShulker(block)) return 0xFFB58CFF;
        return 0;
    }

    private boolean isShulker(Block b) {
        return b == Blocks.SHULKER_BOX || b == Blocks.WHITE_SHULKER_BOX || b == Blocks.ORANGE_SHULKER_BOX
            || b == Blocks.MAGENTA_SHULKER_BOX || b == Blocks.LIGHT_BLUE_SHULKER_BOX || b == Blocks.YELLOW_SHULKER_BOX
            || b == Blocks.LIME_SHULKER_BOX || b == Blocks.PINK_SHULKER_BOX || b == Blocks.GRAY_SHULKER_BOX
            || b == Blocks.LIGHT_GRAY_SHULKER_BOX || b == Blocks.CYAN_SHULKER_BOX || b == Blocks.PURPLE_SHULKER_BOX
            || b == Blocks.BLUE_SHULKER_BOX || b == Blocks.BROWN_SHULKER_BOX || b == Blocks.GREEN_SHULKER_BOX
            || b == Blocks.RED_SHULKER_BOX || b == Blocks.BLACK_SHULKER_BOX;
    }

    public List<StorageTarget> targets() { return List.copyOf(targets); }
    public int range() { return range; }
    public void setRange(int value) { range = Math.max(4, Math.min(96, value)); }
    public boolean chests() { return chests; }
    public void setChests(boolean value) { chests = value; }
    public boolean trappedChests() { return trappedChests; }
    public void setTrappedChests(boolean value) { trappedChests = value; }
    public boolean barrels() { return barrels; }
    public void setBarrels(boolean value) { barrels = value; }
    public boolean shulkers() { return shulkers; }
    public void setShulkers(boolean value) { shulkers = value; }

    public record StorageTarget(BlockPos pos, int argb) {}
}
