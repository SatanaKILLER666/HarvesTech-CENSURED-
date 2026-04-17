package com.ogryzok.separator.item;

import com.ogryzok.harvestech;
import com.ogryzok.separator.client.render.SeparatorWhiskItemRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemSeparatorWhisk extends Item implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemSeparatorWhisk() {
        setRegistryName(harvestech.MODID, "separator_whisk");
        setTranslationKey(harvestech.MODID + ".separator_whisk");
        setCreativeTab(CreativeTabs.MATERIALS);
        setMaxStackSize(64);
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            private final SeparatorWhiskItemRenderer renderer = new SeparatorWhiskItemRenderer();

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
