package com.ogryzok.manualharvest.block;

import com.ogryzok.harvestech;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.manualharvest.ManualHarvestLogic;
import com.ogryzok.manualharvest.tile.TilePallet;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockPallet extends Block {
    private static final AxisAlignedBB ZERO_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 2);
    public static final int STAGE_EMPTY = 0;
    public static final int STAGE_SEMI = 1;
    public static final int STAGE_FULL = 2;
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.1875D, 0.9375D);

    public BlockPallet() {
        super(Material.WOOD);
        setRegistryName(harvestech.MODID, "pallet");
        setTranslationKey(harvestech.MODID + ".pallet");
        setCreativeTab(CreativeTabs.DECORATIONS);
        setHardness(1.0F);
        setResistance(2.0F);
        setSoundType(SoundType.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(STAGE, STAGE_EMPTY));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TilePallet();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        int stage = state.getValue(STAGE);
        if (stage == STAGE_FULL) {
            if (ManualHarvestLogic.useCanAndGive(player, player.getHeldItem(hand), new ItemStack(FoodRegistry.BIOMASS_CAN))) {
                world.setBlockState(pos, state.withProperty(STAGE, STAGE_SEMI), 3);
                return true;
            }
        } else if (stage == STAGE_SEMI) {
            if (ManualHarvestLogic.useCanAndGive(player, player.getHeldItem(hand), new ItemStack(FoodRegistry.BIOMASS_CAN))) {
                world.setBlockState(pos, state.withProperty(STAGE, STAGE_EMPTY), 3);
                return true;
            }
        }
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return AABB; }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
        return ZERO_AABB.offset(pos);
    }
    @Override
    public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override
    public boolean isFullCube(IBlockState state) { return false; }
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.ENTITYBLOCK_ANIMATED; }
    public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }
    @Override
    public int getMetaFromState(IBlockState state) { return state.getValue(STAGE); }
    @Override
    public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(STAGE, Math.max(0, Math.min(2, meta))); }
    @Override
    protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, STAGE); }
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }
}
