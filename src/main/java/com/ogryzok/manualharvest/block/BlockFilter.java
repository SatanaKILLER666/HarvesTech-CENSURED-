package com.ogryzok.manualharvest.block;

import com.ogryzok.harvestech;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.manualharvest.ManualHarvestRegistry;
import com.ogryzok.manualharvest.tile.TileFilter;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockFilter extends Block {
    private static final AxisAlignedBB ZERO_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);

    public BlockFilter() {
        super(Material.ROCK);
        setRegistryName(harvestech.MODID, "filter");
        setTranslationKey(harvestech.MODID + ".filter");
        setCreativeTab(CreativeTabs.DECORATIONS);
        setHardness(1.5F);
        setResistance(6.0F);
        setSoundType(SoundType.STONE);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileFilter();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer) {
        EnumFacing horizontal = placer == null ? EnumFacing.NORTH : placer.getHorizontalFacing().getOpposite();
        return getDefaultState().withProperty(FACING, horizontal);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileFilter)) {
            return false;
        }

        TileFilter filter = (TileFilter) te;
        if (world.isRemote) {
            return true;
        }

        if (held.isEmpty()) {
            return false;
        }

        if (held.getItem() == Item.getItemFromBlock(Blocks.SAND)) {
            return filter.tryAddSand(player, hand, held);
        }

        if (held.getItem() == ManualHarvestRegistry.DIRTY_BIOMASS || FoodRegistry.isDirtyBiomassContainer(held)) {
            return filter.tryStartFiltering(player, hand, held);
        }

        if (held.getItem() == FoodRegistry.CAN || held.getItem() == FoodRegistry.WOODEN_JAR) {
            return filter.tryExtractBiomass(player, hand, held);
        }

        return false;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, java.util.Random rand, int fortune) {
        return net.minecraft.init.Items.AIR;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileFilter) {
                TileFilter filter = (TileFilter) te;
                if (filter.isAssembled()) {
                    spawnAsEntity(world, pos, new ItemStack(ManualHarvestRegistry.FILTER_BASE));
                    spawnAsEntity(world, pos, new ItemStack(ManualHarvestRegistry.FILTER_HOPPER));
                    if (filter.getSandCount() > 0) {
                        spawnAsEntity(world, pos, new ItemStack(Blocks.SAND, filter.getSandCount()));
                    }
                } else {
                    spawnAsEntity(world, pos, new ItemStack(ManualHarvestRegistry.FILTER_BASE));
                }
            }
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        super.breakBlock(world, pos, state);
    }


    @SideOnly(Side.CLIENT)
    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
        IBlockState particleState = net.minecraft.init.Blocks.PLANKS.getDefaultState().withProperty(net.minecraft.block.BlockPlanks.VARIANT, net.minecraft.block.BlockPlanks.EnumType.SPRUCE);

        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 8; ++y) {
                for (int z = 0; z < 4; ++z) {
                    double px = pos.getX() + (x + 0.5D) / 4.0D;
                    double py = pos.getY() + (y + 0.5D) / 8.0D;
                    double pz = pos.getZ() + (z + 0.5D) / 4.0D;
                    manager.spawnEffectParticle(
                            EnumParticleTypes.BLOCK_CRACK.getParticleID(),
                            px, py, pz,
                            px - pos.getX() - 0.5D,
                            py - pos.getY() - 1.0D,
                            pz - pos.getZ() - 0.5D,
                            Block.getStateId(particleState)
                    );
                }
            }
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean addHitEffects(IBlockState state, World world, RayTraceResult target, ParticleManager manager) {
        BlockPos pos = target.getBlockPos();
        if (pos == null) return false;

        IBlockState particleState = net.minecraft.init.Blocks.PLANKS.getDefaultState().withProperty(net.minecraft.block.BlockPlanks.VARIANT, net.minecraft.block.BlockPlanks.EnumType.SPRUCE);
        AxisAlignedBB box = state.getBoundingBox(world, pos).offset(pos);

        double x = pos.getX() + world.rand.nextDouble() * (box.maxX - box.minX - 0.2D) + 0.1D + box.minX - pos.getX();
        double y = pos.getY() + world.rand.nextDouble() * (box.maxY - box.minY - 0.2D) + 0.1D + box.minY - pos.getY();
        double z = pos.getZ() + world.rand.nextDouble() * (box.maxZ - box.minZ - 0.2D) + 0.1D + box.minZ - pos.getZ();

        switch (target.sideHit) {
            case DOWN: y = box.minY - 0.1D; break;
            case UP: y = box.maxY + 0.1D; break;
            case NORTH: z = box.minZ - 0.1D; break;
            case SOUTH: z = box.maxZ + 0.1D; break;
            case WEST: x = box.minX - 0.1D; break;
            case EAST: x = box.maxX + 0.1D; break;
        }

        manager.spawnEffectParticle(
                EnumParticleTypes.BLOCK_CRACK.getParticleID(),
                x, y, z,
                0.0D, 0.0D, 0.0D,
                Block.getStateId(particleState)
        );
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
        return ZERO_AABB.offset(pos);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }


    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{FACING});
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}
