package dev.starclient.module.modules;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public final class EnderChestEspModule extends BlockFinderModule {
    public EnderChestEspModule() {
        super("Ender Chest ESP", "Highlights nearby ender chests.", 0xFFB58CFF);
    }

    @Override
    protected boolean matches(BlockState state) {
        return state.isOf(Blocks.ENDER_CHEST);
    }
}
