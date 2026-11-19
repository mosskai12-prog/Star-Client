package dev.starclient.module.modules;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public final class SpawnerEspModule extends BlockFinderModule {
    public SpawnerEspModule() {
        super("Spawner ESP", "Finds mob spawners and trial spawners in loaded terrain.", 0xFFFF665E);
    }

    @Override
    protected boolean matches(BlockState state) {
        return state.isOf(Blocks.SPAWNER) || state.isOf(Blocks.TRIAL_SPAWNER);
    }
}
