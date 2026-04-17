package com.ogryzok.disease.item;

import com.ogryzok.disease.DiseaseHandler;
import com.ogryzok.disease.MalePowerHandler;
import com.ogryzok.disease.client.render.VaccineItemRenderer;
import com.ogryzok.harvestech;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemAidsVaccine extends Item implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private final boolean curesDisease;

    public ItemAidsVaccine() {
        this("aids_vaccine", "aids_vaccine", true);
    }

    public ItemAidsVaccine(String registryPath, String translationPath, boolean curesDisease) {
        this.curesDisease = curesDisease;
        setRegistryName(harvestech.MODID, registryPath);
        setTranslationKey(harvestech.MODID + "." + translationPath);
        setCreativeTab(CreativeTabs.BREWING);
        setMaxStackSize(16);
    }



    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            private final VaccineItemRenderer renderer = new VaccineItemRenderer();

            @Override
            public void renderByItem(ItemStack stack) {
                renderer.renderByItem(stack);
            }
        });
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!curesDisease) {
            if (!world.isRemote) {
                MalePowerHandler.consumeSteroid(player);
                if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
                }
                player.getCooldownTracker().setCooldown(this, 20);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (!world.isRemote && DiseaseHandler.downgradeStage(player)) {
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
            player.getCooldownTracker().setCooldown(this, 20);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
}
