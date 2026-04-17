package com.ogryzok.semendestiller.block;

import com.ogryzok.semendestiller.SemenDestillerRegistry;
import com.ogryzok.semendestiller.tile.TileSemenDestillerBase;
import com.ogryzok.semendestiller.tile.TileSemenDestillerMotor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockSemenDestillerMotor extends Block {
    private static final AxisAlignedBB ZERO_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0,0,0,1,1,1);

    public BlockSemenDestillerMotor() {
        super(Material.IRON);
        setRegistryName(SemenDestillerRegistry.MODID, "semen_destiller_motor");
        setTranslationKey(SemenDestillerRegistry.MODID + ".semen_destiller_motor");
        setHardness(5.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 2);
        setSoundType(SoundType.METAL);
        setCreativeTab(CreativeTabs.REDSTONE);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new TileSemenDestillerMotor(); }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        IBlockState below = world.getBlockState(pos.down());
        if (below.getBlock() == SemenDestillerRegistry.SEMEN_DESTILLER_BASE) return getDefaultState().withProperty(FACING, below.getValue(BlockSemenDestillerBase.FACING));
        return getDefaultState().withProperty(FACING, placer == null ? EnumFacing.NORTH : placer.getHorizontalFacing().getOpposite());
    }
    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) { if (!world.isRemote) { TileEntity te = world.getTileEntity(pos); if (te instanceof TileSemenDestillerMotor) ((TileSemenDestillerMotor) te).syncStructureState(); TileEntity below = world.getTileEntity(pos.down()); if (below instanceof TileSemenDestillerBase) { world.setBlockState(pos.down(), world.getBlockState(pos.down()).withProperty(BlockSemenDestillerBase.FACING, state.getValue(FACING)), 3); ((TileSemenDestillerBase) below).syncStructureState(); } }}
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false; TileEntity te = world.getTileEntity(pos.down()); if (!(te instanceof TileSemenDestillerBase)) return false; TileSemenDestillerBase base = (TileSemenDestillerBase) te; if (!world.isRemote) { if (!base.isAssembled()) { player.sendStatusMessage(new TextComponentString("The destiller only works when assembled: base below and motor above"), true); return true; } player.openGui(SemenDestillerRegistry.getModInstance(), SemenDestillerRegistry.GUI_ID, world, pos.getX(), pos.getY()-1, pos.getZ()); } return true; }
    @Override public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) { if (!world.isRemote) { TileEntity te = world.getTileEntity(pos); if (te instanceof TileSemenDestillerMotor) ((TileSemenDestillerMotor) te).syncStructureState(); }}
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
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
}
