package com.ogryzok.lifecrusher.item;

import com.google.common.collect.Multimap;
import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import com.ogryzok.lifecrusher.block.BlockLifeCrusher;
import com.ogryzok.lifecrusher.client.render.LifeCrusherRodRenderer;
import com.ogryzok.lifecrusher.tile.TileLifeCrusher;
import java.util.List;
import net.minecraft.util.text.translation.I18n;
import java.util.UUID;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemLifeCrusherRod extends ItemSword implements IAnimatable {
    public static final Item.ToolMaterial LIFE_CRUSHER_ROD_MATERIAL =
            EnumHelper.addToolMaterial("LIFE_CRUSHER_ROD_MATERIAL", 3, 1561, 4.0F, 0.0F, 10);

    private static final UUID REACH_MODIFIER_ID = UUID.fromString("8d3f8f2c-5d84-4efa-9f65-6dbf437b0d8a");
    private static final double ATTACK_DAMAGE = 15.7D;
    private static final double SPLASH_DAMAGE = 12.7D;
    private static final double ATTACK_SPEED = -3.33D;
    private static final double REACH_BONUS = 1.5D;

    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemLifeCrusherRod() {
        super(LIFE_CRUSHER_ROD_MATERIAL);

        setRegistryName(LifeCrusherRegistry.MODID, "life_crusher_rod");
        setTranslationKey(LifeCrusherRegistry.MODID + ".life_crusher_rod");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
        setMaxDamage(LIFE_CRUSHER_ROD_MATERIAL.getMaxUses());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void setTileEntityItemStackRenderer(TileEntityItemStackRenderer renderer) {
        super.setTileEntityItemStackRenderer(renderer);
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        this.setTileEntityItemStackRenderer(new LifeCrusherRodRenderer());
    }

    private boolean canBeInstalled(ItemStack stack) {
        return !stack.isItemDamaged();
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        if (!(world.getBlockState(pos).getBlock() instanceof BlockLifeCrusher)) {
            return EnumActionResult.PASS;
        }

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileLifeCrusher)) {
            return EnumActionResult.PASS;
        }

        TileLifeCrusher crusher = (TileLifeCrusher) te;
        ItemStack heldStack = player.getHeldItem(hand);

        if (!crusher.canAcceptRod()) {
            player.sendStatusMessage(new TextComponentString(crusher.getBuildStatusText()), true);
            return EnumActionResult.SUCCESS;
        }

        if (!this.canBeInstalled(heldStack)) {
            player.sendStatusMessage(new TextComponentString("A full-durability rod is required for assembly"), true);
            return EnumActionResult.SUCCESS;
        }

        if (!isTopRodPlacementClick(facing, hitY)) {
            player.sendStatusMessage(new TextComponentString("The rod must be installed from above"), true);
            return EnumActionResult.SUCCESS;
        }

        if (crusher.installRod()) {
            if (!player.capabilities.isCreativeMode) {
                heldStack.shrink(1);
            }

            player.sendStatusMessage(new TextComponentString("Machine assembled"), true);
            return EnumActionResult.SUCCESS;
        }

        return EnumActionResult.PASS;
    }

    private boolean isTopRodPlacementClick(EnumFacing facing, float hitY) {
        return facing == EnumFacing.UP || hitY >= 1.0F;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        boolean result = super.hitEntity(stack, target, attacker);

        if (!attacker.world.isRemote) {
            target.addPotionEffect(new PotionEffect(LifeCrusherRegistry.STUPOR, 140, 0));

            AxisAlignedBB splashBox = target.getEntityBoundingBox().grow(1.15D, 0.5D, 1.15D);
            for (EntityLivingBase nearby : attacker.world.getEntitiesWithinAABB(EntityLivingBase.class, splashBox)) {
                if (nearby == attacker || nearby == target || !nearby.isEntityAlive()) {
                    continue;
                }

                nearby.attackEntityFrom(DamageSource.causeMobDamage(attacker), (float) SPLASH_DAMAGE);
            }
        }

        return result;
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);

        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            modifiers.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());

            modifiers.put(
                    SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", ATTACK_DAMAGE, 0)
            );

            modifiers.put(
                    SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", ATTACK_SPEED, 0)
            );

            modifiers.put(
                    EntityPlayer.REACH_DISTANCE.getName(),
                    new AttributeModifier(REACH_MODIFIER_ID, "Life crusher rod reach modifier", REACH_BONUS, 0)
            );
        }

        return modifiers;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.harvestech.life_crusher_rod.slash"));
        tooltip.add(TextFormatting.DARK_RED + I18n.translateToLocal("tooltip.harvestech.life_crusher_rod.stupor"));
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
