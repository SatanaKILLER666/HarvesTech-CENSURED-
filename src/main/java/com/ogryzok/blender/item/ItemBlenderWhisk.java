package com.ogryzok.blender.item;

import com.ogryzok.blender.client.render.BlenderWhiskItemRenderer;
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

public class ItemBlenderWhisk extends Item implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemBlenderWhisk() {
        setRegistryName(harvestech.MODID, "blender_whisk");
        setTranslationKey(harvestech.MODID + ".blender_whisk");
        setCreativeTab(CreativeTabs.MATERIALS);
        setMaxStackSize(64);
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            private final BlenderWhiskItemRenderer renderer = new BlenderWhiskItemRenderer();

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
