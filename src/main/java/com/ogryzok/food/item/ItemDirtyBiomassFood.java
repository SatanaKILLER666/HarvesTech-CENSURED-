package com.ogryzok.food.item;

import com.ogryzok.disease.DiseaseHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemDirtyBiomassFood extends ItemFoodBasic {

    private final float infectionChance;

    public ItemDirtyBiomassFood(String name, int amount, float saturation, boolean useSnowEatingParticles, Item containerItem, float infectionChance) {
        super(name, amount, saturation, false, useSnowEatingParticles, containerItem);
        this.infectionChance = infectionChance;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        ItemStack result = super.onItemUseFinish(stack, worldIn, entityLiving);

        if (!worldIn.isRemote && entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityLiving;
            if (player.getRNG().nextFloat() < infectionChance) {
                DiseaseHandler.infectIfHealthy(player);
            }
        }

        return result;
    }
}
