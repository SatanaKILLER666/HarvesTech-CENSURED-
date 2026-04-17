package com.ogryzok.disease.item;

import com.ogryzok.disease.client.render.SyringeItemRenderer;
import com.ogryzok.harvestech;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemSyringe extends Item implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemSyringe() {
        setRegistryName(harvestech.MODID, "syringe");
        setTranslationKey(harvestech.MODID + ".syringe");
        setCreativeTab(CreativeTabs.BREWING);
        setMaxStackSize(16);
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            private final SyringeItemRenderer renderer = new SyringeItemRenderer();

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
}
