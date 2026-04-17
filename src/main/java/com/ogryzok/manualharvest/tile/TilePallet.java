package com.ogryzok.manualharvest.tile;

import com.ogryzok.manualharvest.block.BlockPallet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class TilePallet extends TileEntity implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    @Override
    public void registerControllers(AnimationData animationData) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    public int getStage() {
        if (world == null || pos == null) return BlockPallet.STAGE_EMPTY;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockPallet) {
            return state.getValue(BlockPallet.STAGE).intValue();
        }
        return BlockPallet.STAGE_EMPTY;
    }
}
