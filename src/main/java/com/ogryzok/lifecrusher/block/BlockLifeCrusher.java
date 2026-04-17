package com.ogryzok.lifecrusher.block;

import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import com.ogryzok.lifecrusher.tile.TileLifeCrusher;
import com.ogryzok.lifecrusher.util.OreDictUtil;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

public class BlockLifeCrusher extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    private static final AxisAlignedBB FULL_TWO_BLOCKS = new AxisAlignedBB(
            0.0D, 0.0D, 0.0D,
            1.0D, 2.0D, 1.0D
    );

    private static final AxisAlignedBB ZERO_AABB = new AxisAlignedBB(
            0.0D, 0.0D, 0.0D,
            0.0D, 0.0D, 0.0D
    );

    public BlockLifeCrusher() {
        super(Material.IRON);
        setRegistryName(LifeCrusherRegistry.MODID, "life_crusher");
        setTranslationKey(LifeCrusherRegistry.MODID + ".life_crusher");
        setHardness(5.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 2);
        setSoundType(SoundType.METAL);
        setCreativeTab(CreativeTabs.REDSTONE);
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(POWERED, Boolean.FALSE));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileLifeCrusher();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        EnumFacing horizontal = placer == null ? EnumFacing.NORTH : placer.getHorizontalFacing().getOpposite();
        boolean redstone = world.isBlockPowered(pos);
        return getDefaultState()
                .withProperty(FACING, horizontal)
                .withProperty(POWERED, redstone);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            updatePowerState(world, pos, state);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!world.isRemote) {
            updatePowerState(world, pos, state);
        }
    }

    private void updatePowerState(World world, BlockPos pos, IBlockState state) {
        boolean redstone = world.isBlockPowered(pos);
        boolean old = state.getValue(POWERED);
        if (old != redstone) {
            world.setBlockState(pos, state.withProperty(POWERED, redstone), 3);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            return false;
        }

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileLifeCrusher)) {
            return false;
        }

        TileLifeCrusher tile = (TileLifeCrusher) te;

        if (!world.isRemote) {
            ItemStack held = player.getHeldItem(hand);

            // Установка медных проводов
            if (!held.isEmpty() && tile.canAcceptWire() && isCopperWire(held)) {
                if (tile.addWire()) {
                    if (!player.capabilities.isCreativeMode) {
                        held.shrink(1);
                    }

                    player.sendStatusMessage(
                            new TextComponentString(tile.getBuildStatusText()),
                            true
                    );
                }
                return true;
            }

            // Установка rod только по верхней части механизма
            if (!held.isEmpty()
                    && held.getItem() == LifeCrusherRegistry.LIFE_CRUSHER_ROD
                    && tile.canAcceptRod()) {

                if (held.isItemDamaged()) {
                    player.sendStatusMessage(
                            new TextComponentString("A full-durability rod is required for assembly"),
                            true
                    );
                    return true;
                }

                // Для высокой кастомной модели клики по верхней части часто приходят как боковые,
                // поэтому не блокируем установку rod по facing/hitY: достаточно полной прочности
                // и стадии сборки tile.canAcceptRod().

                if (tile.installRod()) {
                    if (!player.capabilities.isCreativeMode) {
                        held.shrink(1);
                    }

                    player.sendStatusMessage(
                            new TextComponentString("Rod installed"),
                            true
                    );
                }
                return true;
            }

            if (!tile.isAssembled()) {
                player.sendStatusMessage(
                        new TextComponentString(tile.getBuildStatusText()),
                        true
                );
                return true;
            }

            player.openGui(
                    LifeCrusherRegistry.getModInstance(),
                    LifeCrusherRegistry.GUI_ID,
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );
        }

        return true;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileLifeCrusher) {
            TileLifeCrusher lc = (TileLifeCrusher) tile;
            return Math.min(15, Math.max(0,
                    (int) Math.floor((lc.getEnergyStored() / (float) lc.getMaxEnergyStored()) * 15.0F)));
        }
        return 0;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(POWERED)) {
            meta |= 4;
        }
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(POWERED, (meta & 4) != 0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{FACING, POWERED});
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
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_TWO_BLOCKS;
    }


    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.SOLID;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        super.eventReceived(state, world, pos, id, param);
        TileEntity te = world.getTileEntity(pos);
        return te != null && te.receiveClientEvent(id, param);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos,
                                      AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
                                      @Nullable Entity entityIn, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_TWO_BLOCKS);
    }

    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
        return ZERO_AABB.offset(pos);
    }

    private boolean isTopRodPlacementClick(EnumFacing facing, float hitY) {
        return facing == EnumFacing.UP || hitY >= 1.0F;
    }

    private boolean isCopperWire(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        if (OreDictUtil.matches(stack, "wireCopper")) {
            return true;
        }

        if (stack.getItem().getRegistryName() != null) {
            String reg = stack.getItem().getRegistryName().toString().toLowerCase();

            if (reg.startsWith("ic2:") && reg.contains("cable")) {
                return true;
            }

            if (reg.startsWith("ic2:") && reg.contains("wire")) {
                return true;
            }
        }

        String name = stack.getDisplayName().toLowerCase();
        return (name.contains("copper") || name.contains("copper"))
                && (name.contains("cable") || name.contains("wire") || name.contains("wire"));
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
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        // Пусто: кастомный дроп спавним в breakBlock(), пока tile ещё существует.
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileLifeCrusher) {
                TileLifeCrusher tile = (TileLifeCrusher) te;

                // База всегда
                spawnAsEntity(world, pos, new ItemStack(LifeCrusherRegistry.LIFE_CRUSHER_BASE));

                int wires = getWireCountSafe(tile);
                boolean rodInstalled = hasRodInstalledSafe(tile);

                // Rod, если установлен
                if (rodInstalled) {
                    spawnAsEntity(world, pos, new ItemStack(LifeCrusherRegistry.LIFE_CRUSHER_ROD));
                }

                // 0-3 медной пыли, если стадия 5+ проводов или уже есть rod
                if (wires >= 5 || rodInstalled) {
                    ItemStack dust = getCopperDustStack(world.rand.nextInt(4));
                    if (!dust.isEmpty()) {
                        spawnAsEntity(world, pos, dust);
                    }
                }
            }
        }

        super.breakBlock(world, pos, state);
    }

    private int getWireCountSafe(TileLifeCrusher tile) {
        try {
            Method m = tile.getClass().getMethod("getWiresInstalled");
            Object result = m.invoke(tile);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception ignored) {}

        try {
            Field f = tile.getClass().getDeclaredField("wiresInstalled");
            f.setAccessible(true);
            return f.getInt(tile);
        } catch (Exception ignored) {}

        return 0;
    }

    private boolean hasRodInstalledSafe(TileLifeCrusher tile) {
        String[] methodNames = new String[] {
                "hasRodInstalled",
                "isRodInstalled",
                "getRodInstalled"
        };

        for (String name : methodNames) {
            try {
                Method m = tile.getClass().getMethod(name);
                Object result = m.invoke(tile);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception ignored) {}
        }

        try {
            Field f = tile.getClass().getDeclaredField("rodInstalled");
            f.setAccessible(true);
            return f.getBoolean(tile);
        } catch (Exception ignored) {}

        try {
            return tile.isAssembled();
        } catch (Exception ignored) {
            return false;
        }
    }

    private ItemStack getCopperDustStack(int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        NonNullList<ItemStack> ores = OreDictionary.getOres("dustCopper");
        if (ores != null && !ores.isEmpty()) {
            ItemStack stack = ores.get(0).copy();
            stack.setCount(count);
            return stack;
        }

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ic2", "dust"));
        if (item != null) {
            return new ItemStack(item, count, 0);
        }

        item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ic2", "copper_dust"));
        if (item != null) {
            return new ItemStack(item, count);
        }

        return ItemStack.EMPTY;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
        IBlockState state = net.minecraft.init.Blocks.IRON_BLOCK.getDefaultState();

        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 8; ++y) {
                for (int z = 0; z < 4; ++z) {

                    double px = pos.getX() + (x + 0.5D) / 4.0D;
                    double py = pos.getY() + (y + 0.5D) / 8.0D;
                    double pz = pos.getZ() + (z + 0.5D) / 4.0D;

                    manager.spawnEffectParticle(
                            net.minecraft.util.EnumParticleTypes.BLOCK_CRACK.getParticleID(),
                            px, py, pz,
                            px - pos.getX() - 0.5D,
                            py - pos.getY() - 1.0D,
                            pz - pos.getZ() - 0.5D,
                            Block.getStateId(state)
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
        IBlockState particleState = net.minecraft.init.Blocks.IRON_BLOCK.getDefaultState();

        double x = pos.getX() + world.rand.nextDouble();
        double y = pos.getY() + world.rand.nextDouble() * 2.0D;
        double z = pos.getZ() + world.rand.nextDouble();

        EnumFacing side = target.sideHit;
        double off = 0.1D;

        if (side == EnumFacing.DOWN) y = pos.getY() - off;
        if (side == EnumFacing.UP) y = pos.getY() + 2.0D + off;
        if (side == EnumFacing.NORTH) z = pos.getZ() - off;
        if (side == EnumFacing.SOUTH) z = pos.getZ() + 1.0D + off;
        if (side == EnumFacing.WEST) x = pos.getX() - off;
        if (side == EnumFacing.EAST) x = pos.getX() + 1.0D + off;

        net.minecraft.client.particle.Particle particle =
                manager.spawnEffectParticle(
                        net.minecraft.util.EnumParticleTypes.BLOCK_CRACK.getParticleID(),
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
}