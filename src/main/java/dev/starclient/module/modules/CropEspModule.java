package dev.starclient.module.modules;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;

public final class CropEspModule extends BlockFinderModule {
    public CropEspModule() {
        super("Crop ESP", "Finds fully grown wheat, carrots, potatoes, beetroot and nether wart.", 0xFF63DF91);
    }

    @Override
    protected boolean matches(BlockState state) {
        if (state.isOf(Blocks.WHEAT) || state.isOf(Blocks.CARROTS) || state.isOf(Blocks.POTATOES)) {
            return state.get(Properties.AGE_7) >= 7;
        }
        if (state.isOf(Blocks.BEETROOTS) || state.isOf(Blocks.NETHER_WART)) {
            return state.get(Properties.AGE_3) >= 3;
        }
        return false;
    }
}
