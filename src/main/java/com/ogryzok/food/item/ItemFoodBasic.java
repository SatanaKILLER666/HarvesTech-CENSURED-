package com.ogryzok.food.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemFoodBasic extends ItemFood {

    private final boolean useSnowEatingParticles;
    private final EnumAction useAction;
    private final Item containerItem;

    public ItemFoodBasic(String name, int amount, float saturation, boolean isWolfFood) {
        this(name, amount, saturation, isWolfFood, false, EnumAction.EAT, null);
    }

    public ItemFoodBasic(String name, int amount, float saturation, boolean isWolfFood, boolean useSnowEatingParticles) {
        this(name, amount, saturation, isWolfFood, useSnowEatingParticles, useSnowEatingParticles ? EnumAction.DRINK : EnumAction.EAT, null);
    }

    public ItemFoodBasic(String name, int amount, float saturation, boolean isWolfFood, boolean useSnowEatingParticles, EnumAction useAction) {
        this(name, amount, saturation, isWolfFood, useSnowEatingParticles, useAction, null);
    }

    public ItemFoodBasic(String name, int amount, float saturation, boolean isWolfFood, boolean useSnowEatingParticles, Item containerItem) {
        this(name, amount, saturation, isWolfFood, useSnowEatingParticles, useSnowEatingParticles ? EnumAction.DRINK : EnumAction.EAT, containerItem);
    }

    public ItemFoodBasic(String name, int amount, float saturation, boolean isWolfFood, boolean useSnowEatingParticles, EnumAction useAction, Item containerItem) {
        super(amount, saturation, isWolfFood);

        this.useSnowEatingParticles = useSnowEatingParticles;
        this.useAction = useAction;
        this.containerItem = containerItem;

        this.setRegistryName(name);
        this.setTranslationKey(name);
        this.setCreativeTab(CreativeTabs.FOOD);
        this.setAlwaysEdible();
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return useAction;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        ItemStack result = super.onItemUseFinish(stack, worldIn, entityLiving);

        if (containerItem == null || worldIn.isRemote) {
            return result;
        }

        if (entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityLiving;

            if (player.capabilities.isCreativeMode) {
                return result;
            }

            ItemStack emptyContainer = new ItemStack(containerItem);

            if (result.isEmpty()) {
                return emptyContainer;
            }

            if (!player.inventory.addItemStackToInventory(emptyContainer)) {
                player.dropItem(emptyContainer, false);
            }
        }

        return result;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onUsingTick(ItemStack stack, EntityLivingBase entityLiving, int count) {
        super.onUsingTick(stack, entityLiving, count);

        if (!useSnowEatingParticles || !entityLiving.world.isRemote || count <= 0 || count % 4 != 0) {
            return;
        }

        for (int i = 0; i < 6; ++i) {
            double xSpeed = (entityLiving.world.rand.nextDouble() - 0.5D) * 0.08D;
            double ySpeed = entityLiving.world.rand.nextDouble() * 0.08D;
            double zSpeed = (entityLiving.world.rand.nextDouble() - 0.5D) * 0.08D;

            float yawRad = entityLiving.rotationYawHead * 0.017453292F;
            float pitchRad = entityLiving.rotationPitch * 0.017453292F;
            double sideOffset = (entityLiving.world.rand.nextDouble() - 0.5D) * 0.3D;
            double forwardOffset = 0.3D;

            double x = entityLiving.posX - (double) MathHelper.sin(yawRad) * forwardOffset + (double) MathHelper.cos(yawRad) * sideOffset;
            double y = entityLiving.posY + (double) entityLiving.getEyeHeight() - 0.15D - (double) MathHelper.sin(pitchRad) * 0.1D;
            double z = entityLiving.posZ + (double) MathHelper.cos(yawRad) * forwardOffset + (double) MathHelper.sin(yawRad) * sideOffset;

            entityLiving.world.spawnParticle(EnumParticleTypes.SNOWBALL, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
