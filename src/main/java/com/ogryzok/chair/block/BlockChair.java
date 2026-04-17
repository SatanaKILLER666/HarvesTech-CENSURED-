package com.ogryzok.chair.block;

import com.ogryzok.chair.entity.EntitySeat;
import com.ogryzok.chair.tile.TileChair;
import com.ogryzok.harvestech;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockChair extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    private static final AxisAlignedBB CHAIR_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);

    public BlockChair() {
        super(Material.IRON);
        setRegistryName(harvestech.MODID, "chair");
        setTranslationKey(harvestech.MODID + ".chair");
        setCreativeTab(CreativeTabs.DECORATIONS);
        setHardness(1.8F);
        setResistance(3.0F);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileChair();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, net.minecraft.entity.EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }
        if (player.isSneaking() || player.isRiding()) {
            return true;
        }

        if (!world.isRemote) {
            List<EntitySeat> seats = world.getEntitiesWithinAABB(EntitySeat.class, new AxisAlignedBB(pos).grow(0.25D));
            for (EntitySeat seat : seats) {
                if (seat.isDead) {
                    continue;
                }
                if (!seat.getPassengers().isEmpty()) {
                    return true;
                }
                seat.setDead();
            }

            EntitySeat seat = new EntitySeat(world, pos, state.getValue(FACING));
            world.spawnEntity(seat);
            player.startRiding(seat, true);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            List<EntitySeat> seats = world.getEntitiesWithinAABB(EntitySeat.class, new AxisAlignedBB(pos).grow(0.25D));
            for (EntitySeat seat : seats) {
                seat.setDead();
            }
        }
        super.breakBlock(world, pos, state);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
        IBlockState particleState = net.minecraft.init.Blocks.CONCRETE_POWDER.getDefaultState().withProperty(net.minecraft.block.BlockColored.COLOR, net.minecraft.item.EnumDyeColor.SILVER);

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
        if (pos == null) {
            return false;
        }

        IBlockState particleState = net.minecraft.init.Blocks.CONCRETE_POWDER.getDefaultState().withProperty(net.minecraft.block.BlockColored.COLOR, net.minecraft.item.EnumDyeColor.SILVER);
        double x = pos.getX() + world.rand.nextDouble();
        double y = pos.getY() + world.rand.nextDouble();
        double z = pos.getZ() + world.rand.nextDouble();
        double off = 0.1D;

        if (target.sideHit == EnumFacing.DOWN) y = pos.getY() - off;
        if (target.sideHit == EnumFacing.UP) y = pos.getY() + 1.0D + off;
        if (target.sideHit == EnumFacing.NORTH) z = pos.getZ() - off;
        if (target.sideHit == EnumFacing.SOUTH) z = pos.getZ() + 1.0D + off;
        if (target.sideHit == EnumFacing.WEST) x = pos.getX() - off;
        if (target.sideHit == EnumFacing.EAST) x = pos.getX() + 1.0D + off;

        net.minecraft.client.particle.Particle particle = manager.spawnEffectParticle(
                EnumParticleTypes.BLOCK_CRACK.getParticleID(),
                x, y, z,
                0.0D, 0.0D, 0.0D,
                Block.getStateId(particleState)
        );

        if (particle != null) {
            particle.multiplyVelocity(0.2F);
            particle.multipleParticleScaleBy(0.6F);
        }

        return true;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, java.util.Random rand, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return CHAIR_AABB;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return CHAIR_AABB;
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
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
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
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
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
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}
