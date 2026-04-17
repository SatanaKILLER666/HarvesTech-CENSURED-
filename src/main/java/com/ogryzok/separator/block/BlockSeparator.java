package com.ogryzok.separator.block;

import com.ogryzok.separator.SeparatorRegistry;
import com.ogryzok.separator.tile.TileSeparator;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockSeparator extends Block {
    private static final AxisAlignedBB ZERO_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool ASSEMBLED = PropertyBool.create("assembled");
    private static final AxisAlignedBB AABB = FULL_BLOCK_AABB;

    public BlockSeparator() {
        super(Material.IRON);
        setRegistryName(SeparatorRegistry.MODID, "separator");
        setTranslationKey(SeparatorRegistry.MODID + ".separator");
        setHardness(5.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 2);
        setSoundType(SoundType.STONE);
        setCreativeTab(CreativeTabs.REDSTONE);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(ASSEMBLED, Boolean.FALSE));
    }

    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new TileSeparator(); }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState()
                .withProperty(FACING, placer == null ? EnumFacing.NORTH : placer.getHorizontalFacing().getOpposite())
                .withProperty(ASSEMBLED, Boolean.FALSE);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileSeparator) {
                ((TileSeparator) te).markForSync();
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        TileEntity te = world.getTileEntity(pos);

        if (player.isSneaking() && te instanceof TileSeparator && !held.isEmpty() && held.getItem() == Items.BUCKET) {
            if (((TileSeparator) te).tryFillBucket(player, hand, held)) {
                return true;
            }
        }

        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }

        if (!world.isRemote && te instanceof TileSeparator) {
            TileSeparator tile = (TileSeparator) te;
            if (!tile.isAssembled()) {
                if (!held.isEmpty() && held.getItem() == SeparatorRegistry.SEPARATOR_WHISK) {
                    if (tile.assemble() && !player.capabilities.isCreativeMode) {
                        held.shrink(1);
                    }
                } else {
                    player.sendStatusMessage(new TextComponentTranslation("tooltip.harvestech.separator.missing_blade"), true);
                }
                return true;
            }
        }

        if (player.isSneaking()) return false;
        if (!world.isRemote) {
            player.openGui(SeparatorRegistry.getModInstance(), SeparatorRegistry.GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex() | (state.getValue(ASSEMBLED) ? 4 : 0); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(ASSEMBLED, (meta & 4) != 0); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, ASSEMBLED); }
    @Override public IBlockState withRotation(IBlockState state, Rotation rot) { return state.withProperty(FACING, rot.rotate(state.getValue(FACING))); }
    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) { return state.withRotation(mirror.toRotation(state.getValue(FACING))); }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.INVISIBLE; }
    @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return AABB; }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
        return ZERO_AABB.offset(pos);
    }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }
    @Nullable @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn, BlockPos pos) { return AABB; }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    @Override
    public net.minecraft.item.Item getItemDropped(IBlockState state, java.util.Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileSeparator) {
                TileSeparator tile = (TileSeparator) te;
                spawnAsEntity(world, pos, new ItemStack(SeparatorRegistry.SEPARATOR));
                if (tile.isAssembled()) {
                    spawnAsEntity(world, pos, new ItemStack(SeparatorRegistry.SEPARATOR_WHISK));
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
        IBlockState particleState = net.minecraft.init.Blocks.STONE.getDefaultState();
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

        IBlockState particleState = net.minecraft.init.Blocks.STONE.getDefaultState();
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

}
