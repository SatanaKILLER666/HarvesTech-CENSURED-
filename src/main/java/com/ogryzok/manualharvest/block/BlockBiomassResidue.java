package com.ogryzok.manualharvest.block;

import com.ogryzok.harvestech;
import com.ogryzok.manualharvest.ManualHarvestLogic;
import com.ogryzok.manualharvest.ManualHarvestRegistry;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockBiomassResidue extends BlockBush {
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D);
    private static final SoundType BIOMASS_BREAK_SOUND = new SoundType(1.0F, 1.0F, SoundEvents.BLOCK_SLIME_BREAK, SoundEvents.BLOCK_GRASS_STEP, SoundEvents.BLOCK_SLIME_PLACE, SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);

    public BlockBiomassResidue() {
        super(Material.PLANTS);
        setRegistryName(harvestech.MODID, "biomass_residue");
        setTranslationKey(harvestech.MODID + ".biomass_residue");
        setHardness(0.0F);
        setResistance(0.0F);
        setSoundType(BIOMASS_BREAK_SOUND);
        setLightOpacity(0);
        disableStats();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) return false;
        if (ManualHarvestLogic.useCanAndGive(player, held, new ItemStack(ManualHarvestRegistry.DIRTY_BIOMASS))) {
            world.setBlockToAir(pos);
            return true;
        }
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return AABB; }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        return AABB.offset(pos);
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return true;
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
        return super.collisionRayTrace(blockState, worldIn, pos, start, end);
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) { return NULL_AABB; }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) { return true; }

    @Override
    public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override
    public boolean isFullCube(IBlockState state) { return false; }
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }

    public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }
}
