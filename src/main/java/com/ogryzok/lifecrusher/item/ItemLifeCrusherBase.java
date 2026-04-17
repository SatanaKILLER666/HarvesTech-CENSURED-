package com.ogryzok.lifecrusher.item;

import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import com.ogryzok.lifecrusher.block.BlockLifeCrusher;
import com.ogryzok.lifecrusher.client.render.LifeCrusherBaseRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemLifeCrusherBase extends Item implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemLifeCrusherBase() {
        setRegistryName(LifeCrusherRegistry.MODID, "life_crusher_base");
        setTranslationKey(LifeCrusherRegistry.MODID + ".life_crusher_base");
        setCreativeTab(CreativeTabs.MATERIALS);
        setMaxStackSize(64);
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        this.setTileEntityItemStackRenderer(new LifeCrusherBaseRenderer());
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        BlockPos placePos = pos.offset(facing);

        if (!player.canPlayerEdit(placePos, facing, player.getHeldItem(hand))) {
            return EnumActionResult.FAIL;
        }

        if (!world.mayPlace(LifeCrusherRegistry.LIFE_CRUSHER, placePos, false, facing, null)) {
            return EnumActionResult.FAIL;
        }

        EnumFacing horizontal = player.getHorizontalFacing().getOpposite();

        IBlockState state = LifeCrusherRegistry.LIFE_CRUSHER.getDefaultState()
                .withProperty(BlockLifeCrusher.FACING, horizontal)
                .withProperty(BlockLifeCrusher.POWERED, Boolean.FALSE);

        if (world.setBlockState(placePos, state, 3)) {
            if (!player.capabilities.isCreativeMode) {
                player.getHeldItem(hand).shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }

        return EnumActionResult.FAIL;
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}